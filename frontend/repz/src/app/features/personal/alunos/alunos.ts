import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { catchError, forkJoin, of } from 'rxjs';
import { AlunoService, FrequenciaService, PersonalService } from '@core/services';
import type { AlunoDetalheResponse, FrequenciaResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

interface AlunoRow {

  userId: number;
  nome: string;
  email: string;
  planoNome: string;
  ativo: boolean;
  freqMes: number;
  ultimoCheckin: string | null;
}

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

@Component({
  selector: 'app-personal-alunos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './alunos.html',
  styleUrl: './alunos.scss',
})
export class PersonalAlunos implements OnInit {
  protected readonly personalService = inject(PersonalService);
  private readonly alunoService = inject(AlunoService);
  private readonly freqService = inject(FrequenciaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly foco = toSignal(
    this.route.queryParamMap.pipe(map((p) => p.get('foco') ?? 'alunos')),
    { initialValue: 'alunos' },
  );
  readonly ativoSidebar = computed(() => {
    const f = this.foco();
    if (f === 'ficha') return 'fichas';
    if (f === 'avaliacao') return 'avaliacoes';
    return 'alunos';
  });
  readonly tituloPagina = computed(() => {
    const f = this.foco();
    if (f === 'ficha') return 'Fichas dos alunos';
    if (f === 'avaliacao') return 'Avaliações dos alunos';
    return 'Meus alunos';
  });

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly rows = signal<AlunoRow[]>([]);
  readonly busca = signal('');

  readonly rowsFiltradas = computed(() => {
    const q = this.busca().trim().toLowerCase();
    if (!q) return this.rows();
    return this.rows().filter(
      (a) =>
        a.nome.toLowerCase().includes(q) ||
        a.email.toLowerCase().includes(q) ||
        (a.planoNome ?? '').toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const now = new Date();
    const mesInicio = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const mesFim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);

    this.alunoService.listar().subscribe({
      next: (alunos) => {
        if (alunos.length === 0) {
          this.rows.set([]);
          this.carregando.set(false);
          return;
        }

        const freqCalls = alunos.map((a) =>
          this.freqService
            .listarPorAluno(a.userId, mesInicio, mesFim)
            .pipe(catchError(() => of([] as FrequenciaResponse[]))),
        );

        forkJoin(freqCalls).subscribe((freqs) => {
          this.rows.set(this.montarRows(alunos, freqs));
          this.carregando.set(false);
        });
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os alunos.');
      },
    });
  }

  private montarRows(
    alunos: AlunoDetalheResponse[],
    freqs: FrequenciaResponse[][],
  ): AlunoRow[] {
    return alunos.map((a, i) => {
      const lista = freqs[i] ?? [];
      const ordenado = [...lista].sort(
        (x, y) => parseBR(y.dataHora).getTime() - parseBR(x.dataHora).getTime(),
      );
      const ultimo = ordenado[0]?.dataHora?.split(' ')[0] ?? null;
      return {
        userId: a.userId,
        nome: a.nome,
        email: a.email,
        planoNome: a.planoNome ?? '—',
        ativo: a.ativo,
        freqMes: lista.length,
        ultimoCheckin: ultimo,
      };
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
}
