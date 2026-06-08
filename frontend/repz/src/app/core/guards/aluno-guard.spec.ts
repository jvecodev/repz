import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { alunoGuard } from './aluno-guard';
import { AuthService } from '@core/services/auth';

describe('alunoGuard', () => {
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

  it('deve ser criado', () => expect(alunoGuard).toBeTruthy());

  it('permite acesso a usuário com role ALUNO', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ALUNO');
    const result = TestBed.runInInjectionContext(() => alunoGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('permite acesso a usuário com role ADMIN', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ADMIN');
    const result = TestBed.runInInjectionContext(() => alunoGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('bloqueia acesso a usuário com role GERENTE', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('GERENTE');
    const result = TestBed.runInInjectionContext(() => alunoGuard({} as any, {} as any));
    expect(result).toBe(false);
  });

  it('bloqueia acesso quando não há role (não autenticado)', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue(null);
    const result = TestBed.runInInjectionContext(() => alunoGuard({} as any, {} as any));
    expect(result).toBe(false);
  });
});
