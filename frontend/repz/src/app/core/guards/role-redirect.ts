import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth';

/** Rota inicial de cada perfil. */
export function homePorRole(role: string | null): string {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'ACADEMIA':
      return '/academia';
    case 'PERSONAL':
      return '/personal';
    case 'USUARIO':
    case 'ALUNO':
      return '/aluno/ficha-treino';
    default:
      return '/auth';
  }
}

/**
 * Lógica comum dos guards:
 *  - não autenticado  -> /auth
 *  - autenticado mas perfil errado -> home do próprio perfil (não /auth)
 *  - perfil permitido -> libera
 */
export function checarAcesso(rolesPermitidos: string[]): boolean {
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

  // Usuário autenticado sem permissão volta para a página inicial do próprio perfil.
  router.navigate([homePorRole(role)]);
  return false;
}
