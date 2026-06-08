import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user';

const BASE = 'http://localhost:8080/api/users';
const mockUser = { id: 1, name: 'Test', email: 't@t.com', role: 'ALUNO' as const, active: true };

describe('UserService', () => {
  let service: UserService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('criar faz POST', () => {
    service.criar({ name: 'A', email: 'a@a.com', password: '123', role: 'ALUNO', academiaId: 1 }).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === BASE).flush(null);
  });

  it('listar faz GET', () => {
    service.listar().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === BASE).flush([]);
  });

  it('atualizar faz PUT', () => {
    service.atualizar(1, { name: 'B', email: 'b@b.com' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/1`).flush(null);
  });

  it('ativar faz PATCH', () => {
    service.ativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/ativar`).flush(null);
  });

  it('desativar faz PATCH', () => {
    service.desativar(1).subscribe();
    http.expectOne((r) => r.method === 'PATCH' && r.url === `${BASE}/1/desativar`).flush(null);
  });

  it('meuPerfil faz GET e atualiza signal', () => {
    service.meuPerfil().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush(mockUser);
    expect(service.nomeUsuario()).toBe('Test');
  });

  it('atualizarMeuPerfil faz PUT e atualiza nome', () => {
    service.atualizarMeuPerfil({ name: 'Novo', email: 'n@n.com' }).subscribe();
    http.expectOne((r) => r.method === 'PUT' && r.url === `${BASE}/me`).flush({ ...mockUser, name: 'Novo' });
    expect(service.nomeUsuario()).toBe('Novo');
  });

  it('uploadFoto faz POST com FormData', () => {
    const file = new File([''], 'foto.jpg', { type: 'image/jpeg' });
    service.uploadFoto(file).subscribe();
    http.expectOne((r) => r.method === 'POST' && r.url === `${BASE}/me/foto`).flush(mockUser);
  });

  it('carregarNomeLogado chama meuPerfil quando nomeCarregado é false', () => {
    service.carregarNomeLogado();
    http.expectOne((r) => r.url === `${BASE}/me`).flush(mockUser);
  });

  it('carregarNomeLogado não chama meuPerfil quando já carregado', () => {
    service.meuPerfil().subscribe();
    http.expectOne((r) => r.url === `${BASE}/me`).flush(mockUser);
    service.carregarNomeLogado();
    http.expectNone(BASE);
  });

  it('setFotoUrl atualiza signal', () => {
    service.setFotoUrl('https://url.com/foto.jpg');
    expect(service.fotoUrl()).toBe('https://url.com/foto.jpg');
  });

  it('resetar limpa nome e foto', () => {
    service.setFotoUrl('foto.jpg');
    service.resetar();
    expect(service.nomeUsuario()).toBe('');
    expect(service.fotoUrl()).toBeNull();
  });
});
