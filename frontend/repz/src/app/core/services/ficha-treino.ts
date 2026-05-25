import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface ExercicioTreinoResponse {
  id: number;
  nomeExercicio: string;
  grupoMuscular?: string;
  series?: number;
  repeticoes?: string;
  cargaKg?: number;
  descansoSegundos?: number;
  ordem?: number;
  observacao?: string;
}

export interface TreinoResponse {
  id: number;
  nome: string;
  divisao: string;
  objetivo?: string;
  observacoes?: string;
  ativo: boolean;
  validadeAte?: string;
  alunoId?: number;
  alunoNome?: string;
  personalId?: number;
  personalNome?: string;
  academiaId?: number;
  academiaNome?: string;
  dataInclusao?: string;
  dataAlteracao?: string;
  exercicios: ExercicioTreinoResponse[];
}

export interface ExercicioCreateRequest {
  nomeExercicio: string;
  grupoMuscular?: string;
  series?: number;
  repeticoes?: string;
  cargaKg?: number;
  descansoSegundos?: number;
  ordem?: number;
  observacao?: string;
}

export interface TreinoCreateRequest {
  alunoId: number;
  nome: string;
  divisao: string;
  objetivo?: string;
  observacoes?: string;

  validadeAte?: string;
  exercicios?: ExercicioCreateRequest[];
}

@Injectable({ providedIn: 'root' })
export class FichaTreinoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/treinos`;

  obterMinhaFichaAtiva(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me`);
  }

  obterMeuHistorico(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me/historico`);
  }

  obterFichaAtivaDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}`, {
      params: { aluno: alunoId },
    });
  }

  obterHistoricoDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/historico`, {
      params: { aluno: alunoId },
    });
  }

  criarDivisao(req: TreinoCreateRequest): Observable<TreinoResponse> {
    return this.http.post<TreinoResponse>(this.base, req);
  }

  desativarDivisao(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/desativar`, {});
  }
}
