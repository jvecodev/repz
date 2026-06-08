import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FichaTreinoService } from './ficha-treino';

const BASE = 'http://localhost:8080/api/treinos';
const mockTreino = { id: 1, nome: 'Treino A', divisao: 'A', ativo: true, exercicios: [] };

describe('FichaTreinoService', () => {
  let service: FichaTreinoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FichaTreinoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('obterMinhaFichaAtiva faz GET /me', () => {
    service.obterMinhaFichaAtiva().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush([mockTreino]);
  });

  it('obterMeuHistorico faz GET /me/historico', () => {
    service.obterMeuHistorico().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me/historico`).flush([mockTreino]);
  });

  it('obterFichaAtivaDoAluno faz GET com param aluno', () => {
    service.obterFichaAtivaDoAluno(5).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.params.get('aluno')).toBe('5');
    req.flush([mockTreino]);
  });

  it('obterHistoricoDoAluno faz GET /historico com param aluno', () => {
    service.obterHistoricoDoAluno(5).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/historico`);
    expect(req.request.params.get('aluno')).toBe('5');
    req.flush([mockTreino]);
  });

  it('criarDivisao faz POST', () => {
    service.criarDivisao({ alunoId: 1, nome: 'Treino A', divisao: 'A' }).subscribe();
    const req = http.expectOne((r) => r.method === 'POST' && r.url === BASE);
    expect(req.request.body.nome).toBe('Treino A');
    req.flush(mockTreino);
  });

  it('desativarDivisao faz PATCH /:id/desativar', () => {
    service.desativarDivisao(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/desativar`).flush(null);
  });
});
