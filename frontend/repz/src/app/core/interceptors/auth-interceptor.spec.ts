import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { authInterceptor } from './auth-interceptor';
import { AuthService } from '@core/services/auth';

// Payload: {"sub":"test@repz.com","role":"ALUNO","id":1}
const JWT_STUB =
  'eyJhbGciOiJIUzI1NiJ9' +
  '.eyJzdWIiOiJ0ZXN0QHJlcHouY29tIiwicm9sZSI6IkFMVU5PIiwiaWQiOjF9' +
  '.sig';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('deve ser criado', () => expect(authInterceptor).toBeTruthy());

  it('não adiciona Authorization quando não há token', () => {
    http.get('/api/test').subscribe();
    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('adiciona Authorization: Bearer quando há token', () => {
    localStorage.setItem('JWT_TOKEN', 'meu-token');
    http.get('/api/test').subscribe();
    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer meu-token');
    req.flush({});
  });

  it('propaga erro 401 sem tentar refresh quando não há refresh token', async () => {
    await new Promise<void>((resolve) => {
      http.get('/api/recurso').subscribe({
        error: (err) => { expect(err.status).toBe(401); resolve(); },
      });
      httpMock.expectOne('/api/recurso').flush({}, { status: 401, statusText: 'Unauthorized' });
    });
  });

  it('propaga outros erros diretamente', async () => {
    await new Promise<void>((resolve) => {
      http.get('/api/recurso').subscribe({
        error: (err) => { expect(err.status).toBe(500); resolve(); },
      });
      httpMock.expectOne('/api/recurso').flush({}, { status: 500, statusText: 'Server Error' });
    });
  });

  it('não tenta refresh em endpoints /api/auth/ mesmo com 401', async () => {
    localStorage.setItem('REFRESH_TOKEN', 'rt-123');
    await new Promise<void>((resolve) => {
      http.get('http://localhost:8080/api/auth/login').subscribe({
        error: (err) => { expect(err.status).toBe(401); resolve(); },
      });
      httpMock.expectOne('http://localhost:8080/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    });
  });

  it('tenta renovar token em 401 e refaz a requisição', () => {
    localStorage.setItem('JWT_TOKEN', 'token-expirado');
    localStorage.setItem('REFRESH_TOKEN', 'rt-123');

    let resultado: any;
    http.get('/api/recurso').subscribe((r) => (resultado = r));

    httpMock.expectOne('/api/recurso').flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    expect(refreshReq.request.method).toBe('POST');
    refreshReq.flush({ token: JWT_STUB, refreshToken: 'rt-new' });

    const retryReq = httpMock.expectOne('/api/recurso');
    expect(retryReq.request.headers.get('Authorization')).toContain('Bearer');
    retryReq.flush({ data: 'ok' });

    expect(resultado).toEqual({ data: 'ok' });
  });

  it('faz logout quando refresh falha', async () => {
    localStorage.setItem('JWT_TOKEN', 'token-expirado');
    localStorage.setItem('REFRESH_TOKEN', 'rt-123');

    const logoutSpy = vi.spyOn(authService, 'logout').mockImplementation(() => {});

    await new Promise<void>((resolve) => {
      http.get('/api/recurso').subscribe({
        error: () => { expect(logoutSpy).toHaveBeenCalled(); resolve(); },
      });
      httpMock.expectOne('/api/recurso').flush({}, { status: 401, statusText: 'Unauthorized' });
      httpMock.expectOne('http://localhost:8080/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    });
  });
});
