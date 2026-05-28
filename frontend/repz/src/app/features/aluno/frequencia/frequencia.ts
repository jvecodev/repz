import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AlunoService, FrequenciaResponse, FrequenciaService } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

function parseBR(s: string): Date {
  const [d, t = '00:00:00'] = (s ?? '').split(' ');
  const [dia, mes, ano] = d.split('/').map(Number);
  const [h, mi] = t.split(':').map(Number);
  return new Date(ano ?? 2000, (mes ?? 1) - 1, dia ?? 1, h ?? 0, mi ?? 0);
}

function diaSemana(d: Date): string {
  return d.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '');
}

interface DiaCalendario {
  dia: number;
  data: Date;
  temCheckin: boolean;
  hoje: boolean;
  foraDoMes: boolean;
}

@Component({
  selector: 'app-frequencia',
  standalone: true,
  imports: [
    CommonModule,
    AppShell,
    ButtonModule,
    CardModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './frequencia.html',
  styleUrl: './frequencia.scss',
})
export class Frequencia implements OnInit {
  protected readonly freqService = inject(FrequenciaService);
  private readonly alunoService = inject(AlunoService);

  readonly carregando = signal(true);
  readonly fazendoCheckin = signal(false);
  readonly avisoCheckin = signal<string | null>(null);
  readonly avisoCheckinSeverity = signal<'success' | 'error'>('success');

  private alunoId = 0;
  private academiaId = 0;
  readonly erro = signal<string | null>(null);
  readonly checkins = signal<FrequenciaResponse[]>([]);
  readonly nomeAluno = signal('Aluno');

  readonly mesAtual = signal(new Date());

  readonly checkinsDatas = computed(() => {
    const datas = new Set<string>();
    for (const c of this.checkins()) {
      const d = parseBR(c.dataHora);
      datas.add(`${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`);
    }
    return datas;
  });

  readonly totalCheckins = computed(() => this.checkins().length);

  readonly sequenciaAtual = computed(() => {
    const hoje = new Date();
    const datas = this.checkinsDatas();
    let streak = 0;
    const cursor = new Date(hoje);
    for (let i = 0; i < 365; i++) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`;
      if (datas.has(key)) {
        streak++;
        cursor.setDate(cursor.getDate() - 1);
      } else if (i === 0) {
        cursor.setDate(cursor.getDate() - 1);
      } else {
        break;
      }
    }
    return streak;
  });

  readonly checkinsMes = computed(() => {
    const m = this.mesAtual();
    return this.checkins().filter((c) => {
      const d = parseBR(c.dataHora);
      return d.getMonth() === m.getMonth() && d.getFullYear() === m.getFullYear();
    }).length;
  });

  readonly ultimoCheckin = computed(() => {
    if (this.checkins().length === 0) return null;
    const sorted = [...this.checkins()].sort(
      (a, b) => parseBR(b.dataHora).getTime() - parseBR(a.dataHora).getTime(),
    );
    return parseBR(sorted[0]!.dataHora);
  });

  readonly diasDesdeUltimo = computed(() => {
    const u = this.ultimoCheckin();
    if (!u) return null;
    return Math.floor((Date.now() - u.getTime()) / 86400000);
  });

  readonly diasSemana = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

  readonly calendarioDias = computed((): DiaCalendario[] => {
    const m = this.mesAtual();
    const hoje = new Date();
    const datas = this.checkinsDatas();

    const primeiroDia = new Date(m.getFullYear(), m.getMonth(), 1);
    const ultimoDia = new Date(m.getFullYear(), m.getMonth() + 1, 0);
    const dias: DiaCalendario[] = [];

    // Preenche dias antes do primeiro dia do mês
    for (let i = 0; i < primeiroDia.getDay(); i++) {
      const d = new Date(primeiroDia);
      d.setDate(d.getDate() - (primeiroDia.getDay() - i));
      dias.push({ dia: d.getDate(), data: d, temCheckin: false, hoje: false, foraDoMes: true });
    }

    // Dias do mês
    for (let d = 1; d <= ultimoDia.getDate(); d++) {
      const data = new Date(m.getFullYear(), m.getMonth(), d);
      const key = `${data.getFullYear()}-${data.getMonth()}-${data.getDate()}`;
      const ehHoje =
        data.getDate() === hoje.getDate() &&
        data.getMonth() === hoje.getMonth() &&
        data.getFullYear() === hoje.getFullYear();
      dias.push({ dia: d, data, temCheckin: datas.has(key), hoje: ehHoje, foraDoMes: false });
    }

    // Preenche dias depois do último dia até completar semanas
    const resto = 7 - (dias.length % 7);
    if (resto < 7) {
      for (let i = 1; i <= resto; i++) {
        const d = new Date(ultimoDia);
        d.setDate(d.getDate() + i);
        dias.push({ dia: d.getDate(), data: d, temCheckin: false, hoje: false, foraDoMes: true });
      }
    }

    return dias;
  });

  readonly mesLabel = computed(() => {
    const m = this.mesAtual();
    return m.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  });

  readonly ultimosCheckins = computed(() =>
    [...this.checkins()]
      .sort((a, b) => parseBR(b.dataHora).getTime() - parseBR(a.dataHora).getTime())
      .slice(0, 10),
  );

  ngOnInit(): void {
    this.alunoService.meuPerfil().subscribe({
      next: (a) => {
        this.nomeAluno.set(a.nome);
        this.alunoId = a.userId;
        this.academiaId = a.academiaId;
      },
      error: () => {},
    });
    this.freqService.meuHistorico().subscribe({
      next: (lista) => {
        this.checkins.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar seu histórico de frequência.');
      },
    });
  }

  fazerCheckin(): void {
    if (this.fazendoCheckin() || this.freqService.jaFezCheckinHoje()) return;
    if (!this.alunoId || !this.academiaId) return;
    this.fazendoCheckin.set(true);
    this.avisoCheckin.set(null);
    this.freqService.registrar({ alunoId: this.alunoId, academiaId: this.academiaId }).subscribe({
      next: (novo) => {
        this.fazendoCheckin.set(false);
        this.avisoCheckinSeverity.set('success');
        this.avisoCheckin.set('Check-in registrado com sucesso!');
        this.checkins.update((lista) => [novo, ...lista]);
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

  mesAnterior(): void {
    const m = this.mesAtual();
    this.mesAtual.set(new Date(m.getFullYear(), m.getMonth() - 1, 1));
  }

  proximoMes(): void {
    const m = this.mesAtual();
    this.mesAtual.set(new Date(m.getFullYear(), m.getMonth() + 1, 1));
  }

  podeProximoMes(): boolean {
    const m = this.mesAtual();
    const hoje = new Date();
    return (
      m.getFullYear() < hoje.getFullYear() ||
      (m.getFullYear() === hoje.getFullYear() && m.getMonth() < hoje.getMonth())
    );
  }

  formatarData(d: Date): string {
    return d.toLocaleDateString('pt-BR', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
    });
  }

  formatarHora(dataHora: string): string {
    const d = parseBR(dataHora);
    return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }

  diaSemanaCheckin(dataHora: string): string {
    return diaSemana(parseBR(dataHora));
  }

  parseBR(s: string): Date {
    return parseBR(s);
  }
}
