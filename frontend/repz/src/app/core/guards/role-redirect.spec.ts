import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { homePorRole, checarAcesso } from './role-redirect';
import { AuthService } from '@core/services/auth';

describe('homePorRole', () => {
  it('ADMIN → /admin', () => expect(homePorRole('ADMIN')).toBe('/admin'));
  it('GERENTE → /academia', () => expect(homePorRole('GERENTE')).toBe('/academia'));
  it('PERSONAL → /personal', () => expect(homePorRole('PERSONAL')).toBe('/personal'));
  it('ALUNO → /aluno/ficha-treino', () => expect(homePorRole('ALUNO')).toBe('/aluno/ficha-treino'));
  it('null → /auth', () => expect(homePorRole(null)).toBe('/auth'));
  it('desconhecido → /auth', () => expect(homePorRole('OUTRO')).toBe('/auth'));
});

describe('checarAcesso', () => {
  let authService: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    authService = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('permite quando role está na lista', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ALUNO');
    const result = TestBed.runInInjectionContext(() => checarAcesso(['ALUNO', 'ADMIN']));
    expect(result).toBe(true);
  });

  it('bloqueia quando role não está na lista', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('GERENTE');
    const result = TestBed.runInInjectionContext(() => checarAcesso(['ALUNO']));
    expect(result).toBe(false);
  });

  it('redireciona para /auth quando sem role', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue(null);
    const result = TestBed.runInInjectionContext(() => checarAcesso(['ALUNO']));
    expect(result).toBe(false);
  });

  it('ADMIN tem acesso quando ADMIN está na lista', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ADMIN');
    const result = TestBed.runInInjectionContext(() => checarAcesso(['ADMIN']));
    expect(result).toBe(true);
  });
});
