import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '@env/environment';

export type UserRole = 'ADMIN' | 'GERENTE' | 'PERSONAL' | 'ALUNO';

export interface UserCreateRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
  academiaId: number;
  planoId?: number;
}

export interface UserPutRequest {
  name: string;
  email: string;
  role?: UserRole;
  active?: boolean;
}

export interface UserSelfUpdateRequest {
  name: string;
  email: string;
  senha?: string;
}

export interface UserGetResponse {
  id: number;
  name: string;
  email: string;
  lastLogin?: string;
  role: UserRole;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/users`;

  private readonly _nomeUsuario = signal<string>('');
  readonly nomeUsuario = computed(() => this._nomeUsuario());
  private nomeCarregado = false;

  criar(req: UserCreateRequest): Observable<void> {
    return this.http.post<void>(this.base, req);
  }

  listar(): Observable<UserGetResponse[]> {
    return this.http.get<UserGetResponse[]>(this.base);
  }

  atualizar(id: number, req: UserPutRequest): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}`, req);
  }

  ativar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/ativar`, {});
  }

  desativar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/desativar`, {});
  }

  meuPerfil(): Observable<UserGetResponse> {
    return this.http.get<UserGetResponse>(`${this.base}/me`).pipe(
      tap((u) => {
        this._nomeUsuario.set(u.name ?? '');
        this.nomeCarregado = true;
      }),
    );
  }

  atualizarMeuPerfil(req: UserSelfUpdateRequest): Observable<UserGetResponse> {
    return this.http.put<UserGetResponse>(`${this.base}/me`, req).pipe(
      tap((u) => this._nomeUsuario.set(u?.name ?? req.name)),
    );
  }

  carregarNomeLogado(): void {
    if (this.nomeCarregado) return;
    this.nomeCarregado = true;
    this.meuPerfil().subscribe({
      error: () => {
        this.nomeCarregado = false;
      },
    });
  }
}
