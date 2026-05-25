import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth';

export function homePorRole(role: string | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'GERENTE':
      return '/academia';
    case 'PERSONAL':
      return '/personal';
    case 'ALUNO':
      return '/aluno/ficha-treino';
    default:
      return '/auth';
  }
}

export function checarAcesso(rolesPermitidos: string[]): boolean {
  if (!isPlatformBrowser(inject(PLATFORM_ID))) {
    return true;
  }

  const router = inject(Router);
  const auth = inject(AuthService);
  const role = auth.getUserRole();

  if (!role) {
    router.navigate(['/auth']);
    return false;
  }

  if (rolesPermitidos.includes(role)) {
    return true;
  }

  router.navigate([homePorRole(role)]);
  return false;
}
