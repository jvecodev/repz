import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '@env/environment';

interface LoginResponse {
  token: string;
  refreshToken: string;
}

export interface SessaoUsuario {
  email: string;
  role: string;
  id: number | null;
}

const TOKEN_KEY = 'JWT_TOKEN';
const REFRESH_KEY = 'REFRESH_TOKEN';
const ROLE_KEY = 'USER_ROLE';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/auth`;

  /** Sessão atual derivada do JWT (reativa). */
  readonly sessao = signal<SessaoUsuario | null>(this.lerSessaoDoToken());
  readonly autenticado = computed(() => this.sessao() !== null);

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.base}/login`, { email, password })
      .pipe(tap((res) => this.salvarSessao(res.token, res.refreshToken)));
  }

  logout(): void {
    if (!this.isBrowser()) return;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.sessao.set(null);
  }

  getToken(): string | null {
    return this.isBrowser() ? localStorage.getItem(TOKEN_KEY) : null;
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.base}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/reset-password`, { token, newPassword });
  }

  getUserRole(): string | null {
    return this.sessao()?.role ?? (this.isBrowser() ? localStorage.getItem(ROLE_KEY) : null);
  }

  // ----------------------------------------------------------------
  private salvarSessao(token: string, refreshToken: string): void {
    if (!this.isBrowser()) return;
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    const sessao = this.decodificar(token);
    if (sessao) {
      localStorage.setItem(ROLE_KEY, sessao.role);
      this.sessao.set(sessao);
    }
  }

  private lerSessaoDoToken(): SessaoUsuario | null {
    const token = this.getToken();
    return token ? this.decodificar(token) : null;
  }

  /** Decodifica o payload do JWT (claims: sub=email, role, id). */
  private decodificar(token: string): SessaoUsuario | null {
    try {
      const payload = token.split('.')[1];
      const json = JSON.parse(
        decodeURIComponent(
          atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
            .split('')
            .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join(''),
        ),
      );
      return {
        email: payload ? json.sub : '',
        role: json.role ?? '',
        id: json.id ? Number(json.id) : null,
      };
    } catch {
      return null;
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }
}
