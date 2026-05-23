import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { AcademiaService, AlunoService, FrequenciaService, UserService } from '@core/services';
import type { AcademiaResponse, AlunoDetalheResponse, FrequenciaResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

function toInputDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function parseInputDate(s: string, endOfDay = false): Date {
  const [y, m, d] = s.split('-').map(Number);
  return endOfDay
    ? new Date(y, (m ?? 1) - 1, d ?? 1, 23, 59, 59)
    : new Date(y, (m ?? 1) - 1, d ?? 1, 0, 0, 0);
}

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
    FormsModule,
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
  private readonly academiaService = inject(AcademiaService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly checkins = signal<FrequenciaResponse[]>([]);
  readonly totalAlunos = signal(0);
  readonly academia = signal<AcademiaResponse | null>(null);

  dataInicio = '';
  dataFim = '';
  readonly periodoAplicado = signal<{ inicio: Date; fim: Date } | null>(null);

  readonly checkinsMes = computed(() => this.checkins().length);

  readonly mediaPorAluno = computed(() => {
    const t = this.totalAlunos();
    if (t === 0) return 0;
    return Math.round((this.checkinsMes() / t) * 10) / 10;
  });

  readonly periodoLabel = computed(() => {
    const p = this.periodoAplicado();
    if (!p) return '';
    const fmt = (d: Date) =>
      d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    return `${fmt(p.inicio)} – ${fmt(p.fim)}`;
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
    const p = this.periodoAplicado();
    const labels: string[] = [];
    const values: number[] = [];
    if (!p) return { labels, datasets: [] };
    const cursor = new Date(p.inicio.getFullYear(), p.inicio.getMonth(), 1);
    const limite = new Date(p.fim.getFullYear(), p.fim.getMonth(), 1);
    while (cursor.getTime() <= limite.getTime()) {
      labels.push(cursor.toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' }));
      const ano = cursor.getFullYear();
      const mes = cursor.getMonth();
      const count = this.checkins().filter((c) => {
        const d = parseBR(c.dataHora);
        return d.getMonth() === mes && d.getFullYear() === ano;
      }).length;
      values.push(count);
      cursor.setMonth(cursor.getMonth() + 1);
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
    const now = new Date();
    const inicio = new Date(now.getFullYear(), now.getMonth(), 1);
    this.dataInicio = toInputDate(inicio);
    this.dataFim = toInputDate(now);

    this.academiaService.minhaAcademia().subscribe({
      next: (a) => {
        this.academia.set(a);
        this.aplicar();
      },
      error: () => {
        this.erro.set('Não foi possível carregar a academia.');
        this.carregando.set(false);
      },
    });
  }

  aplicar(): void {
    if (!this.dataInicio || !this.dataFim) {
      this.erro.set('Selecione data de início e fim.');
      return;
    }
    const inicio = parseInputDate(this.dataInicio);
    const fim = parseInputDate(this.dataFim, true);
    if (inicio.getTime() > fim.getTime()) {
      this.erro.set('A data de início deve ser anterior à data de fim.');
      return;
    }
    this.erro.set(null);
    this.carregar(inicio, fim);
  }

  preset(meses: number): void {
    const now = new Date();
    const inicio = new Date(now.getFullYear(), now.getMonth() - (meses - 1), 1);
    this.dataInicio = toInputDate(inicio);
    this.dataFim = toInputDate(now);
    this.aplicar();
  }

  private carregar(inicio: Date, fim: Date): void {
    this.carregando.set(true);
    const academiaId = this.academia()?.id;

    forkJoin({
      alunos: this.alunoService.listar().pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      checkins: this.freqService
        .listarPeriodo(inicio, fim, academiaId)
        .pipe(catchError(() => of([] as FrequenciaResponse[]))),
    }).subscribe(({ alunos, checkins }) => {
      this.totalAlunos.set(alunos.filter((a) => a.ativo).length);
      this.checkins.set(checkins);
      this.periodoAplicado.set({ inicio, fim });
      this.carregando.set(false);
    });
  }
}
