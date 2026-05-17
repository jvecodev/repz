CREATE TABLE solicitacao_ficha (
    id            BIGSERIAL    PRIMARY KEY,
    id_aluno      BIGINT       NOT NULL,
    id_personal   BIGINT,
    mensagem      VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    resposta      VARCHAR(500),
    criada_em     TIMESTAMP    NOT NULL,
    respondida_em TIMESTAMP,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_sol_aluno    FOREIGN KEY (id_aluno)    REFERENCES usuario(id),
    CONSTRAINT fk_sol_personal FOREIGN KEY (id_personal) REFERENCES personal(id)
);
