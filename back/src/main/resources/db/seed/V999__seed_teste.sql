-- ============================================================
-- SEED DE TESTE EXPANDIDO (migration V999 — roda por último)
-- Cobre todos os perfis pra testar o sistema inteiro.
--
-- LOGINS (senha de todos: 12345)
--   ADMIN     → admin@repz.com
--   GERENTE   → gerente@repz.com      (Marcos Pinto — responsável da Iron Fit)
--   PERSONAL  → personal@repz.com     (Marina — Hipertrofia)
--   PERSONAL  → renata@repz.com       (Renata — Funcional)
--   ALUNO     → aluno@repz.com        (Lucas Oliveira — ficha + avaliações + check-ins)
--             aluno2@repz.com         (Mariana Costa)
--             aluno3@repz.com         (Pedro Henrique)
--             aluno4@repz.com         (Tiago Moreira — ausente >14d)
--             aluno5@repz.com         (Júlia Fernandes)
--             aluno6@repz.com         (Bruno Ferreira)
--             aluno7@repz.com         (Camila Ribeiro)
--
-- Academia: Iron Fit (CNPJ 99999999000199)
-- Planos: Mensal Básico · Trimestral · Anual Premium · Day Use (inativo)
-- Idempotente: limpa o seed anterior antes de reinserir.
-- ============================================================
DO $$
DECLARE
  v_hash          TEXT := '$2b$10$zR6a.vEQ9a8DdVUu/jjXaeNQhJ/8VHa/paPQBbTZQnoRlnAV/wgUa'; -- senha: 12345

  v_uid_admin     BIGINT;
  v_uid_gerente   BIGINT;
  v_uid_marina    BIGINT;
  v_uid_renata    BIGINT;
  v_uid_lucas     BIGINT;
  v_uid_a2        BIGINT;
  v_uid_a3        BIGINT;
  v_uid_a4        BIGINT;
  v_uid_a5        BIGINT;
  v_uid_a6        BIGINT;
  v_uid_a7        BIGINT;

  v_academia      BIGINT;
  v_pers_marina   BIGINT;
  v_pers_renata   BIGINT;

  v_plano_mensal  BIGINT;
  v_plano_trim    BIGINT;
  v_plano_anual   BIGINT;
  v_plano_day     BIGINT;

  v_aluno_lucas   BIGINT;
  v_aluno_a2      BIGINT;
  v_aluno_a3      BIGINT;
  v_aluno_a4      BIGINT;
  v_aluno_a5      BIGINT;
  v_aluno_a6      BIGINT;
  v_aluno_a7      BIGINT;

  v_treino_a      BIGINT;
  v_treino_b      BIGINT;
  v_treino_c      BIGINT;

  v_emails        TEXT[] := ARRAY[
    'admin@repz.com', 'gerente@repz.com',
    'personal@repz.com', 'renata@repz.com',
    'aluno@repz.com', 'aluno2@repz.com', 'aluno3@repz.com', 'aluno4@repz.com',
    'aluno5@repz.com', 'aluno6@repz.com', 'aluno7@repz.com'
  ];

  d INT;
