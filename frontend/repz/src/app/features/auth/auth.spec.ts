import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Auth } from './auth';
import { AuthService } from '@core/services/auth';

describe('Auth', () => {
  let component: Auth;
  let fixture: ComponentFixture<Auth>;
  let authService: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Auth],
      providers: [provideRouter([]), provideTranslateService(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Auth);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('should create', () => expect(component).toBeTruthy());

  it('etapa inicial é login', () => {
    expect((component as any).etapa()).toBe('login');
  });

  it('irParaEsqueci muda etapa para esqueci', () => {
    (component as any).irParaEsqueci();
    expect((component as any).etapa()).toBe('esqueci');
  });

  it('irParaLogin retorna à etapa de login', () => {
    (component as any).irParaEsqueci();
    (component as any).irParaLogin();
    expect((component as any).etapa()).toBe('login');
  });

  it('irParaLogin com mensagem define sucesso', () => {
    (component as any).irParaLogin('Senha redefinida com sucesso!');
    expect((component as any).sucesso()).toBe('Senha redefinida com sucesso!');
  });

  it('realizarLogin com form inválido não faz login', () => {
    const loginSpy = vi.spyOn(authService, 'login');
    (component as any).realizarLogin();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('realizarLogin com credenciais válidas chama authService.login', () => {
    vi.spyOn(authService, 'login').mockReturnValue(of({ token: 'tok', refreshToken: 'rt' }));
    vi.spyOn(authService, 'getUserRole').mockReturnValue('ALUNO');

    (component as any).loginForm.setValue({ email: 'test@repz.com', password: '123456' });
    (component as any).realizarLogin();

    expect(authService.login).toHaveBeenCalledWith('test@repz.com', '123456');
  });

  it('realizarLogin trata erro 401', () => {
    vi.spyOn(authService, 'login').mockReturnValue(throwError(() => ({ status: 401 })));
    (component as any).loginForm.setValue({ email: 'test@repz.com', password: '123456' });
    (component as any).realizarLogin();
    expect((component as any).carregando()).toBe(false);
  });

  it('realizarLogin trata erro 403', () => {
    vi.spyOn(authService, 'login').mockReturnValue(throwError(() => ({ status: 403 })));
    (component as any).loginForm.setValue({ email: 'test@repz.com', password: '123456' });
    (component as any).realizarLogin();
    expect((component as any).carregando()).toBe(false);
  });

  it('realizarLogin trata erro genérico', () => {
    vi.spyOn(authService, 'login').mockReturnValue(throwError(() => ({ status: 500 })));
    (component as any).loginForm.setValue({ email: 'test@repz.com', password: '123456' });
    (component as any).realizarLogin();
    expect((component as any).carregando()).toBe(false);
  });

  it('enviarEsqueci com form inválido não faz chamada', () => {
    (component as any).irParaEsqueci();
    const spy = vi.spyOn(authService, 'forgotPassword');
    (component as any).enviarEsqueci();
    expect(spy).not.toHaveBeenCalled();
  });

  it('enviarEsqueci com email válido chama forgotPassword', () => {
    vi.spyOn(authService, 'forgotPassword').mockReturnValue(of(undefined as any));
    (component as any).irParaEsqueci();
    (component as any).esqueciForm.setValue({ email: 'test@repz.com' });
    (component as any).enviarEsqueci();
    expect(authService.forgotPassword).toHaveBeenCalledWith('test@repz.com');
  });

  it('redefinirSenha com form inválido não faz chamada', () => {
    const spy = vi.spyOn(authService, 'resetPassword');
    (component as any).redefinirSenha();
    expect(spy).not.toHaveBeenCalled();
  });

  it('redefinirSenha com dados válidos chama resetPassword', () => {
    vi.spyOn(authService, 'resetPassword').mockReturnValue(of(undefined as any));
    (component as any).redefinirForm.setValue({ token: 'abc123', newPassword: 'novaSenha', confirmarSenha: 'novaSenha' });
    (component as any).redefinirSenha();
    expect(authService.resetPassword).toHaveBeenCalledWith('abc123', 'novaSenha');
  });

  it('redefinirSenha trata erro 400', () => {
    vi.spyOn(authService, 'resetPassword').mockReturnValue(throwError(() => ({ status: 400 })));
    (component as any).redefinirForm.setValue({ token: 'abc', newPassword: 'senha1', confirmarSenha: 'senha1' });
    (component as any).redefinirSenha();
    expect((component as any).carregando()).toBe(false);
  });
});
