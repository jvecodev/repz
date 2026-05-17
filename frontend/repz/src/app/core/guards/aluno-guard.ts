import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

// USUARIO/ALUNO = dono da ficha; ADMIN tem visão geral.
export const alunoGuard: CanActivateFn = () =>
  checarAcesso(['USUARIO', 'ALUNO', 'ADMIN']);
