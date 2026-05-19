package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

/**
 * RNF05 - Stored procedures (functions) para queries complexas.
 * Migration em Java porque o DDL e especifico do PostgreSQL
 * (LANGUAGE sql / RETURNS TABLE / dollar-quoting), que o H2 usado
 * nos testes nao suporta. Em H2, cria aliases equivalentes via CREATE ALIAS.
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

    // -------------------------------------------------------------------------
    // H2-compatible aliases (used in tests)
    // -------------------------------------------------------------------------

    private static final String H2_FN_DASHBOARD = """
            CREATE ALIAS IF NOT EXISTS fn_dashboard_academia AS $$
            java.sql.ResultSet fn_dashboard_academia(java.sql.Connection conn) throws Exception {
                return conn.createStatement().executeQuery(
                    "SELECT CAST(COUNT(*) AS BIGINT) AS total_academias, "
                    + "CAST(COALESCE(SUM(COALESCE(total_alunos, 0)), 0) AS INTEGER) AS total_alunos, "
                    + "CAST(COALESCE(SUM(COALESCE(total_professores, 0)), 0) AS INTEGER) AS total_professores, "
                    + "CAST(SUM(CASE WHEN ativo = TRUE THEN 1 ELSE 0 END) AS INTEGER) AS academias_ativas, "
                    + "CAST(SUM(CASE WHEN ativo IS NOT TRUE THEN 1 ELSE 0 END) AS INTEGER) AS academias_inativas, "
                    + "CASE WHEN COUNT(*) > 0 "
                    + "  THEN CAST(COALESCE(SUM(COALESCE(total_alunos, 0)), 0) AS DOUBLE) / COUNT(*) "
                    + "  ELSE 0.0 END AS media_alunos "
                    + "FROM academia"
                );
            }
            $$
            """;

    private static final String H2_FN_RELATORIO = """
            CREATE ALIAS IF NOT EXISTS fn_relatorio_frequencia AS $$
            java.sql.ResultSet fn_relatorio_frequencia(java.sql.Connection conn, long academiaId, java.sql.Timestamp inicio, java.sql.Timestamp fim) throws Exception {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.nome, CAST(COUNT(*) AS BIGINT) "
                    + "FROM checkin c "
                    + "JOIN usuario u ON u.id = c.id_aluno "
                    + "WHERE c.id_academia = ? AND c.data_hora BETWEEN ? AND ? "
                    + "GROUP BY u.nome ORDER BY u.nome"
                );
                ps.setLong(1, academiaId);
                ps.setTimestamp(2, inicio);
                ps.setTimestamp(3, fim);
                return ps.executeQuery();
            }
            $$
            """;

    private static final String H2_FN_INATIVOS = """
            CREATE ALIAS IF NOT EXISTS fn_alunos_inativos AS $$
            java.sql.ResultSet fn_alunos_inativos(java.sql.Connection conn, long academiaId, int dias) throws Exception {
                java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT CAST(u.id AS BIGINT), u.nome, u.email, MAX(c.data_hora) "
                    + "FROM checkin c "
                    + "JOIN usuario u ON u.id = c.id_aluno "
                    + "WHERE c.id_academia = ? "
                    + "GROUP BY u.id, u.nome, u.email"
                );
                ps.setLong(1, academiaId);
                java.sql.ResultSet raw = ps.executeQuery();
                org.h2.tools.SimpleResultSet srs = new org.h2.tools.SimpleResultSet();
                srs.addColumn("ALUNO_ID", java.sql.Types.BIGINT, 20, 0);
                srs.addColumn("ALUNO_NOME", java.sql.Types.VARCHAR, 255, 0);
                srs.addColumn("EMAIL", java.sql.Types.VARCHAR, 255, 0);
                srs.addColumn("DIAS_SEM_TREINO", java.sql.Types.BIGINT, 20, 0);
                srs.addColumn("ATIVO", java.sql.Types.BOOLEAN, 1, 0);
                long millisPerDay = 86400000L;
                long now = System.currentTimeMillis();
                while (raw.next()) {
                    java.sql.Timestamp lastTraining = raw.getTimestamp(4);
                    if (lastTraining == null) continue;
                    long diffDays = (now - lastTraining.getTime()) / millisPerDay;
                    if (diffDays > dias) {
                        srs.addRow(raw.getLong(1), raw.getString(2), raw.getString(3), diffDays, false);
                    }
                }
                return srs;
            }
            $$
            """;

    @Override
    public void migrate(Context context) throws Exception {
        String db = context.getConnection().getMetaData().getDatabaseProductName();
        if (db != null && db.toUpperCase().contains("H2")) {
            try (Statement st = context.getConnection().createStatement()) {
                st.execute(H2_FN_DASHBOARD);
                st.execute(H2_FN_RELATORIO);
                st.execute(H2_FN_INATIVOS);
            }
            return;
        }
        try (Statement st = context.getConnection().createStatement()) {
            st.execute(FN_DASHBOARD);
            st.execute(FN_RELATORIO);
            st.execute(FN_INATIVOS);
        }
    }
}
