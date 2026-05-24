import { Component, computed, inject, input, output } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services/auth';
import { LayoutService } from '@core/services/layout';
import { ButtonModule } from 'primeng/button';

export interface NavItem {
  key: string;
  label: string;
  link: string;
  queryParams?: Record<string, string>;
}

const NAV_ALUNO: NavItem[] = [
  { key: 'dashboard', label: 'Dashboard', link: '/aluno' },
  { key: 'treino', label: 'Meu treino', link: '/aluno/ficha-treino' },
  { key: 'frequencia', label: 'Frequência', link: '/aluno/frequencia' },
  { key: 'evolucao', label: 'Evolução', link: '/aluno/evolucao' },
  { key: 'perfil', label: 'Perfil', link: '/aluno/perfil' },
];

const NAV_PERSONAL: NavItem[] = [
  { key: 'dashboard', label: 'Painel', link: '/personal' },
  { key: 'alunos', label: 'Meus alunos', link: '/personal/alunos' },
  { key: 'fichas', label: 'Fichas', link: '/personal/alunos', queryParams: { foco: 'ficha' } },
  { key: 'avaliacoes', label: 'Avaliações', link: '/personal/alunos', queryParams: { foco: 'avaliacao' } },
  { key: 'perfil', label: 'Perfil', link: '/personal/perfil' },
];

const NAV_ADMIN: NavItem[] = [
  { key: 'dashboard', label: 'Dashboard', link: '/admin' },
  { key: 'academias', label: 'Academias', link: '/admin/academias' },
  { key: 'usuarios', label: 'Usuários', link: '/admin/usuarios' },
  { key: 'perfil', label: 'Perfil', link: '/admin/perfil' },
];

const NAV_GERENTE: NavItem[] = [
  { key: 'dashboard', label: 'Painel', link: '/academia' },
  { key: 'alunos', label: 'Alunos', link: '/academia/alunos' },
  { key: 'personais', label: 'Personais', link: '/academia/personais' },
  { key: 'planos', label: 'Planos', link: '/academia/planos' },
  { key: 'relatorios', label: 'Relatórios', link: '/academia/relatorios' },
  { key: 'perfil', label: 'Perfil', link: '/academia/perfil' },
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
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly nome = input<string>('Usuário');

  readonly subtitulo = input<string>('');

  readonly ativo = input<string>('');

  readonly ctaTitulo = input<string>('Bora treinar!');
  readonly ctaDescricao = input<string>('Registre sua presença e mantenha a sequência.');
  readonly ctaLabel = input<string>('Fazer check-in');
  readonly mostrarCta = input<boolean>(true);

  readonly ctaClick = output<void>();

  readonly navItems = computed<NavItem[]>(() => {
    const role = this.auth.getUserRole();
    if (role === 'PERSONAL') return NAV_PERSONAL;
    if (role === 'ADMIN') return NAV_ADMIN;
    if (role === 'GERENTE') return NAV_GERENTE;
    return NAV_ALUNO;
  });

  readonly inicial = computed(() => (this.nome().trim()[0] ?? 'U').toUpperCase());

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }
}
