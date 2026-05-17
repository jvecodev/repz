import { CanActivateFn } from '@angular/router';
import { checarAcesso } from './role-redirect';

export const personalGuard: CanActivateFn = () =>
  checarAcesso(['PERSONAL', 'ADMIN']);
