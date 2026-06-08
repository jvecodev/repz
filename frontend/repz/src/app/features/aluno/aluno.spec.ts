import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Aluno } from './aluno';
import { AlunoService } from '@core/services/aluno';
import { FichaTreinoService } from '@core/services/ficha-treino';
import { AvaliacaoFisicaService } from '@core/services/avaliacao-fisica';
import { AuthService } from '@core/services/auth';
import { FrequenciaService } from '@core/services/frequencia';
import { UserService } from '@core/services/user';

const mockPerfil = { id: 1, userId: 1, nome: 'João Silva', email: 'joao@test.com', academiaId: 2, ativo: true, planoNome: 'Premium', personalNome: 'Personal A', objetivo: 'Ganho muscular' };
const mockHistorico = [{ id: 1, dataHora: '01/06/2024 10:00:00', alunoId: 1, alunoNome: 'João', academiaId: 2, academiaNome: 'Academia' }];
const mockTreinos = [{ id: 1, nome: 'Treino A — Peito', divisao: 'A', ativo: true, exercicios: [] }];
const mockAvaliacoes = [
  { id: 1, alunoId: 1, alunoNome: 'João', dataAvaliacao: '01/05/2024 00:00:00', pesoKg: 80 },
  { id: 2, alunoId: 1, alunoNome: 'João', dataAvaliacao: '01/06/2024 00:00:00', pesoKg: 78 },
];

describe('Aluno', () => {
  let component: Aluno;
  let fixture: ComponentFixture<Aluno>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Aluno],
      providers: [provideRouter([]), provideTranslateService(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const alunoService = TestBed.inject(AlunoService);
    const fichaService = TestBed.inject(FichaTreinoService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    const freqService = TestBed.inject(FrequenciaService);
    const authService = TestBed.inject(AuthService);
    const userService = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);

    vi.spyOn(userService, 'carregarNomeLogado').mockImplementation(() => {});
    authService.sessao.set({ email: 'joao@test.com', role: 'ALUNO', id: 1 });
    vi.spyOn(alunoService, 'meuPerfil').mockReturnValue(of(mockPerfil));
    vi.spyOn(freqService, 'meuHistorico').mockReturnValue(of(mockHistorico as any));
    vi.spyOn(fichaService, 'obterMinhaFichaAtiva').mockReturnValue(of(mockTreinos as any));
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(of(mockAvaliacoes as any));

    fixture = TestBed.createComponent(Aluno);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('should create', () => expect(component).toBeTruthy());

  it('carrega perfil do aluno no ngOnInit', () => {
    expect(component.nome()).toBe('João Silva');
    expect(component.planoNome()).toBe('Premium');
    expect(component.personalNome()).toBe('Personal A');
  });

  it('processa frequência e define totalGeral', () => {
    expect(component.totalGeral()).toBe(1);
  });

  it('carrega divisões de treino', () => {
    expect(component.divisoes().length).toBe(1);
  });

  it('treinoDoDia retorna divisão baseado no total de treinos', () => {
    expect(component.treinoDoDia()).not.toBeNull();
  });

  it('focoDoDia extrai o foco após separador', () => {
    expect(component.focoDoDia()).toBe('Peito');
  });

  it('primeiroNome extrai o primeiro nome', () => {
    expect(component.primeiroNome()).toBe('João');
  });

  it('pesoAtual é definido a partir das avaliações', () => {
    expect(component.pesoAtual()).toBe(78);
  });

  it('deltaPeso é calculado entre as duas últimas avaliações', () => {
    expect(component.deltaPeso()).toBe(-2);
  });

  it('carregando é false após ngOnInit', () => {
    expect(component.carregando()).toBe(false);
  });

  it('fazerCheckin ignora quando já fez hoje', () => {
    const freqService = TestBed.inject(FrequenciaService);
    (freqService as any)._jaFezCheckinHoje.set(true);
    const spy = vi.spyOn(freqService, 'registrar');
    component.fazerCheckin();
    expect(spy).not.toHaveBeenCalled();
  });

  it('fazerCheckin chama registrar quando não fez hoje', () => {
    (component as any).alunoId = 1;
    (component as any).academiaId = 2;
    const freqService = TestBed.inject(FrequenciaService);
    (freqService as any)._jaFezCheckinHoje.set(false);
    vi.spyOn(freqService, 'registrar').mockReturnValue(of({ id: 1, dataHora: '01/06/2024', alunoId: 1, alunoNome: '', academiaId: 2, academiaNome: '' }));
    component.fazerCheckin();
    expect(freqService.registrar).toHaveBeenCalled();
    expect(component.avisoCheckinSeverity()).toBe('success');
  });

  it('fazerCheckin lida com erro', () => {
    (component as any).alunoId = 1;
    (component as any).academiaId = 2;
    const freqService = TestBed.inject(FrequenciaService);
    (freqService as any)._jaFezCheckinHoje.set(false);
    vi.spyOn(freqService, 'registrar').mockReturnValue(throwError(() => ({ error: { message: 'Erro checkin' } })));
    component.fazerCheckin();
    expect(component.avisoCheckinSeverity()).toBe('error');
  });

  it('ultimoCheckinTexto retorna texto formatado quando há checkin', () => {
    expect(component.ultimoCheckin()).not.toBeNull();
    const texto = component.ultimoCheckinTexto();
    expect(typeof texto).toBe('string');
    expect(texto.length).toBeGreaterThan(0);
  });

  it('fazerCheckin ignora quando alunoId é zero', () => {
    (component as any).alunoId = 0;
    (component as any).academiaId = 2;
    const freqService = TestBed.inject(FrequenciaService);
    (freqService as any)._jaFezCheckinHoje.set(false);
    const spy = vi.spyOn(freqService, 'registrar');
    component.fazerCheckin();
    expect(spy).not.toHaveBeenCalled();
  });

  it('ngOnInit com falha em meuPerfil usa catchError e carrega sem dados', async () => {
    const alunoService = TestBed.inject(AlunoService);
    const fichaService = TestBed.inject(FichaTreinoService);
    const freqService = TestBed.inject(FrequenciaService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    vi.spyOn(alunoService, 'meuPerfil').mockReturnValue(throwError(() => new Error('err')));
    vi.spyOn(freqService, 'meuHistorico').mockReturnValue(throwError(() => new Error('err')));
    vi.spyOn(fichaService, 'obterMinhaFichaAtiva').mockReturnValue(throwError(() => new Error('err')));
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(throwError(() => new Error('err')));
    const newFixture = TestBed.createComponent(Aluno);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.carregando()).toBe(false);
    expect(newFixture.componentInstance.nome()).toBe('Aluno');
  });

  it('divisões com divisao nula são ordenadas sem erro', async () => {
    const fichaService = TestBed.inject(FichaTreinoService);
    vi.spyOn(fichaService, 'obterMinhaFichaAtiva').mockReturnValue(
      of([
        { id: 1, nome: 'B', divisao: null as any, ativo: true, exercicios: [] },
        { id: 2, nome: 'A', divisao: 'A', ativo: true, exercicios: [] },
      ] as any)
    );
    const newFixture = TestBed.createComponent(Aluno);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.divisoes().length).toBe(2);
  });
});
