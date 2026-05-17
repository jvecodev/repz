import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AvaliacaoFisicaResponse {
  id: number;
  alunoId: number;
  alunoNome: string;
  personalId?: number;
  personalNome?: string;
  academiaId?: number;
  academiaNome?: string;
  dataAvaliacao: string;
  pesoKg?: number;
  alturaCm?: number;
  imc?: number;
  percentualGordura?: number;
  medidas?: string;
  cinturaCm?: number;
  quadrilCm?: number;
  bracoCm?: number;
  coxaCm?: number;
}

export interface AvaliacaoFisicaCreateRequest {
  alunoId: number;
  pesoKg: number;
  alturaCm: number;
  percentualGordura?: number;
  cinturaCm?: number;
  quadrilCm?: number;
  bracoCm?: number;
  coxaCm?: number;
}

export interface DadoGrafico {
  data: string;
  peso?: number;
  imc?: number;
  percentualGordura?: number;
}

export interface AvaliacaoGraficoResponse {
  alunoId: number;
  alunoNome: string;
  dados: DadoGrafico[];
}

@Injectable({ providedIn: 'root' })
export class AvaliacaoFisicaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/avaliacoes`;

  /** RF27 — PERSONAL registra avaliação. */
  criar(req: AvaliacaoFisicaCreateRequest): Observable<AvaliacaoFisicaResponse> {
    return this.http.post<AvaliacaoFisicaResponse>(this.base, req);
  }

  /** RF29 — histórico do aluno (PERSONAL/USUARIO/ACADEMIA/ADMIN). */
  listar(alunoId: number): Observable<AvaliacaoFisicaResponse[]> {
    return this.http.get<AvaliacaoFisicaResponse[]>(this.base, {
      params: { aluno: alunoId },
    });
  }

  /** RF30 — pontos de evolução para o gráfico. */
  grafico(alunoId: number): Observable<AvaliacaoGraficoResponse> {
    return this.http.get<AvaliacaoGraficoResponse>(`${this.base}/grafico`, {
      params: { aluno: alunoId },
    });
  }
}
