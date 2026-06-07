import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class LayoutService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  // Inicia fechada em mobile para não cobrir o conteúdo na primeira renderização
  readonly colapsada = signal(
    this.isBrowser ? window.innerWidth <= 900 : false,
  );

  toggleSidebar(): void {
    this.colapsada.update((v) => !v);
  }

  recolher(): void {
    this.colapsada.set(true);
  }

  expandir(): void {
    this.colapsada.set(false);
  }

  // Fecha a sidebar apenas em mobile (não afeta o collapse de desktop)
  fecharMobile(): void {
    if (this.isBrowser && window.innerWidth <= 900) {
      this.colapsada.set(true);
    }
  }
}
