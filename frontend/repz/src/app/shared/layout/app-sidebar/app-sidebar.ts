import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AcademiaService } from '@core/services/academia';
import { AlunoService } from '@core/services/aluno';
import { AuthService } from '@core/services/auth';
import { LayoutService } from '@core/services/layout';
import { UserService } from '@core/services/user';
import { ButtonModule } from 'primeng/button';
import { TranslatePipe } from '@ngx-translate/core';
import { PersonalService } from '@core/services';

export interface NavItem {
  key: string;
  /** Chave de tradução do rótulo (ex.: NAV.DASHBOARD). */
  labelKey: string;
  link: string;
  queryParams?: Record<string, string>;
}

const NAV_ALUNO: NavItem[] = [
  { key: 'dashboard', labelKey: 'NAV.DASHBOARD', link: '/aluno' },
  { key: 'treino', labelKey: 'NAV.MY_WORKOUT', link: '/aluno/ficha-treino' },
  { key: 'frequencia', labelKey: 'NAV.ATTENDANCE', link: '/aluno/frequencia' },
  { key: 'evolucao', labelKey: 'NAV.EVOLUTION', link: '/aluno/evolucao' },
];

const NAV_PERSONAL: NavItem[] = [
  { key: 'dashboard', labelKey: 'NAV.PANEL', link: '/personal' },
  { key: 'alunos', labelKey: 'NAV.MY_STUDENTS', link: '/personal/alunos' },
  { key: 'fichas', labelKey: 'NAV.WORKOUTS', link: '/personal/alunos', queryParams: { foco: 'ficha' } },
  { key: 'avaliacoes', labelKey: 'NAV.ASSESSMENTS', link: '/personal/alunos', queryParams: { foco: 'avaliacao' } },
];

const NAV_ADMIN: NavItem[] = [
  { key: 'dashboard', labelKey: 'NAV.DASHBOARD', link: '/admin' },
  { key: 'academias', labelKey: 'NAV.GYMS', link: '/admin/academias' },
  { key: 'usuarios', labelKey: 'NAV.USERS', link: '/admin/usuarios' },
];

const NAV_GERENTE: NavItem[] = [
  { key: 'dashboard', labelKey: 'NAV.PANEL', link: '/academia' },
  { key: 'alunos', labelKey: 'NAV.STUDENTS', link: '/academia/alunos' },
  { key: 'personais', labelKey: 'NAV.TRAINERS', link: '/academia/personais' },
  { key: 'planos', labelKey: 'NAV.PLANS', link: '/academia/planos' },
  { key: 'relatorios', labelKey: 'NAV.REPORTS', link: '/academia/relatorios' },
];

const PERFIL_LINK_BY_ROLE: Record<string, string> = {
  ALUNO: '/aluno/perfil',
  PERSONAL: '/personal/perfil',
  ADMIN: '/admin/perfil',
  GERENTE: '/academia/perfil',
};

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonModule, TranslatePipe],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.scss',
  host: { '[class.is-collapsed]': 'layout.colapsada()' },
})
export class AppSidebar implements OnInit {
  protected readonly layout = inject(LayoutService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly academiaService = inject(AcademiaService);
  private readonly personalService = inject(PersonalService);
  private readonly alunoService = inject(AlunoService);
  public readonly userService = inject(UserService);

  readonly _academiaNomeCarregado = signal<string>('');

  readonly nome = input<string>('Usuário');

  readonly subtitulo = input<string>('');

  readonly academiaNome = computed(() => this._academiaNomeCarregado());

  readonly fotoUrl = computed(() => this.userService.fotoUrl());

  readonly ativo = input<string>('');

  readonly ctaTitulo = input<string>('SIDEBAR.CTA_TITLE');
  readonly ctaDescricao = input<string>('SIDEBAR.CTA_DESC');
  readonly ctaLabel = input<string>('SIDEBAR.CTA_LABEL');
  readonly mostrarCta = input<boolean>(true);

  readonly ctaClick = output<void>();

  readonly navItems = computed<NavItem[]>(() => {
    const role = this.auth.getUserRole();
    if (role === 'PERSONAL') return NAV_PERSONAL;
    if (role === 'ADMIN') return NAV_ADMIN;
    if (role === 'GERENTE') return NAV_GERENTE;
    return NAV_ALUNO;
  });

  readonly perfilLink = computed<string>(() => {
    const role = this.auth.getUserRole() ?? 'ALUNO';
    return PERFIL_LINK_BY_ROLE[role] ?? '/aluno/perfil';
  });

  readonly inicial = computed(() => (this.nome().trim()[0] ?? 'U').toUpperCase());

  ngOnInit(): void {
    this.userService.carregarNomeLogado();

    const role = this.auth.getUserRole();
    if (role === 'GERENTE') {
      this.academiaService
        .minhaAcademia()
        .pipe(catchError(() => of(null)))
        .subscribe((a) => {
          if (a?.name) this._academiaNomeCarregado.set(a.name);
        });
    } else if (role === 'PERSONAL') {
      this.personalService
        .meuPerfil()
        .pipe(catchError(() => of(null)))
        .subscribe((p) => {
          if (p?.academiaNome) this._academiaNomeCarregado.set(p.academiaNome);
        });
    } else if (role === 'ALUNO') {
      this.alunoService
        .meuPerfil()
        .pipe(catchError(() => of(null)))
        .subscribe((a) => {
          if (a?.academiaNome) this._academiaNomeCarregado.set(a.academiaNome);
        });
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }
}
