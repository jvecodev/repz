import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { FrequenciaService } from './frequencia';

const BASE = 'http://localhost:8080/api/checkins';
const STORAGE_KEY = 'repz_checkin_hoje';

function todayBR(): string {
  const d = new Date();
  const dia = String(d.getDate()).padStart(2, '0');
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  return `${dia}/${mes}/${d.getFullYear()}`;
}

describe('FrequenciaService', () => {
  let service: FrequenciaService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FrequenciaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('deve ser criado', () => expect(service).toBeTruthy());

  it('jaFezCheckinHoje inicia false quando sem localStorage', () => {
    expect(service.jaFezCheckinHoje()).toBe(false);
  });

  it('jaFezCheckinHoje inicia true quando localStorage contém hoje', () => {
    localStorage.setItem(STORAGE_KEY, todayBR());
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const svc = TestBed.inject(FrequenciaService);
    expect(svc.jaFezCheckinHoje()).toBe(true);
    TestBed.inject(HttpTestingController).verify();
  });

  it('meuHistorico faz GET /me', () => {
    service.meuHistorico().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/me`).flush([]);
  });

  it('meuHistorico marca checkin hoje quando há registro com data de hoje', () => {
    service.meuHistorico().subscribe();
    http.expectOne(`${BASE}/me`).flush([{ dataHora: todayBR() + ' 10:00:00', id: 1, alunoId: 1, alunoNome: '', academiaId: 1, academiaNome: '' }]);
    expect(service.jaFezCheckinHoje()).toBe(true);
  });

  it('meuHistorico reseta checkin quando não há registro de hoje', () => {
    service.meuHistorico().subscribe();
    http.expectOne(`${BASE}/me`).flush([{ dataHora: '01/01/2000 10:00:00', id: 1, alunoId: 1, alunoNome: '', academiaId: 1, academiaNome: '' }]);
    expect(service.jaFezCheckinHoje()).toBe(false);
  });

  it('registrar faz POST e marca checkin', () => {
    service.registrar({ alunoId: 1, academiaId: 1 }).subscribe();
    const req = http.expectOne((r) => r.method === 'POST' && r.url === BASE);
    expect(req.request.body).toEqual({ alunoId: 1, academiaId: 1 });
    req.flush({ id: 1, dataHora: todayBR(), alunoId: 1, alunoNome: '', academiaId: 1, academiaNome: '' });
    expect(service.jaFezCheckinHoje()).toBe(true);
  });

  it('registrar 409 marca checkin mesmo assim', () => {
    service.registrar({ alunoId: 1, academiaId: 1 }).subscribe({ error: () => {} });
    http.expectOne(BASE).flush({ message: 'Já fez check-in' }, { status: 409, statusText: 'Conflict' });
    expect(service.jaFezCheckinHoje()).toBe(true);
  });

  it('listarPorAluno faz GET com params', () => {
    const inicio = new Date(2024, 0, 1);
    const fim = new Date(2024, 0, 31);
    service.listarPorAluno(1, inicio, fim).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.params.get('aluno')).toBe('1');
    req.flush([]);
  });

  it('listarPeriodo faz GET com params', () => {
    const inicio = new Date(2024, 0, 1);
    const fim = new Date(2024, 0, 31);
    service.listarPeriodo(inicio, fim, 1).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.params.get('academia')).toBe('1');
    req.flush([]);
  });

  it('listarPeriodo sem academiaId não inclui param academia', () => {
    const inicio = new Date(2024, 0, 1);
    const fim = new Date(2024, 0, 31);
    service.listarPeriodo(inicio, fim).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === BASE);
    expect(req.request.params.has('academia')).toBe(false);
    req.flush([]);
  });

  it('alunosInativos faz GET /alunos/inativos', () => {
    service.alunosInativos().subscribe();
    http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/alunos/inativos`).flush([]);
  });

  it('relatorio faz GET /relatorio com params', () => {
    const inicio = new Date(2024, 0, 1);
    const fim = new Date(2024, 0, 31);
    service.relatorio(inicio, fim, 1).subscribe();
    const req = http.expectOne((r) => r.method === 'GET' && r.url === `${BASE}/relatorio`);
    expect(req.request.params.get('academia')).toBe('1');
    req.flush({ academiaId: 1, periodo: {}, totalFrequencias: 0, frequenciaPorAluno: {}, ocupacaoPorHora: {}, frequenciaPorMes: {} });
  });

  it('carregarStatusHoje chama meuHistorico quando não carregado', () => {
    service.carregarStatusHoje();
    http.expectOne((r) => r.url === `${BASE}/me`).flush([]);
  });

  it('carregarStatusHoje não duplica chamada', () => {
    service.carregarStatusHoje();
    http.expectOne(`${BASE}/me`).flush([]);
    service.carregarStatusHoje();
    http.expectNone(`${BASE}/me`);
  });
});
