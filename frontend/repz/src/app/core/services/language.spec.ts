import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { LanguageService } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideTranslateService()],
    });
    service = TestBed.inject(LanguageService);
  });

  afterEach(() => localStorage.clear());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('idioma padrão é pt-BR', () => {
    expect(service.idioma()).toBe('pt-BR');
  });

  it('usar define o idioma e persiste no localStorage', () => {
    service.usar('en-US');
    expect(service.idioma()).toBe('en-US');
    expect(localStorage.getItem('repz_lang')).toBe('en-US');
  });

  it('alternar muda de pt-BR para en-US', () => {
    service.usar('pt-BR');
    service.alternar();
    expect(service.idioma()).toBe('en-US');
  });

  it('alternar muda de en-US para pt-BR', () => {
    service.usar('en-US');
    service.alternar();
    expect(service.idioma()).toBe('pt-BR');
  });

  it('idioma salvo no localStorage é lido na inicialização', () => {
    localStorage.setItem('repz_lang', 'en-US');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    const svc = TestBed.inject(LanguageService);
    expect(svc.idioma()).toBe('en-US');
  });

  it('valor inválido no localStorage usa pt-BR', () => {
    localStorage.setItem('repz_lang', 'es-ES');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    const svc = TestBed.inject(LanguageService);
    expect(svc.idioma()).toBe('pt-BR');
  });
});
