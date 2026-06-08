import { TestBed } from '@angular/core/testing';
import { LayoutService } from './layout';

describe('LayoutService', () => {
  let service: LayoutService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LayoutService);
  });

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('toggleSidebar alterna o estado', () => {
    const initial = service.colapsada();
    service.toggleSidebar();
    expect(service.colapsada()).toBe(!initial);
  });

  it('recolher define colapsada como true', () => {
    service.expandir();
    service.recolher();
    expect(service.colapsada()).toBe(true);
  });

  it('expandir define colapsada como false', () => {
    service.recolher();
    service.expandir();
    expect(service.colapsada()).toBe(false);
  });

  it('fecharMobile fecha em telas pequenas', () => {
    service.expandir();
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 500 });
    service.fecharMobile();
    expect(service.colapsada()).toBe(true);
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1200 });
  });

  it('fecharMobile não fecha em telas grandes', () => {
    service.expandir();
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1200 });
    service.fecharMobile();
    expect(service.colapsada()).toBe(false);
  });
});
