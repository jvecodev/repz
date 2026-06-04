import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '@env/environment';

@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/files`;

  /** URL temporária (presigned) da foto do usuário logado, ou null se não houver. */
  minhaFoto(): Observable<string | null> {
    return this.http.get(`${this.base}/me`, { responseType: 'text' }).pipe(
      map((url) => (url && url.trim() ? url : null)),
      catchError(() => of(null)),
    );
  }

  /** Faz upload da foto de perfil e retorna a nova URL temporária. */
  upload(file: File): Observable<string> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post(`${this.base}/upload`, form, { responseType: 'text' });
  }
}
