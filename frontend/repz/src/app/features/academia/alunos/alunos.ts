import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  AcademiaService,
  AlunoService,
  PersonalService,
  PlanoService,
  UserService,
} from '@core/services';
import type {
  AcademiaResponse,
  AlunoDetalheResponse,
  AlunoUpdateRequest,
  PersonalResponse,
  PlanoResponse,
  UserCreateRequest,
  UserPutRequest,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-academia-alunos',
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
  templateUrl: './alunos.html',
  styleUrl: './alunos.scss',
})
export class AcademiaAlunos implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly alunoService = inject(AlunoService);
  private readonly personalService = inject(PersonalService);
  private readonly planoService = inject(PlanoService);
  private readonly academiaService = inject(AcademiaService);
  private readonly academia = signal<AcademiaResponse | null>(null);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly alunos = signal<AlunoDetalheResponse[]>([]);
  readonly planos = signal<PlanoResponse[]>([]);
  readonly personais = signal<PersonalResponse[]>([]);
  readonly busca = signal('');
  readonly alterandoId = signal<number | null>(null);

  readonly editando = signal<AlunoDetalheResponse | null>(null);
  readonly salvandoEdicao = signal(false);
  formNome = '';
  formEmail = '';
  formPlanoId: number | null = null;
  formPersonalId: number | null = null;
  formObjetivo = '';

  readonly cadastrando = signal(false);
  readonly salvandoCad = signal(false);
  cadNome = '';
  cadEmail = '';
  cadPlanoId: number | null = null;

  readonly alunosFiltrados = computed(() => {
    const q = this.busca().trim().toLowerCase();
    if (!q) return this.alunos();
    return this.alunos().filter(
      (a) => a.nome.toLowerCase().includes(q) || a.email.toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.userService.carregarNomeLogado();
    this.academiaService.minhaAcademia().subscribe({
      next: (a) => this.academia.set(a),
      error: () => {},
    });
    this.carregar();
  }

  abrirCadastro(): void {
    this.cadNome = '';
    this.cadEmail = '';
    const primeiroPlano = this.planos().find((p) => p.ativo) ?? this.planos()[0];
    this.cadPlanoId = primeiroPlano?.id ?? null;
    this.aviso.set(null);
    this.cadastrando.set(true);
  }

  fecharCadastro(): void {
    this.cadastrando.set(false);
  }

  salvarCadastro(): void {
    const academia = this.academia();
    if (!academia || this.salvandoCad()) return;
    if (!this.cadNome.trim() || !this.cadEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set('Nome e e-mail são obrigatórios.');
      return;
    }
    if (!this.cadPlanoId) {
      this.avisoSeverity.set('error');
      this.aviso.set('Selecione um plano.');
      return;
    }

    this.salvandoCad.set(true);
    const senha = 'Repz' + Math.floor(1000 + Math.random() * 9000);
    const req: UserCreateRequest = {
      name: this.cadNome.trim(),
      email: this.cadEmail.trim(),
      password: senha,
      role: 'ALUNO',
      academiaId: academia.id,
      planoId: this.cadPlanoId,
    };

    this.userService.criar(req).subscribe({
      next: () => {
        this.salvandoCad.set(false);
        this.cadastrando.set(false);
        this.avisoSeverity.set('success');
        this.aviso.set(`Aluno "${req.name}" cadastrado(a)! Senha temporária: ${senha}`);
        setTimeout(() => this.aviso.set(null), 6000);
        this.carregar();
      },
      error: (err) => {
        this.salvandoCad.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao cadastrar aluno.');
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({
      alunos: this.alunoService.listar(),
      planos: this.planoService.listar(),
      personais: this.personalService.listar(),
    }).subscribe({
      next: ({ alunos, planos, personais }) => {
        this.alunos.set(alunos);
        this.planos.set(planos);
        this.personais.set(personais);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os alunos.');
      },
    });
  }

  abrirEdicao(a: AlunoDetalheResponse): void {
    this.formNome = a.nome;
    this.formEmail = a.email;
    this.formPlanoId = a.planoId ?? null;
    this.formPersonalId = a.personalId ?? null;
    this.formObjetivo = a.objetivo ?? '';
    this.aviso.set(null);
    this.editando.set(a);
  }

  fecharEdicao(): void {
    this.editando.set(null);
  }

  private nomePlano(id: number | null): string | undefined {
    return this.planos().find((p) => p.id === id)?.nome;
  }

  private nomePersonal(id: number | null): string | undefined {
    return this.personais().find((p) => p.id === id)?.userName;
  }

  salvarEdicao(): void {
    const a = this.editando();
    if (!a || this.salvandoEdicao()) return;
    if (!this.formNome.trim() || !this.formEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set('Nome e e-mail são obrigatórios.');
      return;
    }
    this.salvandoEdicao.set(true);

    const userReq: UserPutRequest = {
      name: this.formNome.trim(),
      email: this.formEmail.trim(),
      role: 'ALUNO',
      active: a.ativo,
    };

    const alunoReq: AlunoUpdateRequest = {
      planoId: this.formPlanoId ?? undefined,
      personalId: this.formPersonalId ?? undefined,
      objetivo: this.formObjetivo.trim() || undefined,
    };

    this.userService.atualizar(a.userId, userReq).subscribe({
      next: () => {
        this.alunoService.atualizar(a.id, alunoReq).subscribe({
          next: () => {
            const nome = this.formNome.trim();
            const email = this.formEmail.trim();
            this.alunos.update((lista) =>
              lista.map((x) =>
                x.id === a.id
                  ? {
                      ...x,
                      nome,
                      email,
                      planoId: this.formPlanoId ?? undefined,
                      planoNome: this.nomePlano(this.formPlanoId),
                      personalId: this.formPersonalId ?? undefined,
                      personalNome: this.nomePersonal(this.formPersonalId),
                      objetivo: this.formObjetivo.trim() || undefined,
                    }
                  : x,
              ),
            );
            this.salvandoEdicao.set(false);
            this.editando.set(null);
            this.avisoSeverity.set('success');
            this.aviso.set(`Dados de "${nome}" atualizados.`);
            setTimeout(() => this.aviso.set(null), 3500);
          },
          error: (err) => {
            this.salvandoEdicao.set(false);
            this.avisoSeverity.set('error');
            this.aviso.set(err?.error?.message ?? 'Erro ao atualizar a matrícula.');
          },
        });
      },
      error: (err) => {
        this.salvandoEdicao.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao atualizar os dados do aluno.');
      },
    });
  }

  inativar(a: AlunoDetalheResponse): void {
    if (this.alterandoId() !== null) return;
    this.alterandoId.set(a.id);
    this.aviso.set(null);

    this.alunoService.inativar(a.id).subscribe({
      next: () => {
        this.alunos.update((lista) =>
          lista.map((x) => (x.id === a.id ? { ...x, ativo: false } : x)),
        );
        this.alterandoId.set(null);
        this.avisoSeverity.set('success');
        this.aviso.set(`Matrícula de "${a.nome}" inativada.`);
        setTimeout(() => this.aviso.set(null), 3500);
      },
      error: (err) => {
        this.alterandoId.set(null);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao inativar a matrícula.');
      },
    });
  }
}
