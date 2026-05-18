-- Renomeia os valores de perfil: ACADEMIA → GERENTE, USUARIO → ALUNO
UPDATE usuario SET perfil = 'GERENTE' WHERE perfil = 'ACADEMIA';
UPDATE usuario SET perfil = 'ALUNO'   WHERE perfil = 'USUARIO';
