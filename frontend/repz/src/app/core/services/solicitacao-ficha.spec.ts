import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SolicitacaoFichaService } from './solicitacao-ficha';

const BASE = 'http://localhost:8080/api/solicitacoes-ficha';
const mockSolicitacao = { id: 1, alunoId: 1, alunoNome: 'Aluno', status: 'PENDENTE' as const, criadaEm: '2024-01-01T00:00:00' };

describe('SolicitacaoFichaService', () => {
  let service: SolicitacaoFichaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SolicitacaoFichaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('criar faz POST', () => {
    service.criar({ mensagem: 'Preciso de ficha' }).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === BASE).flush(mockSolicitacao);
  });

  it('cancelar faz POST /:id/cancelar', () => {
    service.cancelar(1).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === `${BASE}/1/cancelar`).flush(mockSolicitacao);
  });

  it('pendente faz GET /pendente', () => {
    service.pendente().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/pendente`).flush(mockSolicitacao);
  });

  it('listarParaPersonal sem status faz GET sem query string', () => {
    service.listarParaPersonal().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === BASE).flush([mockSolicitacao]);
  });

  it('listarParaPersonal com status inclui query string', () => {
    service.listarParaPersonal('PENDENTE').subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}?status=PENDENTE`).flush([mockSolicitacao]);
  });

  it('responder faz PATCH /:id/responder', () => {
    service.responder(1, { status: 'APROVADA', resposta: 'Ok' }).subscribe();
    const req = http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/responder`);
    expect(req.request.body).toEqual({ status: 'APROVADA', resposta: 'Ok' });
    req.flush({ ...mockSolicitacao, status: 'APROVADA' });
  });
});
