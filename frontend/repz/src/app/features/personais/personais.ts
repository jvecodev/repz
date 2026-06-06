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
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
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
    TranslatePipe,
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
  private readonly i18n = inject(TranslateService);

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
          this.erro.set(this.i18n.instant('MGMT.TRAINERS.GYMS_LOAD_ERROR'));
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
        this.erro.set(this.i18n.instant('MGMT.TRAINERS.LOAD_ERROR'));
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
        this.erroForm.set(this.i18n.instant('MGMT.TRAINERS.FILL_REQUIRED'));
        Object.values(formEl.controls).forEach((c) => c.markAsTouched());
        return;
      }
      if (f.password.length < 5) {
        this.erroForm.set(this.i18n.instant('MGMT.STUDENTS.PASSWORD_MIN'));
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
            this.erroForm.set(err?.error?.message || this.i18n.instant('MGMT.TRAINERS.CREATE_FAIL'));
          },
        });
    } else {
      // Atualização: apenas especialidade (e ativo é tratado pelo botão).
      if (!f.id || !f.especialidade.trim()) {
        this.erroForm.set(this.i18n.instant('MGMT.TRAINERS.SPECIALTY_REQUIRED'));
        return;
      }
      this.salvando.set(true);
      const req: PersonalUpdateRequest = { especialidade: f.especialidade };
      this.service.atualizar(f.id, req, f.academiaId).subscribe({
        next: () => {
          this.salvando.set(false);
          this.dialogAberto.set(false);
          this.toast.add({ severity: 'success', summary: this.i18n.instant('MGMT.TRAINERS.TRAINER_UPDATED'), life: 3000 });
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
          this.finalizarCriacao(this.i18n.instant('MGMT.TRAINERS.PARTIAL_NO_SPECIALTY'));
          return;
        }
        this.service
          .atualizar(novo.id, { especialidade: f.especialidade }, f.academiaId)
          .subscribe({
            next: () => this.finalizarCriacao(),
            error: () =>
              this.finalizarCriacao(this.i18n.instant('MGMT.TRAINERS.PARTIAL_SPECIALTY_FAIL')),
          });
      },
      error: () => this.finalizarCriacao(this.i18n.instant('MGMT.TRAINERS.PARTIAL_RELOAD_FAIL')),
    });
  }

  private finalizarCriacao(mensagemAviso?: string): void {
    this.salvando.set(false);
    this.dialogAberto.set(false);
    this.toast.add({
      severity: mensagemAviso ? 'warn' : 'success',
      summary: mensagemAviso ?? this.i18n.instant('MGMT.TRAINERS.TRAINER_CREATED'),
      life: 3500,
    });
    this.carregar();
  }

  confirmarToggleAtivo(p: PersonalResponse): void {
    const ativar = !p.ativo;
    this.confirmation.confirm({
      header: this.i18n.instant(ativar ? 'MGMT.TRAINERS.ACTIVATE_TRAINER' : 'MGMT.TRAINERS.DEACTIVATE_TRAINER'),
      message: this.i18n.instant(
        ativar ? 'MGMT.TRAINERS.CONFIRM_ACTIVATE' : 'MGMT.TRAINERS.CONFIRM_DEACTIVATE',
        { nome: p.userName },
      ),
      icon: ativar ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptLabel: this.i18n.instant(ativar ? 'ADMIN.DASH.ACTIVATE' : 'ADMIN.DASH.DEACTIVATE'),
      rejectLabel: this.i18n.instant('COMMON.CANCEL'),
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
          summary: this.i18n.instant(ativar ? 'MGMT.TRAINERS.TRAINER_ACTIVATED' : 'MGMT.TRAINERS.TRAINER_DEACTIVATED'),
          life: 3000,
        });
        this.carregar();
      },
      error: () =>
        this.toast.add({
          severity: 'error',
          summary: this.i18n.instant(ativar ? 'ADMIN.DASH.ACTIVATE_FAIL' : 'ADMIN.DASH.DEACTIVATE_FAIL'),
          life: 3500,
        }),
    });
  }
}
