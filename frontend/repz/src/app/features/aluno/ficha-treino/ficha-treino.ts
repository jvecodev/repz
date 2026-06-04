import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AuthService,
  FichaTreinoService,
  FrequenciaService,
  SolicitacaoFichaService,
} from '@core/services';
import type { SolicitacaoFichaResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import {
  DivisaoVM,
  FichaVM,
  HistoricoVM,
  mapearFichaAtiva,
  mapearHistorico,
} from './ficha-treino.mapper';

@Component({
  selector: 'app-ficha-treino',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    TranslatePipe,
    ButtonModule,
    CardModule,
    DialogModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './ficha-treino.html',
  styleUrl: './ficha-treino.scss',
})
export class FichaTreino implements OnInit {
  private readonly service = inject(FichaTreinoService);
  private readonly solicitacaoService = inject(SolicitacaoFichaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly freq = inject(FrequenciaService);
  private readonly i18n = inject(TranslateService);

  readonly ficha = signal<FichaVM | null>(null);
  readonly historico = signal<HistoricoVM[]>([]);
  readonly tabAtiva = signal<string>('A');
  readonly historicoAberto = signal(false);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly dialogAberto = signal(false);
  readonly mensagemSolicitacao = signal('');
  readonly enviando = signal(false);
  readonly solicitacaoPendente = signal<SolicitacaoFichaResponse | null>(null);
  readonly avisoSolicitacao = signal<string | null>(null);

  readonly aluno = computed(() => this.ficha()?.alunoNome ?? 'Aluno');
  readonly alunoObjetivo = computed(
    () => this.ficha()?.objetivo || this.i18n.instant('ALUNO.FICHA.DEFAULT_GOAL'),
  );
  readonly letras = computed(() => this.ficha()?.letras ?? []);

  readonly treinoAtual = computed<DivisaoVM | null>(
    () => this.ficha()?.treinos[this.tabAtiva()] ?? null,
  );

  readonly totalExercicios = computed(() => {
    const f = this.ficha();
    return f ? Object.values(f.treinos).reduce((acc, d) => acc + d.exercicios.length, 0) : 0;
  });

  readonly podesolicitar = computed(() => this.auth.getUserRole() === 'ALUNO');

  ngOnInit(): void {
    this.freq.carregarStatusHoje();

    const idParam = this.route.snapshot.paramMap.get('id');
    const alunoId = idParam ? Number(idParam) : null;

    const ativa$ = alunoId
      ? this.service.obterFichaAtivaDoAluno(alunoId)
      : this.service.obterMinhaFichaAtiva();
    const historico$ = alunoId
      ? this.service.obterHistoricoDoAluno(alunoId)
      : this.service.obterMeuHistorico();

    const pendente$ = !alunoId
      ? this.solicitacaoService.pendente().pipe(catchError(() => of(null)))
      : of(null);

    forkJoin({ ativa: ativa$, historico: historico$, pendente: pendente$ }).subscribe({
      next: ({ ativa, historico, pendente }) => {
        const ficha = mapearFichaAtiva(ativa);
        this.ficha.set(ficha);
        if (ficha) this.tabAtiva.set(ficha.letras[0]);
        this.historico.set(mapearHistorico(historico));
        this.solicitacaoPendente.set(pendente);
        this.carregando.set(false);
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(
          err?.status === 401 || err?.status === 403
            ? this.i18n.instant('ALUNO.FICHA.NO_ACCESS')
            : this.i18n.instant('ALUNO.FICHA.LOAD_ERROR'),
        );
      },
    });
  }

  selecionarTab(letra: string): void {
    this.tabAtiva.set(letra);
  }

  toggleHistorico(): void {
    this.historicoAberto.update((v) => !v);
  }

  abrirDialogSolicitacao(): void {
    this.mensagemSolicitacao.set('');
    this.avisoSolicitacao.set(null);
    this.dialogAberto.set(true);
  }

  fecharDialog(): void {
    this.dialogAberto.set(false);
  }

  enviarSolicitacao(): void {
    if (this.enviando()) return;
    this.avisoSolicitacao.set(null);
    this.enviando.set(true);

    this.solicitacaoService
      .criar({
        personalId: this.ficha()?.personalId ?? undefined,
        mensagem: this.mensagemSolicitacao().trim() || undefined,
      })
      .subscribe({
        next: (res) => {
          this.enviando.set(false);
          this.solicitacaoPendente.set(res);
          this.dialogAberto.set(false);
        },
        error: (err) => {
          this.enviando.set(false);
          const msg = err?.error?.message ?? this.i18n.instant('ALUNO.FICHA.REQUEST_ERROR');
          this.avisoSolicitacao.set(msg);
        },
      });
  }

  irParaCheckin(): void {
    this.router.navigate(['/aluno/frequencia']);
  }
}
