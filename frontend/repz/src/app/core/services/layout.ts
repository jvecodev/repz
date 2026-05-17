import { Injectable, signal } from '@angular/core';

/**
 * Estado compartilhado do layout (sidebar recolhida/expandida).
 * Reutilizável por qualquer tela que use o AppShell.
 */
@Injectable({ providedIn: 'root' })
export class LayoutService {
  readonly colapsada = signal(false);

  toggleSidebar(): void {
    this.colapsada.update((v) => !v);
  }

  recolher(): void {
    this.colapsada.set(true);
  }

  expandir(): void {
    this.colapsada.set(false);
  }
}
