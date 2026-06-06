import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { PlanoRequest, PlanoResponse, PlanoService } from '@core/services';
import { AppShell } from '@shared/layout';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

interface PlanoForm {
  id?: number;
  nome: string;
  duracaoDias: number | null;
  valor: number | null;
}

const FORM_VAZIO = (): PlanoForm => ({ nome: '', duracaoDias: null, valor: null });

@Component({
  selector: 'app-planos',
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
    InputNumberModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './planos.html',
  styleUrl: './planos.scss',
})
export class AcademiaPlanos implements OnInit {
  private readonly service = inject(PlanoService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly toast = inject(MessageService);
  private readonly i18n = inject(TranslateService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly lista = signal<PlanoResponse[]>([]);

  readonly dialogAberto = signal(false);
  readonly salvando = signal(false);
  readonly editando = signal(false);
  readonly form = signal<PlanoForm>(FORM_VAZIO());
  readonly erroForm = signal<string | null>(null);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.service.listar().subscribe({
      next: (res) => {
        this.lista.set(res ?? []);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set(this.i18n.instant('ACADEMIA.PLANS.LOAD_ERROR'));
      },
    });
  }

  novo(): void {
    this.form.set(FORM_VAZIO());
    this.editando.set(false);
    this.erroForm.set(null);
    this.dialogAberto.set(true);
  }

  editar(p: PlanoResponse): void {
    this.form.set({ id: p.id, nome: p.nome, duracaoDias: p.duracaoDias, valor: p.valor });
    this.editando.set(true);
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
    if (formEl.invalid || !f.duracaoDias || !f.valor) {
      this.erroForm.set(this.i18n.instant('ACADEMIA.PLANS.FILL_REQUIRED'));
      Object.values(formEl.controls).forEach((c) => c.markAsTouched());
      return;
    }
    if (f.duracaoDias <= 0 || f.valor <= 0) {
      this.erroForm.set(this.i18n.instant('ACADEMIA.PLANS.POSITIVE_VALUES'));
      return;
    }
    this.erroForm.set(null);
    this.salvando.set(true);

    const payload: PlanoRequest = {
      nome: f.nome,
      duracaoDias: f.duracaoDias,
      valor: f.valor,
    };
    const obs = this.editando() && f.id
      ? this.service.atualizar(f.id, payload)
      : this.service.criar(payload);

    obs.subscribe({
      next: () => {
        this.salvando.set(false);
        this.dialogAberto.set(false);
        this.toast.add({
          severity: 'success',
          summary: this.i18n.instant(this.editando() ? 'ACADEMIA.PLANS.PLAN_UPDATED' : 'ACADEMIA.PLANS.PLAN_CREATED'),
          life: 3000,
        });
        this.carregar();
      },
      error: (err) => {
        this.salvando.set(false);
        this.erroForm.set(
          err?.error?.message ||
            this.i18n.instant(this.editando() ? 'ACADEMIA.PLANS.UPDATE_FAIL' : 'ACADEMIA.PLANS.CREATE_FAIL'),
        );
      },
    });
  }

  confirmarToggleAtivo(p: PlanoResponse): void {
    const ativar = !p.ativo;
    this.confirmation.confirm({
      header: this.i18n.instant(ativar ? 'ACADEMIA.PLANS.ACTIVATE_PLAN' : 'ACADEMIA.PLANS.DEACTIVATE_PLAN'),
      message: this.i18n.instant(
        ativar ? 'ACADEMIA.PLANS.CONFIRM_ACTIVATE' : 'ACADEMIA.PLANS.CONFIRM_DEACTIVATE',
        { nome: p.nome },
      ),
      icon: ativar ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptLabel: this.i18n.instant(ativar ? 'ADMIN.DASH.ACTIVATE' : 'ADMIN.DASH.DEACTIVATE'),
      rejectLabel: this.i18n.instant('COMMON.CANCEL'),
      acceptButtonStyleClass: ativar ? 'p-button-success' : 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.alterarStatus(p, ativar),
    });
  }

  private alterarStatus(p: PlanoResponse, ativar: boolean): void {
    const obs = ativar ? this.service.ativar(p.id) : this.service.desativar(p.id);
    obs.subscribe({
      next: () => {
        this.toast.add({
          severity: 'success',
          summary: this.i18n.instant(ativar ? 'ACADEMIA.PLANS.PLAN_ACTIVATED' : 'ACADEMIA.PLANS.PLAN_DEACTIVATED'),
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
