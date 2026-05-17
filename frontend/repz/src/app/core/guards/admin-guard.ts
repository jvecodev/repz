import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

export const adminGuard: CanActivateFn = () => checarAcesso(['ADMIN']);
