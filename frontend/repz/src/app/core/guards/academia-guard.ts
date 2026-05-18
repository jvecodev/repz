import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

export const academiaGuard: CanActivateFn = () => checarAcesso(['GERENTE', 'ADMIN']);
