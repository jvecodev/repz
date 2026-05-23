import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, of } from 'rxjs';
import { AlunoService, FrequenciaService, UserService } from '@core/services';
import type { AlunoDetalheResponse, FrequenciaResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

@Component({
  selector: 'app-academia-relatorios',
  standalone: true,
  imports: [
    CommonModule,
    AppShell,
    ButtonModule,
    CardModule,
    ChartModule,
    MessageModule,
    ProgressSpinnerModule,
  ],
  templateUrl: './relatorios.html',
  styleUrl: './relatorios.scss',
})
export class AcademiaRelatorios implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly freqService = inject(FrequenciaService);
  private readonly alunoService = inject(AlunoService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly checkins = signal<FrequenciaResponse[]>([]);
  readonly totalAlunos = signal(0);

  readonly checkinsMes = computed(() => this.checkins().length);

  readonly mediaPorAluno = computed(() => {
    const t = this.totalAlunos();
    if (t === 0) return 0;
    return Math.round((this.checkinsMes() / t) * 10) / 10;
  });

  readonly horaPico = computed(() => {
    const buckets = this.ocupacaoBuckets();
    let maxH = 0;
    let max = -1;
    buckets.forEach((c, h) => {
      if (c > max) {
        max = c;
        maxH = h;
      }
    });
    return { hora: maxH, count: max < 0 ? 0 : max };
  });

  private readonly ocupacaoBuckets = computed(() => {
    const buckets = new Array(24).fill(0);
    for (const c of this.checkins()) {
      const d = parseBR(c.dataHora);
      buckets[d.getHours()] = (buckets[d.getHours()] ?? 0) + 1;
    }
    return buckets;
  });

  readonly ocupacaoData = computed(() => {
    const buckets = this.ocupacaoBuckets();
    const horas: number[] = [];
    for (let h = 6; h <= 22; h++) horas.push(h);
    return {
      labels: horas.map((h) => `${String(h).padStart(2, '0')}h`),
      datasets: [
        {
          data: horas.map((h) => buckets[h] ?? 0),
          backgroundColor: 'rgba(52, 211, 153, 0.85)',
          borderRadius: 6,
          borderSkipped: false,
        },
      ],
    };
  });

  readonly ocupacaoOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        border: { display: false },
        grid: { display: false },
        ticks: { color: '#7c8493', font: { size: 11 } },
      },
      y: {
        border: { display: false },
        grid: { color: 'rgba(255, 255, 255, 0.06)' },
        ticks: { color: '#7c8493', font: { size: 11 }, stepSize: 1 },
      },
    },
  };

  readonly mesData = computed(() => {
    const now = new Date();
    const labels: string[] = [];
    const values: number[] = [];
    for (let i = 5; i >= 0; i--) {
      const ref = new Date(now.getFullYear(), now.getMonth() - i, 1);
      labels.push(ref.toLocaleDateString('pt-BR', { month: 'short' }));
      const count = this.checkins().filter((c) => {
        const d = parseBR(c.dataHora);
        return d.getMonth() === ref.getMonth() && d.getFullYear() === ref.getFullYear();
      }).length;
      values.push(count);
    }
    return {
      labels,
      datasets: [
        {
          data: values,
          borderColor: '#34d399',
          backgroundColor: 'rgba(52, 211, 153, 0.14)',
          pointBackgroundColor: '#34d399',
          tension: 0.35,
          fill: true,
        },
      ],
    };
  });

  readonly mesOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        border: { display: false },
        grid: { display: false },
        ticks: { color: '#7c8493', font: { size: 11 } },
      },
      y: {
        border: { display: false },
        grid: { color: 'rgba(255, 255, 255, 0.06)' },
        ticks: { color: '#7c8493', font: { size: 11 } },
      },
    },
  };

  ngOnInit(): void {
    this.userService.carregarNomeLogado();
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const now = new Date();
    const inicio = new Date(now.getFullYear(), now.getMonth() - 6, 1, 0, 0, 0);
    const fim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    forkJoin({
      alunos: this.alunoService.listar().pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      checkins: this.freqService
        .listarPeriodo(inicio, fim)
        .pipe(catchError(() => of([] as FrequenciaResponse[]))),
    }).subscribe(({ alunos, checkins }) => {
      this.totalAlunos.set(alunos.filter((a) => a.ativo).length);
      this.checkins.set(checkins);
      this.carregando.set(false);
    });
  }
}
