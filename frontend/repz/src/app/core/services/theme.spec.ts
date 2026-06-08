import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => localStorage.clear());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('tema inicia com valor padrão', () => {
    expect(['dark', 'light']).toContain(service.tema());
  });

  it('toggleTema alterna o tema', () => {
    const before = service.tema();
    service.toggleTema();
    const after = service.tema();
    expect(after).not.toBe(before);
    expect(['dark', 'light']).toContain(after);
  });

  it('toggleTema pode ser alternado de volta', () => {
    const original = service.tema();
    service.toggleTema();
    service.toggleTema();
    expect(service.tema()).toBe(original);
  });

  it('tema salvo no localStorage é respeitado na inicialização', () => {
    localStorage.setItem('repz-tema', 'light');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const svc = TestBed.inject(ThemeService);
    expect(svc.tema()).toBe('light');
  });

  it('tema dark salvo no localStorage é respeitado', () => {
    localStorage.setItem('repz-tema', 'dark');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    const svc = TestBed.inject(ThemeService);
    expect(svc.tema()).toBe('dark');
  });
});
