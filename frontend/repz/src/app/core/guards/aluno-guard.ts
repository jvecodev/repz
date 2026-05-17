import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

// USUARIO/ALUNO representa o dono da ficha; ADMIN mantém visão geral.
export const alunoGuard: CanActivateFn = () => checarAcesso(['USUARIO', 'ALUNO', 'ADMIN']);
