import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  AuthService,
  AvaliacaoFisicaService,
  DadoGrafico,
  FrequenciaService,
  PersonalService,
  ThemeService,
} from '@core/services';
import { AppShell } from '@shared/layout';
import type { ChartData, ChartOptions, TooltipItem } from 'chart.js';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
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
  ],
  templateUrl: './avaliacao-fisica.html',
  styleUrl: './avaliacao-fisica.scss',
})
export class AvaliacaoFisica implements OnInit {
  private readonly service = inject(AvaliacaoFisicaService);
  protected readonly auth = inject(AuthService);
  protected readonly personalService = inject(PersonalService);
  protected readonly freq = inject(FrequenciaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
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

  readonly podeRegistrar = computed(() => this.auth.getUserRole() === 'PERSONAL');

  readonly nomeSidebar = computed(() =>
    this.auth.getUserRole() === 'PERSONAL'
      ? this.personalService.nomePersonal()
      : this.alunoNome(),
  );

  readonly ativoNav = computed(() =>
    this.auth.getUserRole() === 'PERSONAL' ? 'alunos' : 'evolucao',
  );

  readonly crumbs = computed<string[]>(() =>
    this.auth.getUserRole() === 'PERSONAL'
      ? ['Personal', 'Meus alunos', 'Avaliações']
      : ['Aluno', 'Evolução'],
  );

  readonly mostrarCheckin = computed(
    () => this.auth.getUserRole() === 'ALUNO' && !this.freq.jaFezCheckinHoje(),
  );

  readonly ultimaAvaliacao = computed(() => this.historico()[0] ?? null);

  private readonly primeiraAvaliacao = computed(() => {
    const h = this.historico();
    return h.length > 1 ? h[h.length - 1] : null;
  });

  readonly kpis = computed(() => {
    const atual = this.ultimaAvaliacao();
    if (!atual) return null;
    const antiga = this.primeiraAvaliacao();

    const massaMagra = (av: AvaliacaoVM | null): number | null =>
      av?.peso != null && av?.gordura != null
        ? Number((av.peso * (1 - av.gordura / 100)).toFixed(1))
        : null;
    const delta = (cur?: number | null, prev?: number | null): number | null =>
      cur != null && prev != null ? Number((cur - prev).toFixed(1)) : null;

    const mmAtual = massaMagra(atual);
    return {
      peso: atual.peso ?? null,
      pesoDelta: delta(atual.peso, antiga?.peso),
      gordura: atual.gordura ?? null,
      gorduraDelta: delta(atual.gordura, antiga?.gordura),
      massaMagra: mmAtual,
      massaMagraDelta: delta(mmAtual, massaMagra(antiga)),
      cintura: atual.cintura ?? null,
      cinturaDelta: delta(atual.cintura, antiga?.cintura),
    };
  });

  readonly form = {
    pesoKg: null as number | null,
    alturaCm: null as number | null,
    percentualGordura: null as number | null,
    cinturaCm: null as number | null,
    quadrilCm: null as number | null,
    bracoCm: null as number | null,
    coxaCm: null as number | null,
  };

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

    if (this.auth.getUserRole() === 'PERSONAL' && !this.personalService.nomePersonal()) {
      this.personalService.meuPerfil().subscribe({ error: () => {} });
    }

    if (this.auth.getUserRole() === 'ALUNO') {
      this.freq.carregarStatusHoje();
    }

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

  irParaCheckin(): void {
    this.router.navigate(['/aluno/frequencia']);
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
