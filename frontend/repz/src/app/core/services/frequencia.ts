import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
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

function hojeBR(d = new Date()): string {
  const dia = String(d.getDate()).padStart(2, '0');
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  return `${dia}/${mes}/${d.getFullYear()}`;
}

/** Amanhã em BR — cobre servidores UTC que ficam até 3h na frente */
function amanhaBS(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return hojeBR(d);
}

/** Chave usada no localStorage — formato "dd/MM/yyyy" */
const STORAGE_KEY = 'repz_checkin_hoje';

/** Lê o localStorage e retorna true se já fez check-in hoje */
function lerCheckinLocalStorage(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === hojeBR();
  } catch {
    return false;
  }
}

@Injectable({ providedIn: 'root' })
export class FrequenciaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/checkins`;

  // Inicializa já com o valor do localStorage — sem esperar a API
  private readonly _jaFezCheckinHoje = signal(lerCheckinLocalStorage());
  readonly jaFezCheckinHoje = computed(() => this._jaFezCheckinHoje());
  private statusCarregado = false;

  /** Marca check-in feito hoje no sinal e persiste no localStorage */
  private marcarHoje(): void {
    this._jaFezCheckinHoje.set(true);
    try {
      localStorage.setItem(STORAGE_KEY, hojeBR());
    } catch { /* SSR / cookies bloqueados */ }
  }

  meuHistorico(): Observable<FrequenciaResponse[]> {
    return this.http.get<FrequenciaResponse[]>(`${this.base}/me`).pipe(
      tap((lista) => {
        this.statusCarregado = true;
        // Compara com hoje E amanhã: servidor UTC pode gravar 1 dia à frente
        const hoje = hojeBR();
        const amanha = amanhaBS();
        const fezHoje = lista.some(
          (c) => c.dataHora?.startsWith(hoje) || c.dataHora?.startsWith(amanha),
        );
        if (fezHoje) {
          this.marcarHoje();
        } else {
          this._jaFezCheckinHoje.set(false);
          try { localStorage.removeItem(STORAGE_KEY); } catch { /* ignore */ }
        }
      }),
    );
  }

  registrar(req: FrequenciaCreateRequest): Observable<FrequenciaResponse> {
    return this.http.post<FrequenciaResponse>(this.base, req).pipe(
      tap(() => this.marcarHoje()),
      catchError((err) => {
        // 409 = já fez check-in hoje: atualiza UI mesmo sem novo registro
        if (err.status === 409) this.marcarHoje();
        return throwError(() => err);
      }),
    );
  }

  listarPorAluno(alunoId: number, inicio: Date, fim: Date): Observable<FrequenciaResponse[]> {
    return this.http.get<FrequenciaResponse[]>(this.base, {
      params: { aluno: alunoId, inicio: isoLocal(inicio), fim: isoLocal(fim) },
    });
  }

  listarPeriodo(inicio: Date, fim: Date, academiaId?: number): Observable<FrequenciaResponse[]> {
    const params: Record<string, string | number> = {
      inicio: isoLocal(inicio),
      fim: isoLocal(fim),
    };
    if (academiaId != null) params['academia'] = academiaId;
    return this.http.get<FrequenciaResponse[]>(this.base, { params });
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
