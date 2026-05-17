import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type SolicitacaoStatus = 'PENDENTE' | 'APROVADA' | 'REJEITADA' | 'CANCELADA';

export interface SolicitacaoFichaResponse {
  id: number;
  alunoId: number;
  alunoNome: string;
  personalId?: number;
  personalNome?: string;
  mensagem?: string;
  status: SolicitacaoStatus;
  resposta?: string;
  criadaEm: string;
  respondidaEm?: string;
}

export interface SolicitacaoFichaCreateRequest {
  personalId?: number;
  mensagem?: string;
}

export interface SolicitacaoFichaResponderRequest {
  status: 'APROVADA' | 'REJEITADA';
  resposta?: string;
}

@Injectable({ providedIn: 'root' })
export class SolicitacaoFichaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/solicitacoes-ficha`;

  /** Aluno solicita nova ficha. */
  criar(request: SolicitacaoFichaCreateRequest): Observable<SolicitacaoFichaResponse> {
    return this.http.post<SolicitacaoFichaResponse>(this.base, request);
  }

  /** Aluno cancela solicitação pendente. */
  cancelar(id: number): Observable<SolicitacaoFichaResponse> {
    return this.http.post<SolicitacaoFichaResponse>(`${this.base}/${id}/cancelar`, {});
  }

  /** Aluno consulta se tem solicitação pendente. */
  pendente(): Observable<SolicitacaoFichaResponse | null> {
    return this.http.get<SolicitacaoFichaResponse | null>(`${this.base}/pendente`);
  }

  /** Personal lista solicitações dos seus alunos. */
  listarParaPersonal(status?: SolicitacaoStatus): Observable<SolicitacaoFichaResponse[]> {
    const params = status ? `?status=${status}` : '';
    return this.http.get<SolicitacaoFichaResponse[]>(`${this.base}${params}`);
  }

  /** Personal responde (aprova/rejeita) uma solicitação. */
  responder(id: number, request: SolicitacaoFichaResponderRequest): Observable<SolicitacaoFichaResponse> {
    return this.http.patch<SolicitacaoFichaResponse>(`${this.base}/${id}/responder`, request);
  }
}