BEGIN
  -- ============================================================
  -- LIMPEZA (ordem FK-safe)
  -- ============================================================
  DELETE FROM checkin
    WHERE id_aluno IN (
      SELECT id FROM aluno WHERE id_usuario IN (
        SELECT id FROM usuario WHERE email = ANY(v_emails)
      )
    );

  DELETE FROM exercicio_treino
    WHERE id_treino IN (
      SELECT id FROM treino WHERE id_usuario IN (
        SELECT id FROM usuario WHERE email = ANY(v_emails)
      )
    );

  DELETE FROM treino
    WHERE id_usuario IN (SELECT id FROM usuario WHERE email = ANY(v_emails));

  DELETE FROM avaliacao_fisica
    WHERE id_usuario IN (SELECT id FROM usuario WHERE email = ANY(v_emails));

  DELETE FROM aluno
    WHERE id_usuario IN (SELECT id FROM usuario WHERE email = ANY(v_emails));

  DELETE FROM personal
    WHERE id_usuario IN (SELECT id FROM usuario WHERE email = ANY(v_emails));

  DELETE FROM plano
    WHERE academia_id IN (SELECT id FROM academia WHERE cnpj = '99999999000199');

  DELETE FROM academia WHERE cnpj = '99999999000199';

  DELETE FROM usuario WHERE email = ANY(v_emails);

  -- ============================================================
  -- USUÁRIOS
  -- ============================================================
  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Admin Repz', 'admin@repz.com', v_hash, 'ADMIN', TRUE)
    RETURNING id INTO v_uid_admin;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Marcos Pinto', 'gerente@repz.com', v_hash, 'GERENTE', TRUE)
    RETURNING id INTO v_uid_gerente;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Marina Mendes', 'personal@repz.com', v_hash, 'PERSONAL', TRUE)
    RETURNING id INTO v_uid_marina;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Renata Souza', 'renata@repz.com', v_hash, 'PERSONAL', TRUE)
    RETURNING id INTO v_uid_renata;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Lucas Oliveira', 'aluno@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_lucas;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Mariana Costa', 'aluno2@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a2;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Pedro Henrique', 'aluno3@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a3;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Tiago Moreira', 'aluno4@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a4;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Julia Fernandes', 'aluno5@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a5;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Bruno Ferreira', 'aluno6@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a6;

  INSERT INTO usuario (nome, email, senha, perfil, ativo) VALUES
    ('Camila Ribeiro', 'aluno7@repz.com', v_hash, 'ALUNO', TRUE)
    RETURNING id INTO v_uid_a7;

  -- ============================================================
  -- ACADEMIA (Iron Fit — Marcos Pinto como responsável/gerente)
  -- ============================================================
  INSERT INTO academia (cnpj, nome, endereco, responsavel, ativo, id_usuario_responsavel)
    VALUES ('99999999000199', 'Iron Fit', 'Av. Paulista, 1500 — São Paulo/SP',
            'Marcos Pinto', TRUE, v_uid_gerente)
    RETURNING id INTO v_academia;

  -- ============================================================
  -- PERSONAIS
  -- ============================================================
  INSERT INTO personal (especialidades, ativo, id_usuario, id_academia) VALUES
    ('Hipertrofia', TRUE, v_uid_marina, v_academia)
    RETURNING id INTO v_pers_marina;

  INSERT INTO personal (especialidades, ativo, id_usuario, id_academia) VALUES
    ('Funcional', TRUE, v_uid_renata, v_academia)
    RETURNING id INTO v_pers_renata;

  -- ============================================================
  -- PLANOS (3 ativos + 1 inativo para testar o "Ativar")
  -- ============================================================
  INSERT INTO plano (nome, duracao_dias, valor, ativo, academia_id) VALUES
    ('Mensal Básico', 30, 149.00, TRUE, v_academia)
    RETURNING id INTO v_plano_mensal;

  INSERT INTO plano (nome, duracao_dias, valor, ativo, academia_id) VALUES
    ('Trimestral', 90, 399.00, TRUE, v_academia)
    RETURNING id INTO v_plano_trim;

  INSERT INTO plano (nome, duracao_dias, valor, ativo, academia_id) VALUES
    ('Anual Premium', 365, 1290.00, TRUE, v_academia)
    RETURNING id INTO v_plano_anual;

  INSERT INTO plano (nome, duracao_dias, valor, ativo, academia_id) VALUES
    ('Day Use', 1, 39.00, FALSE, v_academia)
    RETURNING id INTO v_plano_day;

  -- ============================================================
  -- ALUNOS (matriculados em planos e personais variados)
  -- ============================================================
  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 200, 'Hipertrofia', v_uid_lucas, v_academia, v_pers_marina, v_plano_anual, TRUE, '11999991111')
    RETURNING id INTO v_aluno_lucas;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 75, 'Emagrecimento', v_uid_a2, v_academia, v_pers_marina, v_plano_trim, TRUE, '11999992222')
    RETURNING id INTO v_aluno_a2;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 320, 'Performance', v_uid_a3, v_academia, v_pers_marina, v_plano_anual, TRUE, '11999993333')
    RETURNING id INTO v_aluno_a3;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 40, 'Condicionamento', v_uid_a4, v_academia, NULL, v_plano_mensal, TRUE, '11999994444')
    RETURNING id INTO v_aluno_a4;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 30, 'Tonificação', v_uid_a5, v_academia, v_pers_renata, v_plano_mensal, TRUE, '11999995555')
    RETURNING id INTO v_aluno_a5;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 100, 'Hipertrofia', v_uid_a6, v_academia, v_pers_renata, v_plano_trim, TRUE, '11999996666')
    RETURNING id INTO v_aluno_a6;

  INSERT INTO aluno (data_inicio, objetivo, id_usuario, id_academia, id_personal, plano_id, ativo, telefone)
    VALUES (CURRENT_DATE - 50, 'Mobilidade', v_uid_a7, v_academia, v_pers_renata, v_plano_mensal, TRUE, '11999997777')
    RETURNING id INTO v_aluno_a7;

  -- ============================================================
  -- TREINOS DO LUCAS (3 divisões completas)
  -- ============================================================
  INSERT INTO treino (nome, divisao, objetivo, observacoes, validade_ate, ativo, id_usuario, id_personal, id_academia)
    VALUES ('Treino A — Peito e Tríceps', 'A', 'Hipertrofia muscular',
            'Aquecer 8 min na esteira antes de iniciar.', CURRENT_DATE + 60, TRUE,
            v_uid_lucas, v_pers_marina, v_academia)
    RETURNING id INTO v_treino_a;

  INSERT INTO treino (nome, divisao, objetivo, observacoes, validade_ate, ativo, id_usuario, id_personal, id_academia)
    VALUES ('Treino B — Costas e Bíceps', 'B', 'Hipertrofia muscular',
            'Aquecer 8 min na esteira antes de iniciar.', CURRENT_DATE + 60, TRUE,
            v_uid_lucas, v_pers_marina, v_academia)
    RETURNING id INTO v_treino_b;

  INSERT INTO treino (nome, divisao, objetivo, observacoes, validade_ate, ativo, id_usuario, id_personal, id_academia)
    VALUES ('Treino C — Pernas e Ombros', 'C', 'Hipertrofia muscular',
            'Foco em volume; dropset na última série de cada exercício.', CURRENT_DATE + 60, TRUE,
            v_uid_lucas, v_pers_marina, v_academia)
    RETURNING id INTO v_treino_c;

  INSERT INTO exercicio_treino
    (nome_exercicio, grupo_muscular, series, repeticoes, carga_kg, descanso_segundos, ordem, observacao, id_treino) VALUES
    -- Treino A
    ('Supino reto com barra',   'Peito',   4, '8-10',  70, 90, 1, 'Controlar a descida', v_treino_a),
    ('Supino inclinado halter', 'Peito',   3, '10-12', 24, 75, 2, NULL,                   v_treino_a),
    ('Crucifixo na máquina',    'Peito',   3, '12-15', 40, 60, 3, NULL,                   v_treino_a),
    ('Tríceps corda',           'Tríceps', 4, '12',    32, 60, 4, NULL,                   v_treino_a),
    ('Tríceps testa',           'Tríceps', 3, '10-12', 20, 60, 5, NULL,                   v_treino_a),
    -- Treino B
    ('Puxada frente',           'Costas',  4, '10',    55, 90, 1, NULL,                   v_treino_b),
    ('Remada curvada',          'Costas',  4, '8-10',  50, 90, 2, 'Pegada pronada',       v_treino_b),
    ('Remada baixa',            'Costas',  4, '10-12', 45, 75, 3, NULL,                   v_treino_b),
    ('Rosca direta',            'Bíceps',  4, '10-12', 14, 60, 4, NULL,                   v_treino_b),
    ('Rosca martelo',           'Bíceps',  3, '12',    12, 60, 5, NULL,                   v_treino_b),
    -- Treino C
    ('Agachamento livre',       'Pernas',  4, '8-10',  80, 120, 1, 'Profundidade 90°',    v_treino_c),
    ('Leg press 45°',           'Pernas',  4, '10-12', 180, 90, 2, NULL,                  v_treino_c),
    ('Cadeira extensora',       'Pernas',  3, '12-15', 50, 60, 3, NULL,                   v_treino_c),
    ('Mesa flexora',            'Pernas',  3, '12-15', 45, 60, 4, NULL,                   v_treino_c),
    ('Desenvolvimento halter',  'Ombros',  4, '10',    18, 75, 5, NULL,                   v_treino_c),
    ('Elevação lateral',        'Ombros',  4, '12-15', 10, 45, 6, NULL,                   v_treino_c);

  -- ============================================================
  -- AVALIAÇÕES FÍSICAS — Lucas (5 datas com evolução)
  -- ============================================================
  FOR d IN 0..4 LOOP
    INSERT INTO avaliacao_fisica
      (peso_kg, altura_cm, imc, percentual_gordura, medidas,
       cintura_cm, quadril_cm, braco_cm, coxa_cm,
       id_usuario, id_personal, id_academia, data_avaliacao, ativo)
    VALUES
      (84 - d * 1.5, 178, ROUND(((84 - d * 1.5) / (1.78 * 1.78))::numeric, 2),
       22 - d * 1.2, NULL,
       90 - d * 1.5, 100 - d, 36 + d * 0.3, 58 + d * 0.2,
       v_uid_lucas, v_pers_marina, v_academia,
       (CURRENT_TIMESTAMP - ((4 - d) * INTERVAL '21 days')), TRUE);
  END LOOP;

  -- Avaliações pontuais pra outros alunos (1 cada)
  INSERT INTO avaliacao_fisica
    (peso_kg, altura_cm, imc, percentual_gordura, cintura_cm, quadril_cm, braco_cm, coxa_cm,
     id_usuario, id_personal, id_academia, data_avaliacao, ativo) VALUES
    (62, 165, 22.77, 24.0, 78, 96, 28, 54, v_uid_a2, v_pers_marina, v_academia, CURRENT_TIMESTAMP - INTERVAL '10 days', TRUE),
    (90, 188, 25.46, 18.0, 92, 102, 40, 62, v_uid_a3, v_pers_marina, v_academia, CURRENT_TIMESTAMP - INTERVAL '5 days',  TRUE),
    (58, 162, 22.10, 26.0, 76, 95, 27, 52, v_uid_a5, v_pers_renata, v_academia, CURRENT_TIMESTAMP - INTERVAL '15 days', TRUE),
    (75, 175, 24.49, 20.0, 86, 98, 34, 58, v_uid_a6, v_pers_renata, v_academia, CURRENT_TIMESTAMP - INTERVAL '20 days', TRUE);

  -- ============================================================
  -- CHECK-INS — distribuição variada por aluno + horários
  -- (alimenta o gráfico "Ocupação por horário" dos relatórios)
  -- ============================================================

  -- Lucas: 12 check-ins, hours 6..20 (alterna)
  FOR d IN 0..11 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 2)) + INTERVAL '6 hours' + ((d % 8) * INTERVAL '2 hours'),
       v_aluno_lucas, TRUE);
  END LOOP;

  -- Mariana: 14 check-ins, manhã (7-9h)
  FOR d IN 0..13 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 2)) + INTERVAL '7 hours' + ((d % 3) * INTERVAL '1 hour'),
       v_aluno_a2, TRUE);
  END LOOP;

  -- Pedro: 16 check-ins, noite (18-21h)
  FOR d IN 0..15 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 2)) + INTERVAL '18 hours' + ((d % 4) * INTERVAL '1 hour'),
       v_aluno_a3, TRUE);
  END LOOP;

  -- Tiago: ausente — 1 check-in há 28 dias
  INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
    ((CURRENT_DATE - 28) + INTERVAL '10 hours', v_aluno_a4, TRUE);

  -- Júlia: 9 check-ins, horários variados (8-16h)
  FOR d IN 0..8 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 3)) + INTERVAL '8 hours' + ((d % 5) * INTERVAL '2 hours'),
       v_aluno_a5, TRUE);
  END LOOP;

  -- Bruno: 3 check-ins, noite
  FOR d IN 0..2 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 7)) + INTERVAL '19 hours', v_aluno_a6, TRUE);
  END LOOP;

  -- Camila: 11 check-ins, noite (17-19h)
  FOR d IN 0..10 LOOP
    INSERT INTO checkin (data_hora, id_aluno, ativo) VALUES
      ((CURRENT_DATE - (d * 2)) + INTERVAL '17 hours' + ((d % 3) * INTERVAL '1 hour'),
       v_aluno_a7, TRUE);
  END LOOP;

  RAISE NOTICE 'Seed expandido OK -> 11 usuários (1 admin, 1 gerente, 2 personais, 7 alunos), Iron Fit, 4 planos, 3 treinos, 9 avaliações, ~66 check-ins.';
END $$;
