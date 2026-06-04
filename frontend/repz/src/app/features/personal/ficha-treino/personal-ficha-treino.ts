import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { FichaTreinoService, PersonalService } from '@core/services';
import type { ExercicioCreateRequest, TreinoCreateRequest, TreinoResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

interface ExercicioDraft {
  nomeExercicio: string;
  grupoMuscular: string;
  series: number | null;
  repeticoes: string;
  cargaKg: number | null;
  descansoSegundos: number | null;
  observacao: string;
}

@Component({
  selector: 'app-personal-ficha-treino',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DragDropModule,
    AppShell,
    TranslatePipe,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './personal-ficha-treino.html',
  styleUrl: './personal-ficha-treino.scss',
})
export class PersonalFichaTreino implements OnInit {
  private readonly fichaTreinoService = inject(FichaTreinoService);
  protected readonly personalService = inject(PersonalService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(TranslateService);

  readonly LETRAS = ['A', 'B', 'C', 'D', 'E', 'F'];

  private alunoId!: number;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly divisoes = signal<TreinoResponse[]>([]);
  readonly alunoNome = signal('Aluno');
  readonly removendo = signal<number | null>(null);
  readonly criando = signal(false);
  readonly salvando = signal(false);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly exerciciosDraft = signal<ExercicioDraft[]>([]);

  readonly editandoDivisaoId = signal<number | null>(null);

  readonly tabAtiva = signal<number | 'nova' | null>(null);

  readonly divisaoAtiva = computed<TreinoResponse | null>(() => {
    const t = this.tabAtiva();
    if (t === null || t === 'nova') return null;
    return this.divisoes().find((d) => d.id === t) ?? null;
  });

  novaDivisao = '';
  novoNome = '';
  novoObjetivo = '';
  novoObservacoes = '';
  novaValidade = '';

  exNome = '';
  exGrupo = '';
  exSeries: number | null = null;
  exReps = '';
  exCarga: number | null = null;
  exDescanso: number | null = null;
  exObs = '';

  ngOnInit(): void {
    this.alunoId = Number(this.route.snapshot.paramMap.get('id'));
    const nomeParam = this.route.snapshot.queryParamMap.get('nome');
    if (nomeParam) this.alunoNome.set(nomeParam);

    if (!this.personalService.nomePersonal()) {
      this.personalService.meuPerfil().subscribe({ error: () => {} });
    }

    this.carregarFicha();
  }

  private carregarFicha(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.fichaTreinoService.obterFichaAtivaDoAluno(this.alunoId).subscribe({
      next: (treinos) => {
        const ordenados = [...treinos].sort((a, b) =>
          (a.divisao ?? '').localeCompare(b.divisao ?? ''),
        );
        this.divisoes.set(ordenados);
        if (ordenados.length > 0 && !this.route.snapshot.queryParamMap.get('nome')) {
          this.alunoNome.set(ordenados[0].alunoNome ?? 'Aluno');
        }
        this.tabAtiva.set(ordenados[0]?.id ?? 'nova');
        if (!ordenados.length) {
          this.abrirFormulario();
        }
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set(this.i18n.instant('PERSONAL.FICHA.LOAD_ERROR'));
      },
    });
  }

  selecionarTab(t: number | 'nova'): void {
    if (t === 'nova') this.abrirFormulario();
    else {
      this.cancelarFormulario();
      this.tabAtiva.set(t);
    }
  }

  voltar(): void {
    this.router.navigate(['/personal/aluno', this.alunoId], {
      queryParams: { nome: this.alunoNome() },
    });
  }

  abrirFormulario(): void {
    this.criando.set(true);
    this.editandoDivisaoId.set(null);
    this.aviso.set(null);
    this.limparFormulario();
    this.tabAtiva.set('nova');
  }

  reeditarDivisao(d: TreinoResponse): void {
    this.criando.set(true);
    this.editandoDivisaoId.set(d.id);
    this.aviso.set(null);
    this.novaDivisao = d.divisao ?? '';
    this.novoNome = d.nome ?? '';
    this.novoObjetivo = d.objetivo ?? '';
    this.novoObservacoes = d.observacoes ?? '';
    this.novaValidade = '';
    this.exerciciosDraft.set(
      (d.exercicios ?? []).map((ex) => ({
        nomeExercicio: ex.nomeExercicio,
        grupoMuscular: ex.grupoMuscular ?? '',
        series: ex.series ?? null,
        repeticoes: ex.repeticoes ?? '',
        cargaKg: ex.cargaKg ?? null,
        descansoSegundos: ex.descansoSegundos ?? null,
        observacao: ex.observacao ?? '',
      })),
    );
    this.limparCamposExercicio();
    this.tabAtiva.set('nova');
  }

  cancelarFormulario(): void {
    this.criando.set(false);
    this.editandoDivisaoId.set(null);
    this.limparFormulario();
  }

  private limparFormulario(): void {
    this.novaDivisao = '';
    this.novoNome = '';
    this.novoObjetivo = '';
    this.novoObservacoes = '';
    this.novaValidade = '';
    this.exerciciosDraft.set([]);
    this.limparCamposExercicio();
  }

  adicionarExercicioDraft(): void {
    if (!this.exNome.trim()) return;
    this.exerciciosDraft.update((lista) => [
      ...lista,
      {
        nomeExercicio: this.exNome.trim(),
        grupoMuscular: this.exGrupo.trim(),
        series: this.exSeries,
        repeticoes: this.exReps.trim(),
        cargaKg: this.exCarga,
        descansoSegundos: this.exDescanso,
        observacao: this.exObs.trim(),
      },
    ]);
    this.limparCamposExercicio();
  }

  removerExercicioDraft(idx: number): void {
    this.exerciciosDraft.update((lista) => lista.filter((_, i) => i !== idx));
  }

  reordenarExerciciosDraft(event: CdkDragDrop<ExercicioDraft[]>): void {
    this.exerciciosDraft.update((lista) => {
      const novo = [...lista];
      moveItemInArray(novo, event.previousIndex, event.currentIndex);
      return novo;
    });
  }

  private limparCamposExercicio(): void {
    this.exNome = '';
    this.exGrupo = '';
    this.exSeries = null;
    this.exReps = '';
    this.exCarga = null;
    this.exDescanso = null;
    this.exObs = '';
  }

  removerDivisao(id: number): void {
    if (this.removendo() !== null) return;
    this.removendo.set(id);
    this.fichaTreinoService.desativarDivisao(id).subscribe({
      next: () => {
        this.divisoes.update((lista) => lista.filter((d) => d.id !== id));
        this.removendo.set(null);
        this.avisoSeverity.set('success');
        this.aviso.set(this.i18n.instant('PERSONAL.FICHA.DIVISION_REMOVED'));
        setTimeout(() => this.aviso.set(null), 3000);
        if (this.tabAtiva() === id) {
          this.tabAtiva.set(this.divisoes()[0]?.id ?? 'nova');
        }
      },
      error: (err) => {
        this.removendo.set(null);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('PERSONAL.FICHA.REMOVE_ERROR'));
      },
    });
  }

  criarDivisao(): void {
    if (this.salvando() || !this.novaDivisao || !this.novoNome.trim()) return;
    this.aviso.set(null);
    this.salvando.set(true);

    const exercicios: ExercicioCreateRequest[] = this.exerciciosDraft().map((ex, i) => ({
      nomeExercicio: ex.nomeExercicio,
      grupoMuscular: ex.grupoMuscular || undefined,
      series: ex.series ?? undefined,
      repeticoes: ex.repeticoes || undefined,
      cargaKg: ex.cargaKg ?? undefined,
      descansoSegundos: ex.descansoSegundos ?? undefined,
      ordem: i + 1,
      observacao: ex.observacao || undefined,
    }));

    const req: TreinoCreateRequest = {
      alunoId: this.alunoId,
      nome: this.novoNome.trim(),
      divisao: this.novaDivisao,
      objetivo: this.novoObjetivo.trim() || undefined,
      observacoes: this.novoObservacoes.trim() || undefined,
      validadeAte: this.novaValidade || undefined,
      exercicios,
    };

    const antigaId = this.editandoDivisaoId();

    this.fichaTreinoService.criarDivisao(req).subscribe({
      next: (nova) => {
        const finalizar = () => {
          this.divisoes.update((lista) => {
            const filtrada = antigaId ? lista.filter((d) => d.id !== antigaId) : lista;
            return [...filtrada, nova].sort((a, b) =>
              (a.divisao ?? '').localeCompare(b.divisao ?? ''),
            );
          });
          this.salvando.set(false);
          this.criando.set(false);
          this.editandoDivisaoId.set(null);
          this.limparFormulario();
          this.avisoSeverity.set('success');
          this.aviso.set(
            antigaId
              ? this.i18n.instant('PERSONAL.FICHA.DIVISION_UPDATED', { div: nova.divisao })
              : this.i18n.instant('PERSONAL.FICHA.DIVISION_CREATED', { div: nova.divisao, nome: nova.nome }),
          );
          setTimeout(() => this.aviso.set(null), 4000);
          this.tabAtiva.set(nova.id);
        };

        if (antigaId) {
          this.fichaTreinoService.desativarDivisao(antigaId).subscribe({
            next: finalizar,
            error: () => finalizar(),
          });
        } else {
          finalizar();
        }
      },
      error: (err) => {
        this.salvando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('PERSONAL.FICHA.SAVE_ERROR'));
      },
    });
  }
}
