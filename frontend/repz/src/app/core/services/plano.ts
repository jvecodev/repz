import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface PlanoResponse {
  id: number;
  nome: string;
  duracaoDias: number;
  valor: number;
  ativo: boolean;
}

export interface PlanoRequest {
  nome: string;
  duracaoDias: number;
  valor: number;
}

export type PlanoPostRequest = PlanoRequest;
export type PlanoPutRequest = PlanoRequest;

@Injectable({ providedIn: 'root' })
export class PlanoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/planos`;

  /**
   * Para perfil ADMIN, é preciso informar a academia-alvo via header
   * X-Academia-Id; para GERENTE, o backend deduz pela sessão.
   */
  private headers(academiaId?: number | null): { headers?: HttpHeaders } {
    return academiaId
      ? { headers: new HttpHeaders({ 'X-Academia-Id': String(academiaId) }) }
      : {};
  }

  listar(academiaId?: number | null): Observable<PlanoResponse[]> {
    return this.http.get<PlanoResponse[]>(this.base, this.headers(academiaId));
  }

  buscar(id: number, academiaId?: number | null): Observable<PlanoResponse> {
    return this.http.get<PlanoResponse>(`${this.base}/${id}`, this.headers(academiaId));
  }

  criar(req: PlanoRequest, academiaId?: number | null): Observable<void> {
    return this.http.post<void>(this.base, req, this.headers(academiaId));
  }

  atualizar(id: number, req: PlanoRequest, academiaId?: number | null): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}`, req, this.headers(academiaId));
  }

  ativar(id: number, academiaId?: number | null): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/ativar`, {}, this.headers(academiaId));
  }

  desativar(id: number, academiaId?: number | null): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/desativar`, {}, this.headers(academiaId));
  }
}
