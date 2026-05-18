import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService, AvaliacaoFisicaService, DadoGrafico, ThemeService } from '@core/services';
import { AppShell } from '@shared/layout';
import type { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { AvaliacaoVM, formatarData, Metrica, mapearHistorico } from './avaliacao-fisica.mapper';

@Component({
  selector: 'app-avaliacao-fisica',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    ButtonModule,
    CardModule,
    ChartModule,
    InputNumberModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
  ],
  templateUrl: './avaliacao-fisica.html',
  styleUrl: './avaliacao-fisica.scss',
})
export class AvaliacaoFisica implements OnInit {
  private readonly service = inject(AvaliacaoFisicaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly themeService = inject(ThemeService);

  private alunoId: number | null = null;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly salvando = signal(false);
  readonly aviso = signal<string | null>(null);

  readonly historico = signal<AvaliacaoVM[]>([]);
  readonly dados = signal<DadoGrafico[]>([]);
  readonly metrica = signal<Metrica>('peso');
  readonly alunoNome = signal('Aluno');
  readonly metricas = [
    { label: 'Peso', value: 'peso' satisfies Metrica },
    { label: 'IMC', value: 'imc' satisfies Metrica },
    { label: '% Gordura', value: 'gordura' satisfies Metrica },
  ];

  /** Apenas PERSONAL registra; demais perfis acessam em modo leitura. */
  readonly podeRegistrar = computed(() => this.auth.getUserRole() === 'PERSONAL');

  /** Avaliação mais recente (histórico já está em ordem decrescente). */
  readonly ultimaAvaliacao = computed(() => this.historico()[0] ?? null);

  readonly form = {
    pesoKg: null as number | null,
    alturaCm: null as number | null,
    percentualGordura: null as number | null,
    cinturaCm: null as number | null,
    quadrilCm: null as number | null,
    bracoCm: null as number | null,
    coxaCm: null as number | null,
  };

  /** IMC calculado localmente para orientar o preenchimento antes de salvar. */
  readonly imcPreview = computed(() => {
    const p = this.form.pesoKg;
    const a = this.form.alturaCm;
    if (!p || !a) return null;
    const m = a / 100;
    return Number((p / (m * m)).toFixed(1));
  });

  readonly pontosValidos = computed(() =>
    this.dados()
      .map((d) => ({ data: d.data, valor: this.valorMetrica(d) }))
      .filter((d): d is { data: string; valor: number } => d.valor != null),
  );

  readonly graficoData = computed<ChartData<'line'>>(() => {
    const pontos = this.pontosValidos();
    const dotBorder = this.themeService.tema() === 'dark' ? '#0b0d11' : '#fefefe';
    return {
      labels: pontos.map((p) => formatarData(p.data)),
      datasets: [
        {
          data: pontos.map((p) => p.valor),
          borderColor: '#34d399',
          backgroundColor: 'rgba(52, 211, 153, 0.14)',
          pointBackgroundColor: '#34d399',
          pointBorderColor: dotBorder,
          pointBorderWidth: 2,
          pointHoverBackgroundColor: '#34d399',
          pointHoverBorderColor: dotBorder,
          pointHoverRadius: 6,
          pointRadius: 4,
          tension: 0.35,
          fill: true,
        },
      ],
    };
  });

  readonly graficoOptions = computed<ChartOptions<'line'>>(() => {
    const dark = this.themeService.tema() === 'dark';
    const tickColor = dark ? '#7c8493' : '#888888';
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 180 },
      interaction: {
        intersect: false,
        mode: 'index',
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: dark ? '#12151b' : '#fefefe',
          borderColor: dark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)',
          borderWidth: 1,
          bodyColor: dark ? '#f3f5f7' : '#2c2c2c',
          displayColors: false,
          padding: 10,
          titleColor: tickColor,
          callbacks: {
            label: (ctx: TooltipItem<'line'>) =>
              `${this.rotuloMetrica()}: ${this.formatarValorTooltip(Number(ctx.parsed.y))}`,
          },
        },
      },
      scales: {
        x: {
          border: { display: false },
          grid: { display: false },
          ticks: {
            color: tickColor,
            font: { size: 11 },
            maxRotation: 0,
          },
        },
        y: {
          border: { display: false },
          grid: { color: dark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.07)' },
          ticks: {
            color: tickColor,
            font: { size: 11 },
          },
        },
      },
    };
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.alunoId = idParam ? Number(idParam) : (this.auth.sessao()?.id ?? null);

    if (!this.alunoId) {
      this.carregando.set(false);
      this.erro.set('Não foi possível identificar o aluno.');
      return;
    }
    this.carregar();
  }

  selecionarMetrica(m: Metrica): void {
    this.metrica.set(m);
  }

  formatarValorTooltip(valor: number): string {
    const m = this.metrica();
    if (m === 'peso') return `${valor} kg`;
    if (m === 'gordura') return `${valor}%`;
    return String(valor);
  }

  private rotuloMetrica(): string {
    const m = this.metrica();
    if (m === 'peso') return 'Peso';
    if (m === 'imc') return 'IMC';
    return '% Gordura';
  }

  private valorMetrica(d: DadoGrafico): number | undefined {
    const m = this.metrica();
    if (m === 'peso') return d.peso;
    if (m === 'imc') return d.imc;
    return d.percentualGordura;
  }

  salvar(): void {
    if (this.salvando() || !this.alunoId) return;
    if (!this.form.pesoKg || !this.form.alturaCm) {
      this.aviso.set('Peso e altura são obrigatórios.');
      return;
    }
    this.aviso.set(null);
    this.salvando.set(true);

    this.service
      .criar({
        alunoId: this.alunoId,
        pesoKg: this.form.pesoKg,
        alturaCm: this.form.alturaCm,
        percentualGordura: this.form.percentualGordura ?? undefined,
        cinturaCm: this.form.cinturaCm ?? undefined,
        quadrilCm: this.form.quadrilCm ?? undefined,
        bracoCm: this.form.bracoCm ?? undefined,
        coxaCm: this.form.coxaCm ?? undefined,
      })
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.aviso.set('Avaliação registrada com sucesso.');
          this.resetForm();
          this.carregar();
        },
        error: () => {
          this.salvando.set(false);
          this.aviso.set('Não foi possível registrar a avaliação.');
        },
      });
  }

  private carregar(): void {
    if (!this.alunoId) return;
    this.carregando.set(true);
    forkJoin({
      lista: this.service.listar(this.alunoId),
      grafico: this.service.grafico(this.alunoId),
    }).subscribe({
      next: ({ lista, grafico }) => {
        this.historico.set(mapearHistorico(lista));
        this.dados.set(grafico?.dados ?? []);
        if (grafico?.alunoNome) this.alunoNome.set(grafico.alunoNome);
        else if (lista[0]?.alunoNome) this.alunoNome.set(lista[0].alunoNome);
        this.carregando.set(false);
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(
          err?.status === 401 || err?.status === 403
            ? 'Você não tem acesso a estas avaliações.'
            : 'Não foi possível carregar as avaliações.',
        );
      },
    });
  }

  private resetForm(): void {
    this.form.pesoKg = null;
    this.form.alturaCm = null;
    this.form.percentualGordura = null;
    this.form.cinturaCm = null;
    this.form.quadrilCm = null;
    this.form.bracoCm = null;
    this.form.coxaCm = null;
  }
}
