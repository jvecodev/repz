-- ============================================================
-- RNF05 - Stored procedures (functions) para queries complexas
-- Move agregacoes que hoje sao feitas em memoria (Java stream)
-- para o banco, reduzindo I/O e tempo de resposta das telas.
-- ============================================================

-- Metricas do dashboard de academias.
-- Substitui o carregamento de TODAS as academias + soma em memoria
-- (AcademiaServiceImpl.obterDashboard). Mantem a mesma semantica:
-- soma das colunas denormalizadas total_alunos / total_professores,
-- contagem de ativas/inativas e media por academia.
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

-- Relatorio de frequencia: total de check-ins por aluno num periodo.
-- Substitui findByAcademiaIdAndPeriodo + groupingByConcurrent em memoria
-- (FrequenciaServiceImpl.obterRelatorio).
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

-- Alunos inativos: sem check-in ha mais de p_dias na academia.
-- Substitui frequenciaRepository.findAll() + agrupamento em memoria
-- (FrequenciaServiceImpl.obterAlunosInativos). Dias inteiros truncados,
-- equivalente a ChronoUnit.DAYS.between(ultimo, agora).
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
