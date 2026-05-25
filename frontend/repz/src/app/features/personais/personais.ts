import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import {
  AcademiaResponse,
  AcademiaService,
  AuthService,
  PersonalResponse,
  PersonalService,
  PersonalUpdateRequest,
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

interface PersonalForm {
  id?: number;
  /** Só usado em criação (cadastra usuário). */
  name: string;
  email: string;
  password: string;
  especialidade: string;
  academiaId: number | null;
}

const FORM_VAZIO = (): PersonalForm => ({
  name: '',
  email: '',
  password: '',
  especialidade: '',
  academiaId: null,
});

@Component({
  selector: 'app-personais',
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
  templateUrl: './personais.html',
  styleUrl: './personais.scss',
})
export class Personais implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly service = inject(PersonalService);
  private readonly userService = inject(UserService);
  private readonly academiaService = inject(AcademiaService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly toast = inject(MessageService);

  readonly role = computed(() => this.auth.getUserRole());
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly lista = signal<PersonalResponse[]>([]);

  /** ADMIN: lista de academias para escolher contexto/criar; GERENTE: apenas a sua. */
  readonly academias = signal<AcademiaResponse[]>([]);
  readonly academiaSelecionada = signal<number | null>(null);

  readonly dialogAberto = signal(false);
  readonly modo = signal<Modo>('criar');
  readonly salvando = signal(false);
  readonly form = signal<PersonalForm>(FORM_VAZIO());
  readonly erroForm = signal<string | null>(null);

  ngOnInit(): void {
    if (this.isAdmin()) {
      this.academiaService.listar().subscribe({
        next: (res) => {
          this.academias.set(res ?? []);
          // Pré-seleciona a primeira academia ativa para ergonomia.
          const padrao = (res ?? []).find((a) => a.active) ?? res?.[0];
          if (padrao) {
            this.academiaSelecionada.set(padrao.id);
          }
          this.carregar();
        },
        error: () => {
          this.carregando.set(false);
          this.erro.set('Não foi possível carregar a lista de academias.');
        },
      });
    } else {
      // GERENTE: backend deduz academia pelo token; mas precisamos do id para criar usuário.
      this.academiaService.minhaAcademia().subscribe({
        next: (a) => {
          this.academiaSelecionada.set(a?.id ?? null);
          this.carregar();
        },
        error: () => this.carregar(),
      });
    }
  }

  trocarAcademia(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const academiaId = this.isAdmin() ? this.academiaSelecionada() : null;
    this.service.listar(academiaId).subscribe({
      next: (res) => {
        this.lista.set(res ?? []);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os personais.');
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

  editar(p: PersonalResponse): void {
    this.form.set({
      id: p.id,
      name: p.userName,
      email: p.email,
      password: '',
      especialidade: p.especialidade ?? '',
      academiaId: p.academiaId,
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
      if (formEl.invalid || !f.academiaId) {
        this.erroForm.set('Preencha todos os campos obrigatórios.');
        Object.values(formEl.controls).forEach((c) => c.markAsTouched());
        return;
      }
      if (f.password.length < 5) {
        this.erroForm.set('A senha precisa ter no mínimo 5 caracteres.');
        return;
      }
      this.salvando.set(true);
      // 1) Cria usuário PERSONAL → backend cria a entidade Personal vinculada.
      this.userService
        .criar({
          name: f.name,
          email: f.email,
          password: f.password,
          role: 'PERSONAL',
          academiaId: f.academiaId,
        })
        .subscribe({
          next: () => this.aplicarEspecialidadePosCriacao(f),
          error: (err) => {
            this.salvando.set(false);
            this.erroForm.set(err?.error?.message || 'Falha ao criar o personal.');
          },
        });
    } else {
      // Atualização: apenas especialidade (e ativo é tratado pelo botão).
      if (!f.id || !f.especialidade.trim()) {
        this.erroForm.set('Especialidade é obrigatória.');
        return;
      }
      this.salvando.set(true);
      const req: PersonalUpdateRequest = { especialidade: f.especialidade };
      this.service.atualizar(f.id, req, f.academiaId).subscribe({
        next: () => {
          this.salvando.set(false);
          this.dialogAberto.set(false);
          this.toast.add({ severity: 'success', summary: 'Personal atualizado', life: 3000 });
          this.carregar();
        },
        error: (err) => {
          this.salvando.set(false);
          this.erroForm.set(err?.error?.message || 'Falha ao atualizar o personal.');
        },
      });
    }
  }

  /** Depois de criar o usuário PERSONAL, localiza o registro recém-criado pelo
   *  e-mail e aplica a especialidade via PUT /api/personais/{id}. */
  private aplicarEspecialidadePosCriacao(f: PersonalForm): void {
    if (!f.especialidade.trim()) {
      this.finalizarCriacao();
      return;
    }
    this.service.listar(f.academiaId).subscribe({
      next: (lista) => {
        const novo = (lista ?? []).find((p) => p.email === f.email);
        if (!novo) {
          this.finalizarCriacao('Personal criado, mas não foi possível salvar a especialidade.');
          return;
        }
        this.service
          .atualizar(novo.id, { especialidade: f.especialidade }, f.academiaId)
          .subscribe({
            next: () => this.finalizarCriacao(),
            error: () =>
              this.finalizarCriacao('Personal criado, mas falhou ao gravar a especialidade.'),
          });
      },
      error: () => this.finalizarCriacao('Personal criado, mas a lista não pôde ser recarregada.'),
    });
  }

  private finalizarCriacao(mensagemAviso?: string): void {
    this.salvando.set(false);
    this.dialogAberto.set(false);
    this.toast.add({
      severity: mensagemAviso ? 'warn' : 'success',
      summary: mensagemAviso ?? 'Personal criado',
      life: 3500,
    });
    this.carregar();
  }

  confirmarToggleAtivo(p: PersonalResponse): void {
    const ativar = !p.ativo;
    this.confirmation.confirm({
      header: ativar ? 'Ativar personal' : 'Inativar personal',
      message: `Tem certeza que deseja ${ativar ? 'ativar' : 'inativar'} o personal "${p.userName}"?`,
      icon: ativar ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptLabel: ativar ? 'Ativar' : 'Inativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: ativar ? 'p-button-success' : 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.alterarStatus(p, ativar),
    });
  }

  private alterarStatus(p: PersonalResponse, ativar: boolean): void {
    const academiaId = this.isAdmin() ? this.academiaSelecionada() : null;
    const obs = ativar
      ? this.service.ativar(p.id, academiaId)
      : this.service.desativar(p.id, academiaId);
    obs.subscribe({
      next: () => {
        this.toast.add({
          severity: 'success',
          summary: ativar ? 'Personal ativado' : 'Personal inativado',
          life: 3000,
        });
        this.carregar();
      },
      error: () =>
        this.toast.add({
          severity: 'error',
          summary: ativar ? 'Falha ao ativar' : 'Falha ao inativar',
          life: 3500,
        }),
    });
  }
}
