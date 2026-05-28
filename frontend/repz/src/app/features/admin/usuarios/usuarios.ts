import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AcademiaService, AuthService, UserService } from '@core/services';
import type { AcademiaResponse, UserGetResponse, UserPutRequest, UserRole } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

type FiltroRole = 'TODOS' | UserRole;

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    ButtonModule,
    CardModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class AdminUsuarios implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly academiaService = inject(AcademiaService);

  readonly academias = signal<AcademiaResponse[]>([]);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly usuarios = signal<UserGetResponse[]>([]);
  readonly busca = signal('');
  readonly filtroRole = signal<FiltroRole>('TODOS');
  readonly alterandoId = signal<number | null>(null);

  readonly roles: { label: string; value: FiltroRole }[] = [
    { label: 'Todos', value: 'TODOS' },
    { label: 'Admin', value: 'ADMIN' },
    { label: 'Gerente', value: 'GERENTE' },
    { label: 'Personal', value: 'PERSONAL' },
    { label: 'Aluno', value: 'ALUNO' },
  ];

  readonly rolesEdicao: { label: string; value: UserRole }[] = [
    { label: 'Admin', value: 'ADMIN' },
    { label: 'Gerente', value: 'GERENTE' },
    { label: 'Personal', value: 'PERSONAL' },
    { label: 'Aluno', value: 'ALUNO' },
  ];

  readonly editando = signal<UserGetResponse | null>(null);
  readonly salvandoEdicao = signal(false);
  formNome = '';
  formEmail = '';
  formRole: UserRole = 'ALUNO';
  formAcademiaId: number | null = null;

  private readonly meuId = computed(() => this.auth.sessao()?.id ?? null);

  readonly usuariosFiltrados = computed(() => {
    const q = this.busca().trim().toLowerCase();
    const role = this.filtroRole();
    return this.usuarios().filter((u) => {
      const matchRole = role === 'TODOS' || u.role === role;
      const matchBusca =
        !q || u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
      return matchRole && matchBusca;
    });
  });

  ngOnInit(): void {
    this.userService.carregarNomeLogado();
    this.carregar();
    this.academiaService.listar().subscribe({
      next: (lista) => this.academias.set(lista),
      error: () => {},
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.userService.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os usuários.');
      },
    });
  }

  ehProprioUsuario(u: UserGetResponse): boolean {
    return this.meuId() === u.id;
  }

  rotuloRole(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'Admin';
      case 'GERENTE':
        return 'Gerente';
      case 'PERSONAL':
        return 'Personal';
      case 'ALUNO':
        return 'Aluno';
      default:
        return role;
    }
  }

  classeTagRole(role: UserRole): string {
    return role === 'ADMIN' || role === 'GERENTE' ? 'repz-tag-success' : 'repz-tag-muted';
  }

  podeEditar(u: UserGetResponse): boolean {
    return u.role !== 'ADMIN' || this.ehProprioUsuario(u);
  }

  editandoProprioPerfil(): boolean {
    const e = this.editando();
    return !!e && this.ehProprioUsuario(e);
  }

  abrirEdicao(u: UserGetResponse): void {
    if (!this.podeEditar(u)) return;
    this.formNome = u.name;
    this.formEmail = u.email;
    this.formRole = u.role;
    this.formAcademiaId = (u as any).academiaId ?? null;
    this.aviso.set(null);
    this.editando.set(u);
  }

  precisaAcademia(): boolean {
    return this.formRole === 'GERENTE' || this.formRole === 'PERSONAL' || this.formRole === 'ALUNO';
  }

  fecharEdicao(): void {
    this.editando.set(null);
  }

  salvarEdicao(): void {
    const u = this.editando();
    if (!u || this.salvandoEdicao()) return;
    if (!this.formNome.trim() || !this.formEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set('Nome e e-mail são obrigatórios.');
      return;
    }
    this.salvandoEdicao.set(true);

    const req: UserPutRequest = {
      name: this.formNome.trim(),
      email: this.formEmail.trim(),
      role: this.editandoProprioPerfil() ? u.role : this.formRole,
      active: u.active,
      academiaId: this.precisaAcademia() ? (this.formAcademiaId ?? undefined) : undefined,
    };

    this.userService.atualizar(u.id, req).subscribe({
      next: () => {
        this.usuarios.update((lista) =>
          lista.map((x) =>
            x.id === u.id ? { ...x, name: req.name, email: req.email, role: req.role! } : x,
          ),
        );
        this.salvandoEdicao.set(false);
        this.editando.set(null);
        this.avisoSeverity.set('success');
        this.aviso.set(`Usuário "${req.name}" atualizado.`);
        setTimeout(() => this.aviso.set(null), 3500);
      },
      error: (err) => {
        this.salvandoEdicao.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao atualizar usuário.');
      },
    });
  }

  alternarStatus(u: UserGetResponse): void {
    if (this.alterandoId() !== null || this.ehProprioUsuario(u)) return;
    this.alterandoId.set(u.id);
    this.aviso.set(null);

    const op = u.active ? this.userService.desativar(u.id) : this.userService.ativar(u.id);

    op.subscribe({
      next: () => {
        this.usuarios.update((lista) =>
          lista.map((x) => (x.id === u.id ? { ...x, active: !x.active } : x)),
        );
        this.alterandoId.set(null);
        this.avisoSeverity.set('success');
        this.aviso.set(`Usuário "${u.name}" ${u.active ? 'desativado' : 'ativado'}.`);
        setTimeout(() => this.aviso.set(null), 3500);
      },
      error: (err) => {
        this.alterandoId.set(null);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao alterar status do usuário.');
      },
    });
  }
}
