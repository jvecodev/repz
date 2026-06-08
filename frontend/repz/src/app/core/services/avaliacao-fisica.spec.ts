import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AvaliacaoFisicaService } from './avaliacao-fisica';

const BASE = 'http://localhost:8080/api/avaliacoes';
const mockAvaliacao = { id: 1, alunoId: 1, alunoNome: 'Test', dataAvaliacao: '01/01/2024 00:00:00' };

describe('AvaliacaoFisicaService', () => {
  let service: AvaliacaoFisicaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AvaliacaoFisicaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('criar faz POST', () => {
    service.criar({ alunoId: 1, pesoKg: 70, alturaCm: 170 }).subscribe();
    const req = http.expectOne((r) => r.method === 'POST' && r.url === BASE);
    expect(req.request.body.pesoKg).toBe(70);
    req.flush(mockAvaliacao);
  });

  it('listar faz GET com param aluno', () => {
    service.listar(1).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.params.get('aluno')).toBe('1');
    req.flush([mockAvaliacao]);
  });

  it('grafico faz GET /grafico com param aluno', () => {
    service.grafico(1).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/grafico`);
    expect(req.request.params.get('aluno')).toBe('1');
    req.flush({ alunoId: 1, alunoNome: 'Test', dados: [] });
  });
});
