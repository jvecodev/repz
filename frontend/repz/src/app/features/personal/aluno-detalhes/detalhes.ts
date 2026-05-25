import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AlunoService,
  AvaliacaoFisicaService,
  FichaTreinoService,
  FrequenciaService,
  PersonalService,
} from '@core/services';
import type {
  AlunoDetalheResponse,
  AvaliacaoFisicaResponse,
  TreinoResponse,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

@Component({
  selector: 'app-personal-aluno-detalhes',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    AppShell,
    ButtonModule,
    CardModule,
    ChartModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './detalhes.html',
  styleUrl: './detalhes.scss',
})
export class PersonalAlunoDetalhes implements OnInit {
  protected readonly personalService = inject(PersonalService);
  private readonly alunoService = inject(AlunoService);
  private readonly fichaService = inject(FichaTreinoService);
  private readonly avaliacaoService = inject(AvaliacaoFisicaService);
  private readonly freqService = inject(FrequenciaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private userId!: number;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aluno = signal<AlunoDetalheResponse | null>(null);
  readonly divisoes = signal<TreinoResponse[]>([]);
  readonly avaliacoes = signal<AvaliacaoFisicaResponse[]>([]);
  readonly freqMes = signal(0);

  readonly ultimaAvaliacao = computed<AvaliacaoFisicaResponse | null>(() => {
    const avs = this.avaliacoes();
    if (!avs.length) return null;
    return [...avs].sort(
      (a, b) => parseBR(b.dataAvaliacao).getTime() - parseBR(a.dataAvaliacao).getTime(),
    )[0];
  });

  readonly pesoAtual = computed(() => this.ultimaAvaliacao()?.pesoKg ?? null);
  readonly alturaAtual = computed(() => this.ultimaAvaliacao()?.alturaCm ?? null);
  readonly massaMagra = computed(() => {
    const a = this.ultimaAvaliacao();
    if (!a?.pesoKg || a.percentualGordura == null) return null;
    return Number((a.pesoKg * (1 - a.percentualGordura / 100)).toFixed(1));
  });

  readonly chartData = computed(() => {
    const pontos = [...this.avaliacoes()]
      .filter((a) => a.pesoKg != null)
      .sort(
        (a, b) => parseBR(a.dataAvaliacao).getTime() - parseBR(b.dataAvaliacao).getTime(),
      );
    return {
      labels: pontos.map((p) => p.dataAvaliacao.split(' ')[0].slice(0, 5)),
      datasets: [
        {
          data: pontos.map((p) => p.pesoKg!),
          borderColor: '#34d399',
          backgroundColor: 'rgba(52, 211, 153, 0.14)',
          pointBackgroundColor: '#34d399',
          pointRadius: 3,
          tension: 0.35,
          fill: true,
        },
      ],
    };
  });

  readonly chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        border: { display: false },
        grid: { display: false },
        ticks: { color: '#7c8493', font: { size: 10 } },
      },
      y: {
        border: { display: false },
        grid: { color: 'rgba(255, 255, 255, 0.06)' },
        ticks: { color: '#7c8493', font: { size: 10 } },
      },
    },
  };

  focoDivisao(d: TreinoResponse): string {
    const partes = (d.nome ?? '').split(/[—-]/);
    return partes.length > 1 ? partes[partes.length - 1].trim() : (d.nome ?? '');
  }

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));

    const now = new Date();
    const mesInicio = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const mesFim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    forkJoin({
      alunos: this.alunoService.listar().pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      ficha: this.fichaService
        .obterFichaAtivaDoAluno(this.userId)
        .pipe(catchError(() => of([] as TreinoResponse[]))),
      avaliacoes: this.avaliacaoService
        .listar(this.userId)
        .pipe(catchError(() => of([] as AvaliacaoFisicaResponse[]))),
      freq: this.freqService
        .listarPorAluno(this.userId, mesInicio, mesFim)
        .pipe(catchError(() => of([]))),
    }).subscribe(({ alunos, ficha, avaliacoes, freq }) => {
      const aluno = alunos.find((a) => a.userId === this.userId) ?? null;
      if (!aluno) {
        this.carregando.set(false);
        this.erro.set('Aluno não encontrado ou sem vínculo com você.');
        return;
      }
      this.aluno.set(aluno);
      this.divisoes.set(
        [...ficha].sort((a, b) => (a.divisao ?? '').localeCompare(b.divisao ?? '')),
      );
      this.avaliacoes.set(avaliacoes);
      this.freqMes.set(freq.length);
      this.carregando.set(false);
    });
  }

  voltar(): void {
    this.router.navigate(['/personal/alunos']);
  }

  inicial(nome: string): string {
    return (nome.trim()[0] ?? 'A').toUpperCase();
  }
}
