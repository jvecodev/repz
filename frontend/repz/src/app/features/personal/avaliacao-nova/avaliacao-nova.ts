import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { AlunoService, AvaliacaoFisicaService, PersonalService } from '@core/services';
import type {
  AlunoDetalheResponse,
  AvaliacaoFisicaResponse,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TextareaModule } from 'primeng/textarea';

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

function hojeBR(): string {
  return new Date().toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

@Component({
  selector: 'app-personal-avaliacao-nova',
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
    TextareaModule,
  ],
  templateUrl: './avaliacao-nova.html',
  styleUrl: './avaliacao-nova.scss',
})
export class PersonalAvaliacaoNova implements OnInit {
  protected readonly personalService = inject(PersonalService);
  private readonly alunoService = inject(AlunoService);
  private readonly avaliacaoService = inject(AvaliacaoFisicaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private userId!: number;

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly alunoNome = signal('Aluno');
  readonly ultima = signal<AvaliacaoFisicaResponse | null>(null);
  readonly dataHoje = hojeBR();

  readonly fPeso = signal<number | null>(null);
  readonly fGordura = signal<number | null>(null);
  readonly fAltura = signal<number | null>(null);
  readonly fCintura = signal<number | null>(null);
  readonly fBraco = signal<number | null>(null);
  readonly fCoxa = signal<number | null>(null);
  observacoes = '';

  readonly fMassaMagra = computed(() => {
    const p = this.fPeso();
    const g = this.fGordura();
    if (p == null || g == null) return null;
    return Number((p * (1 - g / 100)).toFixed(1));
  });

  readonly massaMagraAnterior = computed(() => {
    const u = this.ultima();
    if (!u?.pesoKg || u.percentualGordura == null) return null;
    return Number((u.pesoKg * (1 - u.percentualGordura / 100)).toFixed(1));
  });

  readonly deltaPeso = computed(() => this.delta(this.fPeso(), this.ultima()?.pesoKg ?? null));
  readonly deltaGordura = computed(() =>
    this.delta(this.fGordura(), this.ultima()?.percentualGordura ?? null),
  );
  readonly deltaMassaMagra = computed(() =>
    this.delta(this.fMassaMagra(), this.massaMagraAnterior()),
  );
  readonly deltaCintura = computed(() =>
    this.delta(this.fCintura(), this.ultima()?.cinturaCm ?? null),
  );

  private delta(cur: number | null, prev: number | null): number | null {
    if (cur == null || prev == null) return null;
    return Number((cur - prev).toFixed(1));
  }

  ngOnInit(): void {
    this.userId = Number(this.route.snapshot.paramMap.get('id'));
    const nomeParam = this.route.snapshot.queryParamMap.get('nome');
    if (nomeParam) this.alunoNome.set(nomeParam);

    if (!this.personalService.nomePersonal()) {
      this.personalService.meuPerfil().subscribe({ error: () => {} });
    }

    forkJoin({
      alunos: this.alunoService
        .listar()
        .pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      avs: this.avaliacaoService
        .listar(this.userId)
        .pipe(catchError(() => of([] as AvaliacaoFisicaResponse[]))),
    }).subscribe(({ alunos, avs }) => {
      const aluno = alunos.find((a) => a.userId === this.userId);
      if (aluno) this.alunoNome.set(aluno.nome);

      if (avs.length) {
        const sorted = [...avs].sort(
          (a, b) => parseBR(b.dataAvaliacao).getTime() - parseBR(a.dataAvaliacao).getTime(),
        );
        this.ultima.set(sorted[0]);
        if (sorted[0].alturaCm != null) this.fAltura.set(sorted[0].alturaCm);
      }
      this.carregando.set(false);
    });
  }

  voltar(): void {
    this.router.navigate(['/personal/aluno', this.userId], {
      queryParams: { nome: this.alunoNome() },
    });
  }

  salvar(): void {
    if (this.salvando()) return;
    if (this.fPeso() == null || this.fAltura() == null) {
      this.avisoSeverity.set('error');
      this.aviso.set('Peso e altura são obrigatórios.');
      return;
    }

    this.aviso.set(null);
    this.salvando.set(true);

    this.avaliacaoService
      .criar({
        alunoId: this.userId,
        pesoKg: this.fPeso()!,
        alturaCm: this.fAltura()!,
        percentualGordura: this.fGordura() ?? undefined,
        cinturaCm: this.fCintura() ?? undefined,
        bracoCm: this.fBraco() ?? undefined,
        coxaCm: this.fCoxa() ?? undefined,
      })
      .subscribe({
        next: () => {
          this.router.navigate(['/personal/aluno', this.userId], {
            queryParams: { nome: this.alunoNome() },
          });
        },
        error: (err) => {
          this.salvando.set(false);
          this.avisoSeverity.set('error');
          this.aviso.set(err?.error?.message ?? 'Erro ao registrar avaliação.');
        },
      });
  }

  fmtDelta(d: number | null, unit: string): string {
    if (d == null || d === 0) return '';
    return `${d > 0 ? '+' : ''}${d} ${unit}`;
  }

  dataUltimaStr(): string {
    return this.ultima()?.dataAvaliacao?.split(' ')[0] ?? '—';
  }
}
