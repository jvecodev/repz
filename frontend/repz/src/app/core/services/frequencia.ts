import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '@env/environment';

export interface FrequenciaResponse {
  id: number;

  dataHora: string;
  alunoId: number;
  alunoNome: string;
  academiaId: number;
  academiaNome: string;
  registradoPorId?: number;
  registradoPorNome?: string;
}

export interface FrequenciaCreateRequest {
  alunoId: number;
  academiaId: number;
  personalId?: number;
}

export interface AlunoInativoResponse {
  alunoId: number;
  alunoNome: string;
  email: string;
  diasSemTreino: number;
  ativo: boolean;
}

function isoLocal(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

function hojeBR(): string {
  const d = new Date();
  const dia = String(d.getDate()).padStart(2, '0');
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  return `${dia}/${mes}/${d.getFullYear()}`;
}

@Injectable({ providedIn: 'root' })
export class FrequenciaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/checkins`;

  private readonly _jaFezCheckinHoje = signal(false);
  readonly jaFezCheckinHoje = computed(() => this._jaFezCheckinHoje());
  private statusCarregado = false;

  meuHistorico(): Observable<FrequenciaResponse[]> {
    return this.http.get<FrequenciaResponse[]>(`${this.base}/me`).pipe(
      tap((lista) => {
        this.statusCarregado = true;
        this._jaFezCheckinHoje.set(lista.some((c) => c.dataHora?.startsWith(hojeBR())));
      }),
    );
  }

  registrar(req: FrequenciaCreateRequest): Observable<FrequenciaResponse> {
    return this.http.post<FrequenciaResponse>(this.base, req).pipe(
      tap((novo) => {
        if (!novo.dataHora || novo.dataHora.startsWith(hojeBR())) {
          this._jaFezCheckinHoje.set(true);
        }
      }),
    );
  }

  listarPorAluno(alunoId: number, inicio: Date, fim: Date): Observable<FrequenciaResponse[]> {
    return this.http.get<FrequenciaResponse[]>(this.base, {
      params: { aluno: alunoId, inicio: isoLocal(inicio), fim: isoLocal(fim) },
    });
  }

  listarPeriodo(inicio: Date, fim: Date): Observable<FrequenciaResponse[]> {
    return this.http.get<FrequenciaResponse[]>(this.base, {
      params: { inicio: isoLocal(inicio), fim: isoLocal(fim) },
    });
  }

  alunosInativos(): Observable<AlunoInativoResponse[]> {
    return this.http.get<AlunoInativoResponse[]>(`${this.base}/alunos/inativos`);
  }

  carregarStatusHoje(): void {
    if (this.statusCarregado) return;
    this.statusCarregado = true;
    this.meuHistorico().subscribe({
      error: () => {
        this.statusCarregado = false;
      },
    });
  }
}
