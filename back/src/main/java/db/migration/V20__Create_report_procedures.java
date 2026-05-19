package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

/**
 * RNF05 - Stored procedures (functions) para queries complexas.
 * Migration em Java porque o DDL e especifico do PostgreSQL
 * (LANGUAGE sql / RETURNS TABLE / dollar-quoting), que o H2 usado
 * nos testes nao suporta. Em H2 a migration e ignorada (no-op);
 * o {vendor} de location e recurso pago do Flyway, indisponivel aqui.
 */
public class V20__Create_report_procedures extends BaseJavaMigration {

    private static final String FN_DASHBOARD = """
            CREATE OR REPLACE FUNCTION fn_dashboard_academia()
            RETURNS TABLE (
                total_academias    BIGINT,
                total_alunos       INTEGER,
                total_professores  INTEGER,
                academias_ativas   INTEGER,
                academias_inativas INTEGER,
                media_alunos       DOUBLE PRECISION
            )
            LANGUAGE sql STABLE AS $$
                SELECT
                    COUNT(*)::BIGINT,
                    COALESCE(SUM(COALESCE(total_alunos, 0)), 0)::INTEGER,
                    COALESCE(SUM(COALESCE(total_professores, 0)), 0)::INTEGER,
                    COUNT(*) FILTER (WHERE ativo IS TRUE)::INTEGER,
                    COUNT(*) FILTER (WHERE ativo IS NOT TRUE)::INTEGER,
                    CASE WHEN COUNT(*) > 0
                         THEN COALESCE(SUM(COALESCE(total_alunos, 0)), 0)::DOUBLE PRECISION / COUNT(*)
                         ELSE 0
                    END
                FROM academia;
            $$;
            """;

    private static final String FN_RELATORIO = """
            CREATE OR REPLACE FUNCTION fn_relatorio_frequencia(
                p_academia_id BIGINT,
                p_inicio      TIMESTAMP,
                p_fim         TIMESTAMP
            )
            RETURNS TABLE (
                aluno_nome VARCHAR,
                total      BIGINT
            )
            LANGUAGE sql STABLE AS $$
                SELECT u.nome, COUNT(*)::BIGINT
                FROM checkin c
                JOIN usuario u ON u.id = c.id_aluno
                WHERE c.id_academia = p_academia_id
                  AND c.data_hora BETWEEN p_inicio AND p_fim
                GROUP BY u.nome
                ORDER BY u.nome;
            $$;
            """;

    private static final String FN_INATIVOS = """
            CREATE OR REPLACE FUNCTION fn_alunos_inativos(
                p_academia_id BIGINT,
                p_dias        INTEGER
            )
            RETURNS TABLE (
                aluno_id        BIGINT,
                aluno_nome      VARCHAR,
                email           VARCHAR,
                dias_sem_treino BIGINT,
                ativo           BOOLEAN
            )
            LANGUAGE sql STABLE AS $$
                SELECT
                    u.id AS aluno_id,
                    u.nome AS aluno_nome,
                    u.email,
                    FLOOR(EXTRACT(EPOCH FROM (NOW() - MAX(c.data_hora))) / 86400.0)::BIGINT AS dias_sem_treino,
                    (FLOOR(EXTRACT(EPOCH FROM (NOW() - MAX(c.data_hora))) / 86400.0)::BIGINT <= p_dias) AS ativo
                FROM checkin c
                JOIN usuario u ON u.id = c.id_aluno
                WHERE c.id_academia = p_academia_id
                GROUP BY u.id, u.nome, u.email
                HAVING FLOOR(EXTRACT(EPOCH FROM (NOW() - MAX(c.data_hora))) / 86400.0)::BIGINT > p_dias
                ORDER BY dias_sem_treino DESC;
            $$;
            """;

    @Override
    public void migrate(Context context) throws Exception {
        String db = context.getConnection().getMetaData().getDatabaseProductName();
        if (db != null && db.toUpperCase().contains("H2")) {
            return;
        }
        try (Statement st = context.getConnection().createStatement()) {
            st.execute(FN_DASHBOARD);
            st.execute(FN_RELATORIO);
            st.execute(FN_INATIVOS);
        }
    }
}
