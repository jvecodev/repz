-- Alinha as tabelas de ficha de treino com os requisitos de visualização (RF24/RF19/RF20/RF25).
-- Aditiva e idempotente, no mesmo estilo da V10.

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT;

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS id_academia BIGINT;

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS objetivo VARCHAR(255);

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS observacoes VARCHAR(1000);

ALTER TABLE treino
    ADD COLUMN IF NOT EXISTS validade_ate DATE;

ALTER TABLE exercicio_treino
    ADD COLUMN IF NOT EXISTS grupo_muscular VARCHAR(100);

ALTER TABLE exercicio_treino
    ADD COLUMN IF NOT EXISTS descanso_segundos INTEGER;

ALTER TABLE exercicio_treino
    ADD COLUMN IF NOT EXISTS observacao VARCHAR(500);
