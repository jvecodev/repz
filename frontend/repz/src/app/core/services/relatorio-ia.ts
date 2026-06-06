import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export type RelatorioStatus = 'PENDENTE' | 'CONCLUIDO' | 'ERRO' | 'CANCELADO';

export interface RelatorioIAResponse {
  id: number;
  status: RelatorioStatus;
  conteudo?: string;
  criadoEm: string;
}

@Injectable({ providedIn: 'root' })
export class RelatorioIAService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/relatorios`;

  iniciar(alunoId: number): Observable<RelatorioIAResponse> {
    return this.http.post<RelatorioIAResponse>(`${this.base}/avaliacao/${alunoId}`, {});
  }

  buscar(id: number): Observable<RelatorioIAResponse> {
    return this.http.get<RelatorioIAResponse>(`${this.base}/${id}`);
  }

  listar(alunoId: number): Observable<RelatorioIAResponse[]> {
    return this.http.get<RelatorioIAResponse[]>(`${this.base}/aluno/${alunoId}`);
  }

  atualizar(id: number, conteudo: string): Observable<RelatorioIAResponse> {
    return this.http.put<RelatorioIAResponse>(`${this.base}/${id}`, { conteudo });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
