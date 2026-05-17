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

@Injectable({ providedIn: 'root' })
export class FichaTreinoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/treinos`;

  /** Aluno autenticado consulta a própria ficha ativa. */
  obterMinhaFichaAtiva(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me`);
  }

  /** Aluno autenticado consulta o próprio histórico de fichas. */
  obterMeuHistorico(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me/historico`);
  }

  /** Perfis de gestão consultam a ficha ativa de um aluno específico. */
  obterFichaAtivaDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}`, {
      params: { aluno: alunoId },
    });
  }

  /** Perfis de gestão consultam o histórico de fichas de um aluno específico. */
  obterHistoricoDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/historico`, {
      params: { aluno: alunoId },
    });
  }
}
