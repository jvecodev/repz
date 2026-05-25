import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AcademiaResponse {
  id: number;
  cnpj: string;
  name: string;
  address: string;
  responsible: string;
  phone?: string;
  email?: string;
  active: boolean;
  totalStudents?: number;
  totalInstructors?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AcademiaRequest {
  cnpj: string;
  name: string;
  address: string;
  responsible: string;
  email?: string;
  phone?: string;
}

export type AcademiaUpdateRequest = AcademiaRequest;

export interface AcademiaDashboardResponse {
  totalAcademies: number;
  totalStudents: number;
  totalInstructors: number;
  totalActiveAcademies: number;
  totalInactiveAcademies: number;
  averageStudentsPerAcademy: number;
}

@Injectable({ providedIn: 'root' })
export class AcademiaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/academias`;

  listar(): Observable<AcademiaResponse[]> {
    return this.http.get<AcademiaResponse[]>(this.base);
  }

  buscar(id: number): Observable<AcademiaResponse> {
    return this.http.get<AcademiaResponse>(`${this.base}/${id}`);
  }

  criar(req: AcademiaRequest): Observable<AcademiaResponse> {
    return this.http.post<AcademiaResponse>(this.base, req);
  }

  atualizar(id: number, req: AcademiaRequest): Observable<AcademiaResponse> {
    return this.http.put<AcademiaResponse>(`${this.base}/${id}`, req);
  }

  ativar(id: number): Observable<AcademiaResponse> {
    return this.http.patch<AcademiaResponse>(`${this.base}/${id}/ativar`, {});
  }

  desativar(id: number): Observable<AcademiaResponse> {
    return this.http.patch<AcademiaResponse>(`${this.base}/${id}/desativar`, {});
  }

  /** ACADEMIA logada (GERENTE) consulta sua própria academia. */
  minhaAcademia(): Observable<AcademiaResponse> {
    return this.http.get<AcademiaResponse>(`${this.base}/me`);
  }

  atualizarMinha(req: AcademiaRequest): Observable<AcademiaResponse> {
    return this.http.put<AcademiaResponse>(`${this.base}/me`, req);
  }

  atualizarMinhaAcademia(req: AcademiaUpdateRequest): Observable<AcademiaResponse> {
    return this.atualizarMinha(req);
  }

  dashboard(): Observable<AcademiaDashboardResponse> {
    return this.http.get<AcademiaDashboardResponse>(`${this.base}/dashboard`);
  }
}
