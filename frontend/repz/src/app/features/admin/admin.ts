import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AcademiaService,
  AlunoService,
  FrequenciaService,
  PersonalService,
  UserService,
} from '@core/services';
import type {
  AcademiaDashboardResponse,
  AcademiaResponse,
  AlunoDetalheResponse,
  FrequenciaResponse,
  PersonalResponse,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

interface AcademiaRow {
  id: number;
  name: string;
  responsible: string;
  active: boolean;
  alunosAtivos: number;
  personaisAtivos: number;
  frequenciaMedia: number;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    AppShell,
    ButtonModule,
    CardModule,
    ConfirmDialogModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly academiaService = inject(AcademiaService);
  private readonly alunoService = inject(AlunoService);
  private readonly personalService = inject(PersonalService);
  private readonly freqService = inject(FrequenciaService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly toast = inject(MessageService);

  readonly carregando = signal(true);
  readonly carregandoTabela = signal(true);
  readonly erro = signal<string | null>(null);
  readonly dashboard = signal<AcademiaDashboardResponse | null>(null);
  readonly rows = signal<AcademiaRow[]>([]);

  readonly totalAcademias = computed(() => this.rows().length);
  readonly ativas = computed(() => this.rows().filter((r) => r.active).length);
  readonly inativas = computed(() => this.rows().filter((r) => !r.active).length);

  ngOnInit(): void {
    this.userService.carregarNomeLogado();

    this.academiaService.dashboard().subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os indicadores.');
      },
    });

    this.carregarTabela();
  }

  private carregarTabela(): void {
    this.carregandoTabela.set(true);
    this.academiaService
      .listar()
      .pipe(catchError(() => of([] as AcademiaResponse[])))
      .subscribe((academias) => {
        if (academias.length === 0) {
          this.rows.set([]);
          this.carregandoTabela.set(false);
          return;
        }
        this.carregarMetricasPorAcademia(academias);
      });
  }

  private carregarMetricasPorAcademia(academias: AcademiaResponse[]): void {
    const now = new Date();
    const mesInicio = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const mesFim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    const calls = academias.map((a) =>
      forkJoin({
        alunos: this.alunoService
          .listar(a.id)
          .pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
        personais: this.personalService
          .listar(a.id)
          .pipe(catchError(() => of([] as PersonalResponse[]))),
        checkins: this.freqService
          .listarPeriodo(mesInicio, mesFim, a.id)
          .pipe(catchError(() => of([] as FrequenciaResponse[]))),
      }),
    );

    forkJoin(calls).subscribe((resultados) => {
      const rows: AcademiaRow[] = academias.map((a, i) => {
        const { alunos, personais, checkins } = resultados[i];
        const alunosAtivos = alunos.filter((x) => x.ativo).length;
        const personaisAtivos = personais.filter((x) => x.ativo).length;
        const frequenciaMedia =
          alunosAtivos > 0 ? Math.round((checkins.length / alunosAtivos) * 10) / 10 : 0;
        return {
          id: a.id,
          name: a.name,
          responsible: a.responsible,
          active: a.active,
          alunosAtivos,
          personaisAtivos,
          frequenciaMedia,
        };
      });
      this.rows.set(rows);
      this.carregandoTabela.set(false);
    });
  }

  mediaAlunos(): string {
    const m = this.dashboard()?.averageStudentsPerAcademy ?? 0;
    return m.toFixed(1);
  }

  confirmarToggleAtivo(r: AcademiaRow): void {
    const ativar = !r.active;
    this.confirmation.confirm({
      header: ativar ? 'Ativar academia' : 'Inativar academia',
      message: `Tem certeza que deseja ${ativar ? 'ativar' : 'inativar'} a academia "${r.name}"?`,
      icon: ativar ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptLabel: ativar ? 'Ativar' : 'Inativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: ativar ? 'p-button-success' : 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => this.alterarStatus(r, ativar),
    });
  }

  private alterarStatus(r: AcademiaRow, ativar: boolean): void {
    const obs = ativar
      ? this.academiaService.ativar(r.id)
      : this.academiaService.desativar(r.id);
    obs.subscribe({
      next: () => {
        this.toast.add({
          severity: 'success',
          summary: ativar ? 'Academia ativada' : 'Academia inativada',
          life: 3000,
        });
        this.rows.update((rows) =>
          rows.map((x) => (x.id === r.id ? { ...x, active: ativar } : x)),
        );
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

  inicial(nome: string): string {
    return (nome.trim()[0] ?? 'A').toUpperCase();
  }
}
