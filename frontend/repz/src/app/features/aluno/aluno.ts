import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AlunoService,
  AuthService,
  AvaliacaoFisicaService,
  FichaTreinoService,
  FrequenciaService,
} from '@core/services';
import type { AvaliacaoFisicaResponse, TreinoResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

interface DiaSemana {
  label: string;
  altura: number;
  ativo: boolean;
}

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi, se] = t.split(':').map(Number);
  return new Date(ano, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0, se ?? 0);
}

@Component({
  selector: 'app-aluno',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    AppShell,
    ButtonModule,
    CardModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './aluno.html',
  styleUrl: './aluno.scss',
})
export class Aluno implements OnInit {
  private readonly alunoService = inject(AlunoService);
  private readonly fichaService = inject(FichaTreinoService);
  private readonly avaliacaoService = inject(AvaliacaoFisicaService);
  private readonly auth = inject(AuthService);
  protected readonly freq = inject(FrequenciaService);

  readonly carregando = signal(true);
  readonly fazendoCheckin = signal(false);
  readonly avisoCheckin = signal<string | null>(null);
  readonly avisoCheckinSeverity = signal<'success' | 'error'>('success');
  readonly nome = signal('Aluno');
  readonly planoNome = signal('—');
  readonly personalNome = signal('—');
  readonly objetivo = signal('');
  readonly totalGeral = signal(0);
  readonly totalMes = signal(0);
  readonly deltaMes = signal(0);
  readonly pesoAtual = signal<number | null>(null);
  readonly deltaPeso = signal<number | null>(null);
  readonly divisoes = signal<TreinoResponse[]>([]);
  readonly semana = signal<DiaSemana[]>([]);
  readonly ultimoCheckin = signal<Date | null>(null);

  private alunoId = 0;
  private academiaId = 0;

  readonly primeiroNome = computed(() => this.nome().trim().split(' ')[0]);

  readonly treinoDoDia = computed<TreinoResponse | null>(() => {
    const divs = this.divisoes();
    if (divs.length === 0) return null;
    const indice = this.totalGeral() % divs.length;
    return divs[indice] ?? divs[0];
  });

  readonly focoDoDia = computed(() => {
    const d = this.treinoDoDia();
    if (!d?.nome) return '';
    const partes = d.nome.split(/[—-]/);
    return partes.length > 1 ? partes[partes.length - 1].trim() : d.nome.trim();
  });

  readonly ultimoCheckinTexto = computed(() => {
    const d = this.ultimoCheckin();
    if (!d) return 'Nenhum check-in ainda';
    const data = d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const hora = d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    return `${data} às ${hora}`;
  });

  ngOnInit(): void {
    const alunoId = this.auth.sessao()?.id ?? 0;

    forkJoin({
      perfil: this.alunoService.meuPerfil().pipe(catchError(() => of(null))),
      historico: this.freq.meuHistorico().pipe(catchError(() => of([]))),
      ficha: this.fichaService
        .obterMinhaFichaAtiva()
        .pipe(catchError(() => of([] as TreinoResponse[]))),
      avaliacoes: alunoId
        ? this.avaliacaoService
            .listar(alunoId)
            .pipe(catchError(() => of([] as AvaliacaoFisicaResponse[])))
        : of([] as AvaliacaoFisicaResponse[]),
    }).subscribe(({ perfil, historico, ficha, avaliacoes }) => {
      if (perfil) {
        if (perfil.nome) this.nome.set(perfil.nome);
        this.planoNome.set(perfil.planoNome ?? '—');
        this.personalNome.set(perfil.personalNome ?? '—');
        this.objetivo.set(perfil.objetivo ?? '');
        this.alunoId = perfil.userId;
        this.academiaId = perfil.academiaId;
      }
      this.processarFrequencia(historico);
      this.divisoes.set(
        [...ficha].sort((a, b) => (a.divisao ?? '').localeCompare(b.divisao ?? '')),
      );
      this.processarAvaliacoes(avaliacoes);
      this.carregando.set(false);
    });
  }

  fazerCheckin(): void {
    if (this.fazendoCheckin() || this.freq.jaFezCheckinHoje()) return;
    if (!this.alunoId || !this.academiaId) return;
    this.fazendoCheckin.set(true);
    this.avisoCheckin.set(null);
    this.freq.registrar({ alunoId: this.alunoId, academiaId: this.academiaId }).subscribe({
      next: () => {
        this.fazendoCheckin.set(false);
        this.avisoCheckinSeverity.set('success');
        this.avisoCheckin.set('Check-in registrado com sucesso!');
        this.totalMes.update((v) => v + 1);
        this.totalGeral.update((v) => v + 1);
        this.ultimoCheckin.set(new Date());
        setTimeout(() => this.avisoCheckin.set(null), 3000);
      },
      error: (err) => {
        this.fazendoCheckin.set(false);
        this.avisoCheckinSeverity.set('error');
        this.avisoCheckin.set(err?.error?.message ?? 'Não foi possível registrar o check-in.');
        setTimeout(() => this.avisoCheckin.set(null), 4000);
      },
    });
  }

  private processarFrequencia(hist: { dataHora: string }[]): void {
    const datas = hist.map((c) => parseBR(c.dataHora));
    const hoje = new Date();

    this.totalGeral.set(datas.length);

    if (datas.length > 0) {
      const ordenado = [...datas].sort((a, b) => b.getTime() - a.getTime());
      this.ultimoCheckin.set(ordenado[0]);
    }

    const noMes = (d: Date, ref: Date) =>
      d.getMonth() === ref.getMonth() && d.getFullYear() === ref.getFullYear();
    const mesAtual = datas.filter((d) => noMes(d, hoje)).length;
    const mesAnteriorRef = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
    const mesAnterior = datas.filter((d) => noMes(d, mesAnteriorRef)).length;
    this.totalMes.set(mesAtual);
    this.deltaMes.set(mesAtual - mesAnterior);

    const offsetSegunda = (hoje.getDay() + 6) % 7;
    const segunda = new Date(hoje);
    segunda.setDate(hoje.getDate() - offsetSegunda);
    segunda.setHours(0, 0, 0, 0);

    const labels = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    const semana: DiaSemana[] = [];
    for (let i = 0; i < 7; i++) {
      const dia = new Date(segunda);
      dia.setDate(segunda.getDate() + i);
      const ativo = datas.some(
        (d) =>
          d.getDate() === dia.getDate() &&
          d.getMonth() === dia.getMonth() &&
          d.getFullYear() === dia.getFullYear(),
      );
      semana.push({ label: labels[i], altura: ativo ? 100 : 22, ativo });
    }
    this.semana.set(semana);
  }

  private processarAvaliacoes(avs: AvaliacaoFisicaResponse[]): void {
    if (!avs.length) return;
    const ordenadas = [...avs].sort(
      (a, b) => parseBR(b.dataAvaliacao).getTime() - parseBR(a.dataAvaliacao).getTime(),
    );
    const atual = ordenadas[0];
    if (atual.pesoKg != null) this.pesoAtual.set(atual.pesoKg);
    const anterior = ordenadas[1];
    if (atual.pesoKg != null && anterior?.pesoKg != null) {
      this.deltaPeso.set(Number((atual.pesoKg - anterior.pesoKg).toFixed(1)));
    }
  }
}
