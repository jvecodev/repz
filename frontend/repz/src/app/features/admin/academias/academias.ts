import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { AcademiaRequest, AcademiaResponse, AcademiaService } from '@core/services';
import { validarCNPJ } from '@core/validators/cpf-cnpj';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

interface AcademiaForm extends AcademiaRequest {
  id?: number;
}

const FORM_VAZIO = (): AcademiaForm => ({
  cnpj: '',
  name: '',
  address: '',
  responsible: '',
  email: '',
  phone: '',
});

@Component({
  selector: 'app-academias',
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
    TableModule,
    TagModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './academias.html',
  styleUrl: './academias.scss',
})
export class AdminAcademias implements OnInit {
  private readonly service = inject(AcademiaService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly toast = inject(MessageService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly lista = signal<AcademiaResponse[]>([]);

  readonly dialogAberto = signal(false);
  readonly salvando = signal(false);
  readonly editando = signal(false);
  readonly form = signal<AcademiaForm>(FORM_VAZIO());
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
        this.erro.set('Não foi possível carregar as academias.');
      },
    });
  }

  novo(): void {
    this.form.set(FORM_VAZIO());
    this.editando.set(false);
    this.erroForm.set(null);
    this.dialogAberto.set(true);
  }

  editar(a: AcademiaResponse): void {
    this.form.set({
      id: a.id,
      cnpj: a.cnpj,
      name: a.name,
      address: a.address,
      responsible: a.responsible,
      email: a.email ?? '',
      phone: a.phone ?? '',
    });
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
    if (formEl.invalid) {
      this.erroForm.set('Verifique os campos obrigatórios.');
      Object.values(formEl.controls).forEach((c) => c.markAsTouched());
      return;
    }
    const f = this.form();
    const cnpjLimpo = f.cnpj.replace(/\D/g, '');
    if (!validarCNPJ(cnpjLimpo)) {
      this.erroForm.set('CNPJ inválido. Verifique o número informado.');
      return;
    }
    this.erroForm.set(null);
    this.salvando.set(true);

    const payload: AcademiaRequest = {
      cnpj: cnpjLimpo,
      name: f.name,
      address: f.address,
      responsible: f.responsible,
      email: f.email || undefined,
      phone: f.phone || undefined,
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
          summary: this.editando() ? 'Academia atualizada' : 'Academia criada',
          life: 3000,
        });
        this.carregar();
      },
      error: (err) => {
        this.salvando.set(false);
        this.erroForm.set(
          err?.error?.message ||
            (this.editando() ? 'Falha ao atualizar a academia.' : 'Falha ao criar a academia.'),
        );
      },
    });
  }

  confirmarToggleAtivo(a: AcademiaResponse): void {
    const ativar = !a.active;
    this.confirmation.confirm({
      header: ativar ? 'Ativar academia' : 'Inativar academia',
      message: `Tem certeza que deseja ${ativar ? 'ativar' : 'inativar'} a academia "${a.name}"?`,
      icon: ativar ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptLabel: ativar ? 'Ativar' : 'Inativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: ativar ? 'p-button-success' : 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.alterarStatus(a, ativar),
    });
  }

  private alterarStatus(a: AcademiaResponse, ativar: boolean): void {
    const obs = ativar ? this.service.ativar(a.id) : this.service.desativar(a.id);
    obs.subscribe({
      next: () => {
        this.toast.add({
          severity: 'success',
          summary: ativar ? 'Academia ativada' : 'Academia inativada',
          life: 3000,
        });
        this.carregar();
      },
      error: () => {
        this.toast.add({
          severity: 'error',
          summary: ativar ? 'Falha ao ativar' : 'Falha ao inativar',
          life: 3500,
        });
      },
    });
  }
}
