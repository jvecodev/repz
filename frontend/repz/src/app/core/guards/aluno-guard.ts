import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

export const alunoGuard: CanActivateFn = () => checarAcesso(['ALUNO', 'ADMIN']);
