import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AlunoService,
  AvaliacaoFisicaService,
  FrequenciaService,
  PersonalService,
  SolicitacaoFichaService,
} from '@core/services';
import type {
  AlunoDetalheResponse,
  AlunoInativoResponse,
  AvaliacaoFisicaResponse,
  FrequenciaResponse,
  PersonalResponse,
  SolicitacaoFichaResponse,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

export type StatusTreino = 'em_dia' | 'atencao' | 'ausente' | 'sem_checkin';

interface AlunoRow {
  userId: number;
  nome: string;
  email: string;
  planoNome: string;
  ativo: boolean;
  freqMes: number;
  ultimoCheckin: string | null;
  diasSemTreino: number | null;
  statusTreino: StatusTreino;
}

function diasEntre(a: Date, b: Date): number {
  return Math.floor((b.getTime() - a.getTime()) / 86400000);
}

function classificarStatus(dias: number | null): StatusTreino {
  if (dias == null) return 'sem_checkin';
  if (dias <= 7) return 'em_dia';
  if (dias <= 14) return 'atencao';
  return 'ausente';
}

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

@Component({
  selector: 'app-personal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    AppShell,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    SkeletonModule,
    TableModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './personal.html',
  styleUrl: './personal.scss',
})
export class Personal implements OnInit {
  private readonly solicitacaoService = inject(SolicitacaoFichaService);
  private readonly personalService = inject(PersonalService);
  private readonly alunoService = inject(AlunoService);
  private readonly freqService = inject(FrequenciaService);
  private readonly avaliacaoService = inject(AvaliacaoFisicaService);
  private readonly router = inject(Router);

  readonly perfil = signal<PersonalResponse | null>(null);
  readonly nomePersonal = () => this.perfil()?.userName ?? 'Personal';

  readonly carregando = signal(true);
  readonly rows = signal<AlunoRow[]>([]);
  readonly avaliacoesMes = signal(0);
  readonly busca = signal('');

  readonly ausentes7d = computed(
    () => this.rows().filter((r) => r.statusTreino === 'atencao' || r.statusTreino === 'ausente').length,
  );
  readonly ausentes14d = computed(
    () => this.rows().filter((r) => r.statusTreino === 'ausente').length,
  );

  readonly totalAlunos = computed(() => this.rows().length);
  readonly ativos = computed(() => this.rows().filter((r) => r.ativo).length);

  readonly rowsFiltradas = computed(() => {
    const q = this.busca().trim().toLowerCase();
    if (!q) return this.rows();
    return this.rows().filter(
      (a) => a.nome.toLowerCase().includes(q) || a.email.toLowerCase().includes(q),
    );
  });

  readonly solicitacoes = signal<SolicitacaoFichaResponse[]>([]);
  readonly carregandoSol = signal(true);
  readonly totalPendentes = computed(() => this.solicitacoes().length);
  readonly respondendoId = signal<number | null>(null);
  readonly respostaTexto = signal('');
  readonly salvandoResposta = signal(false);
  readonly avisoResposta = signal<string | null>(null);

  ngOnInit(): void {
    this.personalService.meuPerfil().subscribe({
      next: (p) => this.perfil.set(p),
      error: () => {},
    });

    this.carregarDashboard();
    this.carregarSolicitacoes();
  }

  private carregarDashboard(): void {
    const now = new Date();
    const mesInicio = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const mesFim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    forkJoin({
      alunos: this.alunoService.listar().pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      inativos: this.freqService
        .alunosInativos()
        .pipe(catchError(() => of([] as AlunoInativoResponse[]))),
    }).subscribe(({ alunos, inativos }) => {
      if (alunos.length === 0) {
        this.rows.set([]);
        this.avaliacoesMes.set(0);
        this.carregando.set(false);
        return;
      }

      const freqCalls = alunos.map((a) =>
        this.freqService
          .listarPorAluno(a.userId, mesInicio, mesFim)
          .pipe(catchError(() => of([] as FrequenciaResponse[]))),
      );
      const avCalls = alunos.map((a) =>
        this.avaliacaoService
          .listar(a.userId)
          .pipe(catchError(() => of([] as AvaliacaoFisicaResponse[]))),
      );

      forkJoin({ freqs: forkJoin(freqCalls), avs: forkJoin(avCalls) }).subscribe(
        ({ freqs, avs }) => {
          this.rows.set(this.montarRows(alunos, freqs, inativos));
          this.avaliacoesMes.set(this.contarAvaliacoesNoMes(avs, mesInicio, mesFim));
          this.carregando.set(false);
        },
      );
    });
  }

  private montarRows(
    alunos: AlunoDetalheResponse[],
    freqs: FrequenciaResponse[][],
    inativos: AlunoInativoResponse[],
  ): AlunoRow[] {
    const inativoPorId = new Map<number, AlunoInativoResponse>();
    for (const i of inativos) inativoPorId.set(i.alunoId, i);
    const hoje = new Date();

    return alunos.map((a, i) => {
      const lista = freqs[i] ?? [];
      const ordenado = [...lista].sort(
        (x, y) => parseBR(y.dataHora).getTime() - parseBR(x.dataHora).getTime(),
      );
      const ultimoData = ordenado[0]?.dataHora ?? null;
      const ultimo = ultimoData?.split(' ')[0] ?? null;

      let diasSemTreino: number | null;
      const inativoInfo = inativoPorId.get(a.userId);
      if (inativoInfo) {
        diasSemTreino = inativoInfo.diasSemTreino;
      } else if (ultimoData) {
        diasSemTreino = diasEntre(parseBR(ultimoData), hoje);
      } else {
        diasSemTreino = null;
      }

      return {
        userId: a.userId,
        nome: a.nome,
        email: a.email,
        planoNome: a.planoNome ?? '—',
        ativo: a.ativo,
        freqMes: lista.length,
        ultimoCheckin: ultimo,
        diasSemTreino,
        statusTreino: classificarStatus(diasSemTreino),
      };
    });
  }

  statusLabel(s: StatusTreino): string {
    switch (s) {
      case 'em_dia':
        return 'Em dia';
      case 'atencao':
        return 'Atenção';
      case 'ausente':
        return 'Ausente';
      case 'sem_checkin':
        return 'Sem check-in';
    }
  }

  statusClass(s: StatusTreino): string {
    switch (s) {
      case 'em_dia':
        return 'repz-tag-success';
      case 'atencao':
        return 'repz-tag-warn';
      case 'ausente':
        return 'repz-tag-danger';
      case 'sem_checkin':
        return 'repz-tag-muted';
    }
  }

  private contarAvaliacoesNoMes(
    avs: AvaliacaoFisicaResponse[][],
    inicio: Date,
    fim: Date,
  ): number {
    const inicioT = inicio.getTime();
    const fimT = fim.getTime();
    let total = 0;
    for (const lista of avs) {
      for (const a of lista) {
        const t = parseBR(a.dataAvaliacao).getTime();
        if (t >= inicioT && t <= fimT) total++;
      }
    }
    return total;
  }

  private carregarSolicitacoes(): void {
    this.solicitacaoService.listarParaPersonal('PENDENTE').subscribe({
      next: (lista) => {
        this.solicitacoes.set(lista);
        this.carregandoSol.set(false);
      },
      error: () => this.carregandoSol.set(false),
    });
  }

  inicial(nome: string): string {
    return (nome.trim()[0] ?? 'A').toUpperCase();
  }

  freqPct(freqMes: number): number {
    return Math.min(100, Math.round((freqMes / 20) * 100));
  }

  verDetalhes(r: AlunoRow): void {
    this.router.navigate(['/personal/aluno', r.userId], {
      queryParams: { nome: r.nome },
    });
  }

  verFicha(r: AlunoRow): void {
    this.router.navigate(['/personal/aluno', r.userId, 'ficha-treino'], {
      queryParams: { nome: r.nome },
    });
  }

  abrirResposta(id: number): void {
    this.respondendoId.set(id);
    this.respostaTexto.set('');
    this.avisoResposta.set(null);
  }

  fecharResposta(): void {
    this.respondendoId.set(null);
  }

  responder(id: number, status: 'APROVADA' | 'REJEITADA'): void {
    if (this.salvandoResposta()) return;
    this.avisoResposta.set(null);
    this.salvandoResposta.set(true);

    this.solicitacaoService
      .responder(id, { status, resposta: this.respostaTexto().trim() || undefined })
      .subscribe({
        next: () => {
          this.salvandoResposta.set(false);
          this.respondendoId.set(null);
          this.solicitacoes.update((lista) => lista.filter((s) => s.id !== id));
        },
        error: (err) => {
          this.salvandoResposta.set(false);
          this.avisoResposta.set(err?.error?.message ?? 'Erro ao responder.');
        },
      });
  }
}
