import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { effect } from '@angular/core';

export type Tema = 'dark' | 'light';

const CHAVE = 'repz-tema';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly tema = signal<Tema>(this.temaInicial());

  constructor() {
    effect(() => this.aplicar(this.tema()));
  }

  toggleTema(): void {
    this.tema.update((t) => (t === 'dark' ? 'light' : 'dark'));
  }

  private temaInicial(): Tema {
    if (!this.isBrowser) return 'dark';
    const salvo = localStorage.getItem(CHAVE) as Tema | null;
    if (salvo === 'dark' || salvo === 'light') return salvo;
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }

  private aplicar(tema: Tema): void {
    if (!this.isBrowser) return;
    document.documentElement.setAttribute('data-theme', tema);
    localStorage.setItem(CHAVE, tema);
  }
}
