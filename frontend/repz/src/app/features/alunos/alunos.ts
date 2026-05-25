import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import {
  AcademiaResponse,
  AcademiaService,
  AlunoDetalheResponse,
  AlunoService,
  AlunoUpdateRequest,
  AuthService,
  PersonalResponse,
  PersonalService,
  PlanoResponse,
  PlanoService,
  UserService,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

type Modo = 'criar' | 'editar';

interface AlunoForm {
  id?: number;
  userId?: number;
  // criação
  name: string;
  email: string;
  password: string;
  // ambos
  academiaId: number | null;
  planoId: number | null;
  personalId: number | null;
  objetivo: string;
}

const FORM_VAZIO = (): AlunoForm => ({
  name: '',
  email: '',
  password: '',
  academiaId: null,
  planoId: null,
  personalId: null,
  objetivo: '',
});

@Component({
  selector: 'app-alunos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    ButtonModule,
    CardModule,
    ConfirmDialogModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    SelectModule,
    TableModule,
    TagModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './alunos.html',
  styleUrl: './alunos.scss',
})
export class Alunos implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly service = inject(AlunoService);
  private readonly planoService = inject(PlanoService);
  private readonly personalService = inject(PersonalService);
  private readonly userService = inject(UserService);
  private readonly academiaService = inject(AcademiaService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly toast = inject(MessageService);

  readonly role = computed(() => this.auth.getUserRole());
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly lista = signal<AlunoDetalheResponse[]>([]);

  readonly academias = signal<AcademiaResponse[]>([]);
  readonly academiaSelecionada = signal<number | null>(null);
  readonly planos = signal<PlanoResponse[]>([]);
  readonly personais = signal<PersonalResponse[]>([]);

  readonly dialogAberto = signal(false);
  readonly modo = signal<Modo>('criar');
  readonly salvando = signal(false);
  readonly form = signal<AlunoForm>(FORM_VAZIO());
  readonly erroForm = signal<string | null>(null);

  ngOnInit(): void {
    if (this.isAdmin()) {
      this.academiaService.listar().subscribe({
        next: (res) => {
          this.academias.set(res ?? []);
          const padrao = (res ?? []).find((a) => a.active) ?? res?.[0];
          if (padrao) this.academiaSelecionada.set(padrao.id);
          this.recarregarTudo();
        },
        error: () => {
          this.carregando.set(false);
          this.erro.set('Não foi possível carregar as academias.');
        },
      });
    } else {
      this.academiaService.minhaAcademia().subscribe({
        next: (a) => {
          this.academiaSelecionada.set(a?.id ?? null);
          this.recarregarTudo();
        },
        error: () => this.recarregarTudo(),
      });
    }
  }

  trocarAcademia(): void {
    this.recarregarTudo();
  }

  /** Recarrega alunos + planos + personais da academia atual. */
  private recarregarTudo(): void {
    this.carregar();
    this.recarregarReferencias();
  }

  private recarregarReferencias(): void {
    const id = this.isAdmin() ? this.academiaSelecionada() : null;
    this.planoService.listar(id).subscribe({
      next: (res) => this.planos.set((res ?? []).filter((p) => p.ativo)),
      error: () => this.planos.set([]),
    });
    this.personalService.listar(id).subscribe({
      next: (res) => this.personais.set((res ?? []).filter((p) => p.ativo)),
      error: () => this.personais.set([]),
    });
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const id = this.isAdmin() ? this.academiaSelecionada() : null;
    this.service.listar(id).subscribe({
      next: (res) => {
        this.lista.set(res ?? []);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os alunos.');
      },
    });
  }

  novo(): void {
    const f = FORM_VAZIO();
    f.academiaId = this.academiaSelecionada();
    this.form.set(f);
    this.modo.set('criar');
    this.erroForm.set(null);
    this.dialogAberto.set(true);
  }

  editar(a: AlunoDetalheResponse): void {
    this.form.set({
      id: a.id,
      userId: a.userId,
      name: a.nome,
      email: a.email,
      password: '',
      academiaId: a.academiaId,
      planoId: a.planoId ?? null,
      personalId: a.personalId ?? null,
      objetivo: a.objetivo ?? '',
    });
    this.modo.set('editar');
    this.erroForm.set(null);
    this.dialogAberto.set(true);
  }

  fecharDialog(): void {
    if (this.salvando()) return;
    this.dialogAberto.set(false);
  }

  salvar(formEl: NgForm): void {
    if (this.salvando()) return;
    const f = this.form();

    if (this.modo() === 'criar') {
      if (!f.name || !f.email || !f.password || !f.academiaId || !f.planoId) {
        this.erroForm.set('Nome, e-mail, senha, academia e plano são obrigatórios.');
        Object.values(formEl.controls).forEach((c) => c.markAsTouched());
        return;
      }
      if (f.password.length < 5) {
        this.erroForm.set('A senha precisa ter no mínimo 5 caracteres.');
        return;
      }
      this.salvando.set(true);
      this.userService
        .criar({
          name: f.name,
          email: f.email,
          password: f.password,
          role: 'ALUNO',
          academiaId: f.academiaId,
          planoId: f.planoId,
        })
        .subscribe({
          next: () => this.aplicarPersonalEObjetivoPosCriacao(f),
          error: (err) => {
            this.salvando.set(false);
            this.erroForm.set(err?.error?.message || 'Falha ao criar o aluno.');
          },
        });
    } else {
      if (!f.id) return;
      this.salvando.set(true);
      const req: AlunoUpdateRequest = {
        planoId: f.planoId ?? undefined,
        personalId: f.personalId ?? undefined,
        objetivo: f.objetivo || undefined,
      };
      this.service.atualizar(f.id, req, f.academiaId).subscribe({
        next: () => {
          this.salvando.set(false);
          this.dialogAberto.set(false);
          this.toast.add({ severity: 'success', summary: 'Aluno atualizado', life: 3000 });
          this.carregar();
        },
        error: (err) => {
          this.salvando.set(false);
          this.erroForm.set(err?.error?.message || 'Falha ao atualizar o aluno.');
        },
      });
    }
  }

  /** Se o usuário informou personal/objetivo no momento da criação,
   *  busca o aluno recém-criado pelo e-mail e aplica via PUT. */
  private aplicarPersonalEObjetivoPosCriacao(f: AlunoForm): void {
    if (!f.personalId && !f.objetivo) {
      this.finalizarCriacao();
      return;
    }
    this.service.listar(f.academiaId).subscribe({
      next: (lista) => {
        const novo = (lista ?? []).find((a) => a.email === f.email);
        if (!novo) {
          this.finalizarCriacao();
          return;
        }
        const req: AlunoUpdateRequest = {
          planoId: f.planoId ?? undefined,
          personalId: f.personalId ?? undefined,
          objetivo: f.objetivo || undefined,
        };
        this.service.atualizar(novo.id, req, f.academiaId).subscribe({
          next: () => this.finalizarCriacao(),
          error: () =>
            this.finalizarCriacao('Aluno criado, mas falhou ao gravar personal/objetivo.'),
        });
      },
      error: () => this.finalizarCriacao(),
    });
  }

  private finalizarCriacao(mensagemAviso?: string): void {
    this.salvando.set(false);
    this.dialogAberto.set(false);
    this.toast.add({
      severity: mensagemAviso ? 'warn' : 'success',
      summary: mensagemAviso ?? 'Aluno criado',
      life: 3500,
    });
    this.carregar();
  }

  confirmarInativar(a: AlunoDetalheResponse): void {
    this.confirmation.confirm({
      header: 'Inativar aluno',
      message: `Tem certeza que deseja inativar o aluno "${a.nome}"?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Inativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () =>
        this.service.inativar(a.id, this.isAdmin() ? this.academiaSelecionada() : null).subscribe({
          next: () => {
            this.toast.add({ severity: 'success', summary: 'Aluno inativado', life: 3000 });
            this.carregar();
          },
          error: () =>
            this.toast.add({ severity: 'error', summary: 'Falha ao inativar', life: 3500 }),
        }),
    });
  }

  /**
   * Como AlunoController não expõe /ativar, a reativação acontece via
   * UserController.ativar(userId) — o User é o registro fonte do "ativo".
   */
  confirmarReativar(a: AlunoDetalheResponse): void {
    this.confirmation.confirm({
      header: 'Reativar aluno',
      message: `Tem certeza que deseja reativar o aluno "${a.nome}"?`,
      icon: 'pi pi-check-circle',
      acceptLabel: 'Reativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () =>
        this.userService.ativar(a.userId).subscribe({
          next: () => {
            this.toast.add({ severity: 'success', summary: 'Aluno reativado', life: 3000 });
            this.carregar();
          },
          error: () =>
            this.toast.add({ severity: 'error', summary: 'Falha ao reativar', life: 3500 }),
        }),
    });
  }
}
