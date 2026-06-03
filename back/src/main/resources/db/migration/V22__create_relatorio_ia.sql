CREATE TABLE relatorio_ia (
    id          BIGSERIAL PRIMARY KEY,
    aluno_id    INTEGER      NOT NULL REFERENCES usuario(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    conteudo    TEXT,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP  NOT NULL DEFAULT NOW()
);
