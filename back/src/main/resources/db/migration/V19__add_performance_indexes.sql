-- ============================================================
-- RNF05 - Indices de performance
-- Postgres nao cria indice automatico em coluna de FK (apenas PK/unique).
-- Indices nas FKs e nos campos de busca/ordenacao mais frequentes das
-- telas principais (listagens, dashboard, relatorio de frequencia,
-- grafico de evolucao). Idempotente via IF NOT EXISTS.
-- Obs.: em tabelas grandes em producao, criar com CREATE INDEX
-- CONCURRENTLY (fora de transacao) para evitar lock de escrita.
-- ============================================================

-- aluno: listagens por academia / personal / usuario
CREATE INDEX IF NOT EXISTS idx_aluno_id_academia ON aluno (id_academia);
CREATE INDEX IF NOT EXISTS idx_aluno_id_personal ON aluno (id_personal);
CREATE INDEX IF NOT EXISTS idx_aluno_id_usuario  ON aluno (id_usuario);
CREATE INDEX IF NOT EXISTS idx_aluno_plano_id    ON aluno (plano_id);
CREATE INDEX IF NOT EXISTS idx_aluno_ativo       ON aluno (ativo);

-- checkin (entidade Frequencia): relatorio de frequencia e historico
CREATE INDEX IF NOT EXISTS idx_checkin_aluno_data    ON checkin (id_aluno, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_checkin_academia_data ON checkin (id_academia, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_checkin_id_personal   ON checkin (id_personal);

-- avaliacao_fisica: grafico de evolucao por aluno
CREATE INDEX IF NOT EXISTS idx_avfis_usuario_data ON avaliacao_fisica (id_usuario, data_avaliacao);
CREATE INDEX IF NOT EXISTS idx_avfis_id_academia  ON avaliacao_fisica (id_academia);
CREATE INDEX IF NOT EXISTS idx_avfis_id_personal  ON avaliacao_fisica (id_personal);

-- treino: ficha ativa/inativa do aluno
CREATE INDEX IF NOT EXISTS idx_treino_usuario_ativo ON treino (id_usuario, ativo);
CREATE INDEX IF NOT EXISTS idx_treino_id_personal   ON treino (id_personal);
CREATE INDEX IF NOT EXISTS idx_treino_id_academia   ON treino (id_academia);

-- exercicio_treino: carregamento dos exercicios da ficha
CREATE INDEX IF NOT EXISTS idx_extreino_id_treino ON exercicio_treino (id_treino);

-- personal: lookup por usuario / academia
CREATE INDEX IF NOT EXISTS idx_personal_id_usuario  ON personal (id_usuario);
CREATE INDEX IF NOT EXISTS idx_personal_id_academia ON personal (id_academia);

-- solicitacao_ficha: caixa de entrada do personal / status do aluno
CREATE INDEX IF NOT EXISTS idx_solic_aluno_status    ON solicitacao_ficha (id_aluno, status);
CREATE INDEX IF NOT EXISTS idx_solic_personal_status ON solicitacao_ficha (id_personal, status);

-- usuario: filtro de soft-delete + ativo
CREATE INDEX IF NOT EXISTS idx_usuario_ativo_delecao ON usuario (ativo, data_delecao);

-- arquivo / password_reset_token: FKs
CREATE INDEX IF NOT EXISTS idx_arquivo_user_id ON arquivo (user_id);
CREATE INDEX IF NOT EXISTS idx_prt_user_id     ON password_reset_token (user_id);
