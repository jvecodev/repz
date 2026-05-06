UPDATE plano
SET academia_id = (
    SELECT a.id
    FROM academia a
    WHERE a.id_usuario_responsavel = plano.academia_id
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1
    FROM academia a
    WHERE a.id_usuario_responsavel = plano.academia_id
);

CREATE INDEX IF NOT EXISTS idx_plano_academia_id ON plano(academia_id);
