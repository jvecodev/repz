import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LayoutService } from '@core/services/layout';
import { ButtonModule } from 'primeng/button';

export interface NavItem {
  key: string;
  label: string;
  link: string;
}

const NAV: NavItem[] = [
  { key: 'dashboard', label: 'Dashboard', link: '/aluno' },
  { key: 'treino', label: 'Meu treino', link: '/aluno/ficha-treino' },
  { key: 'frequencia', label: 'Frequência', link: '/aluno/frequencia' },
  { key: 'evolucao', label: 'Evolução', link: '/aluno/evolucao' },
  { key: 'perfil', label: 'Perfil', link: '/aluno/perfil' },
];

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, ButtonModule],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
  host: { '[class.is-collapsed]': 'layout.colapsada()' },
})
export class AppSidebar {
  protected readonly layout = inject(LayoutService);

  /** Nome exibido no bloco de perfil */
  readonly nome = input<string>('Usuário');
  /** Subtítulo/objetivo exibido acima do nome */
  readonly subtitulo = input<string>('');
  /** key do item de navegação ativo */
  readonly ativo = input<string>('');

  /** Card CTA (rodapé) */
  readonly ctaTitulo = input<string>('Bora treinar!');
  readonly ctaDescricao = input<string>('Registre sua presença e mantenha a sequência.');
  readonly ctaLabel = input<string>('Fazer check-in');
  readonly mostrarCta = input<boolean>(true);

  readonly ctaClick = output<void>();

  readonly navItems = NAV;

  readonly inicial = computed(() => (this.nome().trim()[0] ?? 'U').toUpperCase());
}
