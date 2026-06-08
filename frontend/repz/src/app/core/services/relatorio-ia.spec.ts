import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RelatorioIAService } from './relatorio-ia';

const BASE = 'http://localhost:8080/api/relatorios';
const mockRelatorio = { id: 1, status: 'CONCLUIDO' as const, conteudo: 'Texto', criadoEm: '2024-01-01T00:00:00' };

describe('RelatorioIAService', () => {
  let service: RelatorioIAService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RelatorioIAService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('iniciar faz POST /avaliacao/:alunoId', () => {
    service.iniciar(5).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === `${BASE}/avaliacao/5`).flush(mockRelatorio);
  });

  it('buscar faz GET /:id', () => {
    service.buscar(1).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`).flush(mockRelatorio);
  });

  it('listar faz GET /aluno/:alunoId', () => {
    service.listar(3).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/aluno/3`).flush([mockRelatorio]);
  });

  it('atualizar faz PUT /:id', () => {
    service.atualizar(1, 'Novo conteúdo').subscribe();
    const req = http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`);
    expect(req.request.body).toEqual({ conteudo: 'Novo conteúdo' });
    req.flush(mockRelatorio);
  });

  it('excluir faz DELETE /:id', () => {
    service.excluir(1).subscribe();
    http.expectOne((r) => r.method === 'DELETE' && r.url === `${BASE}/1`).flush(null);
  });
});
