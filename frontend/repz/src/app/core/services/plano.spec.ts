import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PlanoService } from './plano';

const BASE = 'http://localhost:8080/api/planos';
const mockPlano = { id: 1, nome: 'Básico', duracaoDias: 30, valor: 99.9, ativo: true };
const mockReq = { nome: 'Básico', duracaoDias: 30, valor: 99.9 };

describe('PlanoService', () => {
  let service: PlanoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PlanoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('listar faz GET sem header', () => {
    service.listar().subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.has('X-Academia-Id')).toBe(false);
    req.flush([mockPlano]);
  });

  it('listar com academiaId inclui X-Academia-Id', () => {
    service.listar(2).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.get('X-Academia-Id')).toBe('2');
    req.flush([mockPlano]);
  });

  it('buscar faz GET por id', () => {
    service.buscar(1).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`).flush(mockPlano);
  });

  it('buscar com academiaId inclui header', () => {
    service.buscar(1, 2).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`);
    expect(req.request.headers.get('X-Academia-Id')).toBe('2');
    req.flush(mockPlano);
  });

  it('criar faz POST', () => {
    service.criar(mockReq).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === BASE).flush(null);
  });

  it('atualizar faz PUT', () => {
    service.atualizar(1, mockReq).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`).flush(null);
  });

  it('ativar faz PATCH /ativar', () => {
    service.ativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/ativar`).flush(null);
  });

  it('desativar faz PATCH /desativar', () => {
    service.desativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/desativar`).flush(null);
  });
});
