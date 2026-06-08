import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { personalGuard } from './personal-guard';
import { AuthService } from '@core/services/auth';

describe('personalGuard', () => {
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

  it('deve ser criado', () => expect(personalGuard).toBeTruthy());

  it('permite acesso a PERSONAL', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('PERSONAL');
    const result = TestBed.runInInjectionContext(() => personalGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('permite acesso a ADMIN', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ADMIN');
    const result = TestBed.runInInjectionContext(() => personalGuard({} as any, {} as any));
    expect(result).toBe(true);
  });

  it('bloqueia acesso a ALUNO', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ALUNO');
    const result = TestBed.runInInjectionContext(() => personalGuard({} as any, {} as any));
    expect(result).toBe(false);
  });

  it('bloqueia quando não autenticado', () => {
    vi.spyOn(authService, 'getUserRole').mockReturnValue(null);
    const result = TestBed.runInInjectionContext(() => personalGuard({} as any, {} as any));
    expect(result).toBe(false);
  });
});
