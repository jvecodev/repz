import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type Idioma = 'pt-BR' | 'en-US';

const LANG_KEY = 'repz_lang';
const IDIOMAS: Idioma[] = ['pt-BR', 'en-US'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly translate = inject(TranslateService);

  readonly idioma = signal<Idioma>('pt-BR');

  constructor() {
    this.translate.addLangs(IDIOMAS);
    this.translate.setDefaultLang('pt-BR');
    this.usar(this.lerSalvo() ?? 'pt-BR');
  }

  usar(idioma: Idioma): void {
    this.idioma.set(idioma);
    this.translate.use(idioma);
    if (this.isBrowser()) {
      localStorage.setItem(LANG_KEY, idioma);
    }
  }

  alternar(): void {
    this.usar(this.idioma() === 'pt-BR' ? 'en-US' : 'pt-BR');
  }

  private lerSalvo(): Idioma | null {
    if (!this.isBrowser()) return null;
    const valor = localStorage.getItem(LANG_KEY);
    return valor === 'pt-BR' || valor === 'en-US' ? valor : null;
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
}
