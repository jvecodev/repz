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

  /** RF24 — aluno autenticado vê sua ficha ativa */
  obterMinhaFichaAtiva(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me`);
  }

  /** RF25 — histórico de fichas do aluno autenticado */
  obterMeuHistorico(): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/me/historico`);
  }

  /** RF16 — personal/academia/admin vê a ficha ativa de um aluno */
  obterFichaAtivaDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}`, {
      params: { aluno: alunoId },
    });
  }

  /** RF25 — histórico de fichas de um aluno */
  obterHistoricoDoAluno(alunoId: number): Observable<TreinoResponse[]> {
    return this.http.get<TreinoResponse[]>(`${this.base}/historico`, {
      params: { aluno: alunoId },
    });
  }
}
