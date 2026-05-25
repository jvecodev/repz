import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AlunoDetalheResponse {
  id: number;
  userId: number;
  nome: string;
  email: string;
  telefone?: string;
  fotoUrl?: string;
  academiaId: number;
  academiaNome?: string;
  personalId?: number;
  personalNome?: string;
  planoId?: number;
  planoNome?: string;
  objetivo?: string;
  ativo: boolean;
}

export interface AlunoUpdateRequest {
  planoId?: number;
  personalId?: number;
  objetivo?: string;
}

export interface AlunoMeUpdateRequest {
  nome?: string;
  telefone?: string;
  fotoUrl?: string;
  senha?: string;
}

@Injectable({ providedIn: 'root' })
export class AlunoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/alunos`;

  private headers(academiaId?: number | null): { headers?: HttpHeaders } {
    return academiaId
      ? { headers: new HttpHeaders({ 'X-Academia-Id': String(academiaId) }) }
      : {};
  }

  listar(academiaId?: number | null): Observable<AlunoDetalheResponse[]> {
    return this.http.get<AlunoDetalheResponse[]>(this.base, this.headers(academiaId));
  }

  buscar(id: number): Observable<AlunoDetalheResponse> {
    return this.http.get<AlunoDetalheResponse>(`${this.base}/${id}`);
  }

  atualizar(
    id: number,
    req: AlunoUpdateRequest,
    academiaId?: number | null,
  ): Observable<AlunoDetalheResponse> {
    return this.http.put<AlunoDetalheResponse>(`${this.base}/${id}`, req, this.headers(academiaId));
  }

  inativar(id: number, academiaId?: number | null): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/inativar`, {}, this.headers(academiaId));
  }

  meuPerfil(): Observable<AlunoDetalheResponse> {
    return this.http.get<AlunoDetalheResponse>(`${this.base}/me`);
  }

  atualizarMeuPerfil(req: AlunoMeUpdateRequest): Observable<AlunoDetalheResponse> {
    return this.http.put<AlunoDetalheResponse>(`${this.base}/me`, req);
  }
}
