-- -- ============================================================
-- -- SEED DE TESTE (migration V999 — roda por último, após V18)
-- -- Cria um aluno completo com dados em todas as tabelas.
-- -- Login:  aluno@repz.com / 12345   |   personal@repz.com / 12345
-- -- Idempotente: limpa o seed anterior antes de reinserir.
-- -- ============================================================
-- DO $$
-- DECLARE
--   v_hash       TEXT := '$2b$10$zR6a.vEQ9a8DdVUu/jjXaeNQhJ/8VHa/paPQBbTZQnoRlnAV/wgUa'; -- senha: 12345
--   v_uid_aluno  BIGINT;
--   v_uid_pers   BIGINT;
--   v_academia   BIGINT;
--   v_personal   BIGINT;
--   v_plano      BIGINT;
--   v_aluno      BIGINT;
--   v_treino_a   BIGINT;
--   v_treino_b   BIGINT;
--   d            INT;
-- BEGIN
--   -- limpa seed anterior (ordem segura de FK)
--   DELETE FROM avaliacao_fisica WHERE id_usuario IN (SELECT id FROM usuario WHERE email IN ('aluno@repz.com','personal@repz.com'));
--   DELETE FROM checkin WHERE id_aluno IN (SELECT id FROM aluno WHERE id_usuario IN (SELECT id FROM usuario WHERE email='aluno@repz.com'));
--   DELETE FROM exercicio_treino WHERE id_treino IN (SELECT id FROM treino WHERE id_usuario IN (SELECT id FROM usuario WHERE email='aluno@repz.com'));
--   DELETE FROM treino WHERE id_usuario IN (SELECT id FROM usuario WHERE email='aluno@repz.com');
--   DELETE FROM aluno WHERE id_usuario IN (SELECT id FROM usuario WHERE email='aluno@repz.com');
--   DELETE FROM personal WHERE id_usuario IN (SELECT id FROM usuario WHERE email='personal@repz.com');
--   DELETE FROM plano WHERE nome = 'Plano Teste';
--   DELETE FROM academia WHERE cnpj = '99999999000199';
--   DELETE FROM usuario WHERE email IN ('aluno@repz.com','personal@repz.com');

--   -- usuários
--   INSERT INTO usuario (nome, email, senha, perfil, ativo)
--     VALUES ('Lucas Aluno', 'aluno@repz.com', v_hash, 'ALUNO', TRUE)
--     RETURNING id INTO v_uid_aluno;

--   INSERT INTO usuario (nome, email, senha, perfil, ativo)
--     VALUES ('Marina Personal', 'personal@repz.com', v_hash, 'PERSONAL', TRUE)
--     RETURNING id INTO v_uid_pers;

--   -- academia
--   INSERT INTO academia (cnpj, nome, endereco, responsavel, ativo)
--     VALUES ('99999999000199', 'Academia Teste', 'Rua Teste, 100', 'Resp Teste', TRUE)
--     RETURNING id INTO v_academia;

--   -- personal
--   INSERT INTO personal (especialidades, ativo, id_usuario, id_academia)
--     VALUES ('Hipertrofia', TRUE, v_uid_pers, v_academia)
--     RETURNING id INTO v_personal;

--   -- plano
--   INSERT INTO plano (nome, duracao_dias, valor, ativo, academia_id)
--     VALUES ('Plano Teste', 30, 99.90, TRUE, v_academia)
--     RETURNING id INTO v_plano;

--   -- aluno
--   INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
--     VALUES (CURRENT_DATE - 90, 'Hipertrofia', v_uid_aluno, v_academia, v_personal, v_plano, TRUE, '11999999999')
--     RETURNING id INTO v_aluno;

--   -- treinos (divisões A e B) + exercícios
--   INSERT INTO treino (nome, divisao, objetivo, observacoes, validade_ate, ativo, id_usuario, id_personal, id_academia)
--     VALUES ('Treino A — Peito e Tríceps', 'A', 'Hipertrofia muscular', 'Aquecer 8 min antes.', CURRENT_DATE + 60, TRUE, v_uid_aluno, v_personal, v_academia)
--     RETURNING id INTO v_treino_a;
--   INSERT INTO treino (nome, divisao, objetivo, observacoes, validade_ate, ativo, id_usuario, id_personal, id_academia)
--     VALUES ('Treino B — Costas e Bíceps', 'B', 'Hipertrofia muscular', 'Aquecer 8 min antes.', CURRENT_DATE + 60, TRUE, v_uid_aluno, v_personal, v_academia)
--     RETURNING id INTO v_treino_b;

--   INSERT INTO exercicio_treino (nome_exercicio, grupo_muscular, series, repeticoes, carga_kg, descanso_segundos, ordem, observacao, id_treino) VALUES
--     ('Supino reto com barra', 'Peito', 4, '8-10', 70, 90, 1, 'Controlar a descida', v_treino_a),
--     ('Supino inclinado halter', 'Peito', 3, '10-12', 24, 75, 2, NULL, v_treino_a),
--     ('Tríceps corda', 'Tríceps', 4, '12', 32, 60, 3, NULL, v_treino_a),
--     ('Puxada frente', 'Costas', 4, '10', 55, 90, 1, NULL, v_treino_b),
--     ('Remada curvada', 'Costas', 4, '8-10', 50, 90, 2, 'Pegada pronada', v_treino_b),
--     ('Rosca direta', 'Bíceps', 4, '10-12', 14, 60, 3, NULL, v_treino_b);

--   -- avaliações físicas (5 datas, evolução visível no gráfico)
--   FOR d IN 0..4 LOOP
--     INSERT INTO avaliacao_fisica
--       (peso_kg, altura_cm, imc, percentual_gordura, medidas,
--        cintura_cm, quadril_cm, braco_cm, coxa_cm,
--        id_usuario, id_personal, id_academia, data_avaliacao, ativo)
--     VALUES
--       (84 - d*1.5, 178, ROUND(((84 - d*1.5) / (1.78*1.78))::numeric, 2), 22 - d*1.2, NULL,
--        90 - d*1.5, 100 - d, 36 + d*0.3, 58 + d*0.2,
--        v_uid_aluno, v_personal, v_academia,
--        (CURRENT_TIMESTAMP - ((4-d) * INTERVAL '21 days')), TRUE);
--   END LOOP;

--   -- check-ins (últimos 10 dias alternados)
--   FOR d IN 0..9 LOOP
--     IF d % 2 = 0 THEN
--       INSERT INTO checkin (data_hora, id_aluno, ativo)
--         VALUES (CURRENT_TIMESTAMP - (d * INTERVAL '1 day'), v_aluno, TRUE);
--     END IF;
--   END LOOP;

--   RAISE NOTICE 'Seed OK -> usuario aluno id=% (login aluno@repz.com), personal id=%', v_uid_aluno, v_personal;
-- END $$;
