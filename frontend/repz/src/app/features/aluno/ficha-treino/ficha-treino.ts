import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FichaTreinoService } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
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
    AppShell,
    ButtonModule,
    CardModule,
    ProgressSpinnerModule,
    TableModule,
    TabsModule,
    TagModule,
  ],
  templateUrl: './ficha-treino.html',
  styleUrl: './ficha-treino.scss',
})
export class FichaTreino implements OnInit {
  private readonly service = inject(FichaTreinoService);
  private readonly route = inject(ActivatedRoute);

  readonly ficha = signal<FichaVM | null>(null);
  readonly historico = signal<HistoricoVM[]>([]);
  readonly tabAtiva = signal<string>('A');
  readonly historicoAberto = signal(false);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly aluno = computed(() => this.ficha()?.alunoNome ?? 'Aluno');
  readonly alunoObjetivo = computed(() => this.ficha()?.objetivo || 'Treino');
  readonly letras = computed(() => this.ficha()?.letras ?? []);

  readonly treinoAtual = computed<DivisaoVM | null>(
    () => this.ficha()?.treinos[this.tabAtiva()] ?? null,
  );

  readonly totalExercicios = computed(() => {
    const f = this.ficha();
    return f ? Object.values(f.treinos).reduce((acc, d) => acc + d.exercicios.length, 0) : 0;
  });

  ngOnInit(): void {
    // Na rota do personal, o aluno é definido pelo parâmetro da URL.
    const idParam = this.route.snapshot.paramMap.get('id');
    const alunoId = idParam ? Number(idParam) : null;

    const ativa$ = alunoId
      ? this.service.obterFichaAtivaDoAluno(alunoId)
      : this.service.obterMinhaFichaAtiva();
    const historico$ = alunoId
      ? this.service.obterHistoricoDoAluno(alunoId)
      : this.service.obterMeuHistorico();

    forkJoin({ ativa: ativa$, historico: historico$ }).subscribe({
      next: ({ ativa, historico }) => {
        const ficha = mapearFichaAtiva(ativa);
        this.ficha.set(ficha);
        if (ficha) this.tabAtiva.set(ficha.letras[0]);
        this.historico.set(mapearHistorico(historico));
        this.carregando.set(false);
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(
          err?.status === 401 || err?.status === 403
            ? 'Você não tem acesso a esta ficha (faça login como aluno).'
            : 'Não foi possível carregar sua ficha de treino.',
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
}
