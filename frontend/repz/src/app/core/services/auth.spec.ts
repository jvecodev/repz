import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth';

// Payload: {"sub":"test@repz.com","role":"ALUNO","id":1}
const JWT_STUB =
  'eyJhbGciOiJIUzI1NiJ9' +
  '.eyJzdWIiOiJ0ZXN0QHJlcHouY29tIiwicm9sZSI6IkFMVU5PIiwiaWQiOjF9' +
  '.sig';

const BASE = 'http://localhost:8080/api/auth';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('getToken retorna null quando localStorage vazio', () => {
    expect(service.getToken()).toBeNull();
  });

  it('getToken retorna token salvo no localStorage', () => {
    localStorage.setItem('JWT_TOKEN', 'meu-token');
    expect(service.getToken()).toBe('meu-token');
  });

  it('getRefreshToken retorna null quando não há token', () => {
    expect(service.getRefreshToken()).toBeNull();
  });

  it('getRefreshToken retorna valor do localStorage', () => {
    localStorage.setItem('REFRESH_TOKEN', 'rt-123');
    expect(service.getRefreshToken()).toBe('rt-123');
  });

  it('getUserRole retorna null sem sessão ativa', () => {
    expect(service.getUserRole()).toBeNull();
  });

  it('getUserRole retorna role do signal quando sessão existe', () => {
    service.sessao.set({ email: 'x@x.com', role: 'ADMIN', id: 1 });
    expect(service.getUserRole()).toBe('ADMIN');
  });

  it('getUserRole usa localStorage como fallback quando sessao é null', () => {
    service.sessao.set(null);
    localStorage.setItem('USER_ROLE', 'GERENTE');
    expect(service.getUserRole()).toBe('GERENTE');
  });

  it('autenticado é false sem sessão', () => {
    expect(service.autenticado()).toBe(false);
  });

  it('autenticado é true com sessão definida', () => {
    service.sessao.set({ email: 'x@x.com', role: 'ALUNO', id: 2 });
    expect(service.autenticado()).toBe(true);
  });

  it('login faz POST /login e salva tokens no localStorage', () => {
    service.login('test@repz.com', '123456').subscribe();

    const req = http.expectOne(`${BASE}/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'test@repz.com', password: '123456' });
    req.flush({ token: JWT_STUB, refreshToken: 'rt-abc' });

    expect(localStorage.getItem('JWT_TOKEN')).toBe(JWT_STUB);
    expect(localStorage.getItem('REFRESH_TOKEN')).toBe('rt-abc');
  });

  it('login decodifica o JWT e define a sessão', () => {
    service.login('test@repz.com', '123456').subscribe();
    http.expectOne(`${BASE}/login`).flush({ token: JWT_STUB, refreshToken: 'rt' });
    expect(service.sessao()?.role).toBe('ALUNO');
    expect(service.sessao()?.id).toBe(1);
  });

  it('logout sem token não faz requisição HTTP', () => {
    service.logout();
    http.expectNone(`${BASE}/logout`);
  });

  it('logout com token faz POST /logout e limpa sessão', () => {
    localStorage.setItem('JWT_TOKEN', 'tok');
    service.sessao.set({ email: 'x@x.com', role: 'ALUNO', id: 1 });

    service.logout();

    const req = http.expectOne(`${BASE}/logout`);
    expect(req.request.method).toBe('POST');
    req.flush(null);

    expect(localStorage.getItem('JWT_TOKEN')).toBeNull();
    expect(service.sessao()).toBeNull();
  });

  it('refresh retorna throwError quando não há refresh token', async () => {
    await new Promise<void>((resolve) => {
      service.refresh().subscribe({
        error: (e) => {
          expect(e.message).toContain('ausente');
          resolve();
        },
      });
    });
  });

  it('refresh faz POST /refresh com refresh token disponível', () => {
    localStorage.setItem('REFRESH_TOKEN', 'rt-123');
    service.refresh().subscribe();

    const req = http.expectOne(`${BASE}/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe('rt-123');
    req.flush({ token: JWT_STUB, refreshToken: 'rt-new' });

    expect(localStorage.getItem('JWT_TOKEN')).toBe(JWT_STUB);
  });

  it('forgotPassword faz POST /forgot-password', () => {
    service.forgotPassword('a@b.com').subscribe();
    const req = http.expectOne(`${BASE}/forgot-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.com' });
    req.flush(null);
  });

  it('resetPassword faz POST /reset-password', () => {
    service.resetPassword('tok123', 'novaSenha').subscribe();
    const req = http.expectOne(`${BASE}/reset-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'tok123', newPassword: 'novaSenha' });
    req.flush(null);
  });

  it('decodificar retorna null para token malformado (catch block)', () => {
    const result = (service as any).decodificar('nao.eh.base64.valido!!!');
    expect(result).toBeNull();
  });

  it('decodificar retorna null quando token não tem pontos', () => {
    const result = (service as any).decodificar('sempontos');
    expect(result).toBeNull();
  });

  it('decodificar retorna null quando payload não é JSON', () => {
    const b64 = btoa('not-json-at-all');
    const result = (service as any).decodificar(`header.${b64}.sig`);
    expect(result).toBeNull();
  });

  it('getUserRole usa localStorage como fallback quando sessao é null e há role salva', () => {
    service.sessao.set(null);
    localStorage.setItem('USER_ROLE', 'GERENTE');
    expect(service.getUserRole()).toBe('GERENTE');
  });

  it('autenticado retorna false quando sessao é null', () => {
    service.sessao.set(null);
    expect(service.autenticado()).toBe(false);
  });

  it('logout limpa sessao sem fazer request quando não há token', () => {
    service.sessao.set(null);
    service.logout();
    expect(service.sessao()).toBeNull();
  });
});
