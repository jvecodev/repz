import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AcademiaService } from './academia';

const BASE = 'http://localhost:8080/api/academias';
const mockAcademia = { id: 1, cnpj: '12.345.678/0001-90', name: 'Academia Test', address: 'Rua A', responsible: 'Resp', active: true };
const mockReq = { cnpj: '12.345.678/0001-90', name: 'Academia Test', address: 'Rua A', responsible: 'Resp' };

describe('AcademiaService', () => {
  let service: AcademiaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AcademiaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('listar faz GET /api/academias', () => {
    service.listar().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === BASE).flush([mockAcademia]);
  });

  it('buscar faz GET /api/academias/:id', () => {
    service.buscar(1).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`).flush(mockAcademia);
  });

  it('criar faz POST', () => {
    service.criar(mockReq).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === BASE).flush(mockAcademia);
  });

  it('atualizar faz PUT', () => {
    service.atualizar(1, mockReq).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`).flush(mockAcademia);
  });

  it('ativar faz PATCH /ativar', () => {
    service.ativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/ativar`).flush(mockAcademia);
  });

  it('desativar faz PATCH /desativar', () => {
    service.desativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/desativar`).flush(mockAcademia);
  });

  it('minhaAcademia faz GET /me', () => {
    service.minhaAcademia().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush(mockAcademia);
  });

  it('atualizarMinha faz PUT /me', () => {
    service.atualizarMinha(mockReq).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/me`).flush(mockAcademia);
  });

  it('atualizarMinhaAcademia delega para atualizarMinha', () => {
    service.atualizarMinhaAcademia(mockReq).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/me`).flush(mockAcademia);
  });

  it('dashboard faz GET /dashboard', () => {
    service.dashboard().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/dashboard`).flush({
      totalAcademies: 0, totalStudents: 0, totalInstructors: 0,
      totalActiveAcademies: 0, totalInactiveAcademies: 0, averageStudentsPerAcademy: 0,
    });
  });
});
