import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AlunoService } from './aluno';

const BASE = 'http://localhost:8080/api/alunos';
const mockAluno = { id: 1, userId: 1, nome: 'Aluno Test', email: 'a@a.com', academiaId: 1, ativo: true };

describe('AlunoService', () => {
  let service: AlunoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AlunoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('listar faz GET sem header de academia', () => {
    service.listar().subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.has('X-Academia-Id')).toBe(false);
    req.flush([mockAluno]);
  });

  it('listar passa X-Academia-Id quando informado', () => {
    service.listar(5).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.get('X-Academia-Id')).toBe('5');
    req.flush([mockAluno]);
  });

  it('buscar faz GET por id', () => {
    service.buscar(1).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`).flush(mockAluno);
  });

  it('atualizar faz PUT', () => {
    service.atualizar(1, { objetivo: 'Ganho muscular' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`).flush(mockAluno);
  });

  it('atualizar com academiaId inclui header', () => {
    service.atualizar(1, {}, 2).subscribe();
    const req = http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`);
    expect(req.request.headers.get('X-Academia-Id')).toBe('2');
    req.flush(mockAluno);
  });

  it('inativar faz PATCH', () => {
    service.inativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/inativar`).flush(null);
  });

  it('meuPerfil faz GET /me', () => {
    service.meuPerfil().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush(mockAluno);
  });

  it('atualizarMeuPerfil faz PUT /me', () => {
    service.atualizarMeuPerfil({ nome: 'Novo Nome' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/me`).flush(mockAluno);
  });
});
