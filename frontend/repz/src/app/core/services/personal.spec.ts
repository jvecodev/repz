import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PersonalService } from './personal';

const BASE = 'http://localhost:8080/api/personais';
const mockPersonal = { id: 1, userId: 1, userName: 'Personal Test', email: 'p@p.com', academiaId: 1, ativo: true };

describe('PersonalService', () => {
  let service: PersonalService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PersonalService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('listar sem academiaId faz GET sem header', () => {
    service.listar().subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.has('X-Academia-Id')).toBe(false);
    req.flush([mockPersonal]);
  });

  it('listar com academiaId inclui X-Academia-Id', () => {
    service.listar(3).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.headers.get('X-Academia-Id')).toBe('3');
    req.flush([mockPersonal]);
  });

  it('buscar faz GET por id', () => {
    service.buscar(1).subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/1`).flush(mockPersonal);
  });

  it('atualizar faz PUT', () => {
    service.atualizar(1, { especialidade: 'Musculação' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`).flush(mockPersonal);
  });

  it('ativar faz PATCH /ativar', () => {
    service.ativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/ativar`).flush(mockPersonal);
  });

  it('desativar faz PATCH /desativar', () => {
    service.desativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/desativar`).flush(mockPersonal);
  });

  it('meuPerfil faz GET /me e atualiza nomePersonal', () => {
    service.meuPerfil().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush(mockPersonal);
    expect(service.nomePersonal()).toBe('Personal Test');
  });

  it('meuPerfil com userName nulo define nomePersonal como string vazia', () => {
    service.meuPerfil().subscribe();
    http.expectOne((r) => r.url === `${BASE}/me`).flush({ ...mockPersonal, userName: null });
    expect(service.nomePersonal()).toBe('');
  });

  it('atualizarMeuPerfil faz PUT /me', () => {
    service.atualizarMeuPerfil({ especialidade: 'Pilates' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/me`).flush({ ...mockPersonal, especialidade: 'Pilates' });
  });

  it('atualizarMeuPerfil com userName nulo não atualiza nomePersonal', () => {
    service.atualizarMeuPerfil({ especialidade: 'Pilates' }).subscribe();
    http.expectOne((r) => r.url === `${BASE}/me`).flush({ ...mockPersonal, userName: null });
    expect(service.nomePersonal()).toBe('');
  });

  it('meusAlunos faz GET /me/alunos', () => {
    service.meusAlunos().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me/alunos`).flush({ personalId: 1, personalNome: 'Test', alunos: [] });
  });
});
