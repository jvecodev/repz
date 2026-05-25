import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '@env/environment';

export interface PersonalResponse {
  id: number;
  userId: number;
  userName: string;
  email: string;
  academiaId: number;
  academiaNome?: string;
  especialidade?: string;
  ativo: boolean;
}

export interface PersonalUpdateRequest {
  especialidade: string;
  ativo?: boolean;
}

export interface PersonalSelfUpdateRequest {
  especialidade: string;
}

export interface AlunoResumo {
  id: number;
  nome: string;
  email: string;
}

export interface PersonalAlunosResponse {
  personalId: number;
  personalNome: string;
  especialidade?: string;
  academiaId?: number;
  academiaNome?: string;
  alunos: AlunoResumo[];
}

@Injectable({ providedIn: 'root' })
export class PersonalService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/personais`;

  private readonly _nomePersonal = signal<string>('');
  readonly nomePersonal = computed(() => this._nomePersonal());
  private nomeCarregado = false;

  private headers(academiaId?: number | null): { headers?: HttpHeaders } {
    return academiaId
      ? { headers: new HttpHeaders({ 'X-Academia-Id': String(academiaId) }) }
      : {};
  }

  listar(academiaId?: number | null): Observable<PersonalResponse[]> {
    return this.http.get<PersonalResponse[]>(this.base, this.headers(academiaId));
  }

  buscar(id: number): Observable<PersonalResponse> {
    return this.http.get<PersonalResponse>(`${this.base}/${id}`);
  }

  atualizar(
    id: number,
    req: PersonalUpdateRequest,
    academiaId?: number | null,
  ): Observable<PersonalResponse> {
    return this.http.put<PersonalResponse>(`${this.base}/${id}`, req, this.headers(academiaId));
  }

  ativar(id: number, academiaId?: number | null): Observable<PersonalResponse> {
    return this.http.patch<PersonalResponse>(
      `${this.base}/${id}/ativar`,
      {},
      this.headers(academiaId),
    );
  }

  desativar(id: number, academiaId?: number | null): Observable<PersonalResponse> {
    return this.http.patch<PersonalResponse>(
      `${this.base}/${id}/desativar`,
      {},
      this.headers(academiaId),
    );
  }

  meuPerfil(): Observable<PersonalResponse> {
    return this.http.get<PersonalResponse>(`${this.base}/me`).pipe(
      tap((p) => {
        this._nomePersonal.set(p?.userName ?? '');
        this.nomeCarregado = true;
      }),
    );
  }

  atualizarMeuPerfil(req: PersonalSelfUpdateRequest): Observable<PersonalResponse> {
    return this.http.put<PersonalResponse>(`${this.base}/me`, req).pipe(
      tap((p) => {
        if (p?.userName) this._nomePersonal.set(p.userName);
      }),
    );
  }

  meusAlunos(): Observable<PersonalAlunosResponse> {
    return this.http.get<PersonalAlunosResponse>(`${this.base}/me/alunos`);
  }
}
