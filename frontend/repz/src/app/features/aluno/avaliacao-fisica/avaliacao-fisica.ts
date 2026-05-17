import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService, AvaliacaoFisicaService, DadoGrafico } from '@core/services';
import { AppShell } from '@shared/layout';
import {
  AvaliacaoVM,
  Metrica,
  PontoGrafico,
  linhaPolyline,
  mapearHistorico,
  pontosGrafico,
} from './avaliacao-fisica.mapper';

@Component({
  selector: 'app-avaliacao-fisica',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShell],
  templateUrl: './avaliacao-fisica.html',
  styleUrl: './avaliacao-fisica.scss',
})
export class AvaliacaoFisica implements OnInit {
  private readonly service = inject(AvaliacaoFisicaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  private alunoId: number | null = null;

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly salvando = signal(false);
  readonly aviso = signal<string | null>(null);

  readonly historico = signal<AvaliacaoVM[]>([]);
  readonly dados = signal<DadoGrafico[]>([]);
  readonly metrica = signal<Metrica>('peso');
  readonly alunoNome = signal('Aluno');

  /** Só PERSONAL registra; demais perfis veem em modo leitura (RF31). */
  readonly podeRegistrar = computed(() => this.auth.getUserRole() === 'PERSONAL');

  readonly form = {
    pesoKg: null as number | null,
    alturaCm: null as number | null,
    percentualGordura: null as number | null,
    cinturaCm: null as number | null,
    quadrilCm: null as number | null,
    bracoCm: null as number | null,
    coxaCm: null as number | null,
  };

  /** IMC calculado em tempo real (RF28). */
  readonly imcPreview = computed(() => {
    const p = this.form.pesoKg;
    const a = this.form.alturaCm;
    if (!p || !a) return null;
    const m = a / 100;
    return Number((p / (m * m)).toFixed(1));
  });

  readonly pontos = computed(() =>
    pontosGrafico(this.dados(), this.metrica()),
  );
  readonly polyline = computed(() => linhaPolyline(this.pontos()));
  readonly tooltipPonto = signal<PontoGrafico | null>(null);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.alunoId = idParam ? Number(idParam) : this.auth.sessao()?.id ?? null;

    if (!this.alunoId) {
      this.carregando.set(false);
      this.erro.set('Não foi possível identificar o aluno.');
      return;
    }
    this.carregar();
  }

  selecionarMetrica(m: Metrica): void {
    this.metrica.set(m);
    this.tooltipPonto.set(null);
  }

  mostrarTooltip(p: PontoGrafico): void {
    this.tooltipPonto.set(p);
  }

  ocultarTooltip(): void {
    this.tooltipPonto.set(null);
  }

  formatarValorTooltip(valor: number): string {
    const m = this.metrica();
    if (m === 'peso') return `${valor} kg`;
    if (m === 'gordura') return `${valor}%`;
    return String(valor);
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
