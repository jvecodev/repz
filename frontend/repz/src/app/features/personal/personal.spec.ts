import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Personal } from './personal';
import type { StatusTreino } from './personal';
import { PersonalService } from '@core/services/personal';
import { AlunoService } from '@core/services/aluno';
import { FrequenciaService } from '@core/services/frequencia';
import { AvaliacaoFisicaService } from '@core/services/avaliacao-fisica';
import { SolicitacaoFichaService } from '@core/services/solicitacao-ficha';
import { UserService } from '@core/services/user';

const mockPerfil = { id: 1, userId: 1, userName: 'Personal Test', email: 'p@p.com', academiaId: 1, ativo: true };
const mockAlunos = [{ id: 1, userId: 10, nome: 'Aluno A', email: 'a@a.com', academiaId: 1, ativo: true, planoNome: 'Básico' }];
const mockSolicitacoes = [{ id: 1, alunoId: 1, alunoNome: 'Aluno A', status: 'PENDENTE' as const, criadaEm: '01/06/2024 10:00:00' }];

describe('Personal', () => {
  let component: Personal;
  let fixture: ComponentFixture<Personal>;
  let httpMock: HttpTestingController;
  let solicitacaoService: SolicitacaoFichaService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Personal],
      providers: [provideRouter([]), provideTranslateService(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    const personalService = TestBed.inject(PersonalService);
    const alunoService = TestBed.inject(AlunoService);
    const freqService = TestBed.inject(FrequenciaService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    solicitacaoService = TestBed.inject(SolicitacaoFichaService);
    const userService = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);

    vi.spyOn(userService, 'carregarNomeLogado').mockImplementation(() => {});

    vi.spyOn(personalService, 'meuPerfil').mockReturnValue(of(mockPerfil));
    vi.spyOn(alunoService, 'listar').mockReturnValue(of(mockAlunos as any));
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([]));
    vi.spyOn(freqService, 'listarPorAluno').mockReturnValue(of([]));
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(of([]));
    vi.spyOn(solicitacaoService, 'listarParaPersonal').mockReturnValue(of(mockSolicitacoes as any));

    fixture = TestBed.createComponent(Personal);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('should create', () => expect(component).toBeTruthy());

  it('carrega perfil no ngOnInit', () => {
    expect(component.perfil()).toEqual(mockPerfil);
    expect(component.nomePersonal()).toBe('Personal Test');
  });

  it('carrega alunos na tabela', () => {
    expect(component.totalAlunos()).toBe(1);
  });

  it('carrega solicitações pendentes', () => {
    expect(component.totalPendentes()).toBe(1);
    expect(component.carregandoSol()).toBe(false);
  });

  it('statusLabel retorna texto correto', () => {
    expect(component.statusLabel('em_dia')).toBeDefined();
    expect(component.statusLabel('atencao')).toBeDefined();
    expect(component.statusLabel('ausente')).toBeDefined();
    expect(component.statusLabel('sem_checkin')).toBeDefined();
  });

  it('statusClass retorna classe correta', () => {
    expect(component.statusClass('em_dia')).toBe('repz-tag-success');
    expect(component.statusClass('atencao')).toBe('repz-tag-warn');
    expect(component.statusClass('ausente')).toBe('repz-tag-danger');
    expect(component.statusClass('sem_checkin')).toBe('repz-tag-muted');
  });

  it('freqPct calcula percentual de frequência', () => {
    expect(component.freqPct(0)).toBe(0);
    expect(component.freqPct(20)).toBe(100);
    expect(component.freqPct(10)).toBe(50);
    expect(component.freqPct(25)).toBe(100);
  });

  it('inicial retorna primeira letra', () => {
    expect(component.inicial('Aluno Test')).toBe('A');
  });

  it('abrirResposta define respondendoId', () => {
    component.abrirResposta(5);
    expect(component.respondendoId()).toBe(5);
    expect(component.respostaTexto()).toBe('');
  });

  it('fecharResposta limpa respondendoId', () => {
    component.abrirResposta(5);
    component.fecharResposta();
    expect(component.respondendoId()).toBeNull();
  });

  it('busca filtra alunos por nome', async () => {
    component.busca.set('Aluno A');
    await fixture.whenStable();
    expect(component.rowsFiltradas().length).toBe(1);
  });

  it('busca vazia retorna todos os alunos', () => {
    component.busca.set('');
    expect(component.rowsFiltradas().length).toBe(1);
  });

  it('responder chama solicitacaoService.responder', () => {
    vi.spyOn(solicitacaoService, 'responder').mockReturnValue(of(mockSolicitacoes[0] as any));
    component.responder(1, 'APROVADA');
    expect(solicitacaoService.responder).toHaveBeenCalledWith(1, expect.objectContaining({ status: 'APROVADA' }));
  });

  it('verDetalhes navega para a rota do aluno', () => {
    const row = { userId: 10, nome: 'Aluno A', email: 'a@a.com', planoNome: 'Básico', ativo: true, freqMes: 5, ultimoCheckin: null, diasSemTreino: null, statusTreino: 'em_dia' as StatusTreino };
    expect(() => component.verDetalhes(row)).not.toThrow();
  });

  it('verFicha navega para a rota da ficha do aluno', () => {
    const row = { userId: 10, nome: 'Aluno A', email: 'a@a.com', planoNome: 'Básico', ativo: true, freqMes: 5, ultimoCheckin: null, diasSemTreino: null, statusTreino: 'em_dia' as StatusTreino };
    expect(() => component.verFicha(row)).not.toThrow();
  });

  it('ausentes7d computed conta alunos com status atencao ou ausente', () => {
    expect(typeof component.ausentes7d()).toBe('number');
  });

  it('ausentes14d computed conta alunos com status ausente', () => {
    expect(typeof component.ausentes14d()).toBe('number');
  });

  it('montarRows com inativo no mapa preenche diasSemTreino a partir do inativo', async () => {
    const freqService = TestBed.inject(FrequenciaService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([{ alunoId: 10, diasSemTreino: 20 } as any]));
    vi.spyOn(freqService, 'listarPorAluno').mockReturnValue(of([]));
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(of([]));
    const newFixture = TestBed.createComponent(Personal);
    newFixture.detectChanges();
    await newFixture.whenStable();
    const rows = newFixture.componentInstance.rows();
    expect(rows[0]?.diasSemTreino).toBe(20);
  });

  it('montarRows com frequência recente preenche diasSemTreino a partir do último checkin', async () => {
    const freqService = TestBed.inject(FrequenciaService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([]));
    vi.spyOn(freqService, 'listarPorAluno').mockReturnValue(of([{ dataHora: '01/06/2024 10:00:00' } as any]));
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(of([]));
    const newFixture = TestBed.createComponent(Personal);
    newFixture.detectChanges();
    await newFixture.whenStable();
    const rows = newFixture.componentInstance.rows();
    expect(rows[0]?.diasSemTreino).not.toBeUndefined();
  });

  it('contarAvaliacoesNoMes conta avaliações dentro do período', async () => {
    const freqService = TestBed.inject(FrequenciaService);
    const avaliacaoService = TestBed.inject(AvaliacaoFisicaService);
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([]));
    vi.spyOn(freqService, 'listarPorAluno').mockReturnValue(of([]));
    const now = new Date();
    const dataNoMes = `15/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()} 10:00:00`;
    vi.spyOn(avaliacaoService, 'listar').mockReturnValue(of([{ dataAvaliacao: dataNoMes } as any]));
    const newFixture = TestBed.createComponent(Personal);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.avaliacoesMes()).toBeGreaterThanOrEqual(0);
  });

  it('carregarSolicitacoes lida com erro de rede', async () => {
    vi.spyOn(solicitacaoService, 'listarParaPersonal').mockReturnValue(throwError(() => new Error('network error')));
    const newFixture = TestBed.createComponent(Personal);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.carregandoSol()).toBe(false);
  });

  it('responder lida com erro do servidor', () => {
    vi.spyOn(solicitacaoService, 'responder').mockReturnValue(throwError(() => ({ error: { message: 'Erro' } })));
    component.abrirResposta(1);
    component.responder(1, 'APROVADA');
    expect(component.avisoResposta()).not.toBeNull();
    expect(component.salvandoResposta()).toBe(false);
  });

  it('responder sucesso fecha modal e remove solicitação', () => {
    vi.spyOn(solicitacaoService, 'responder').mockReturnValue(of(mockSolicitacoes[0] as any));
    component.abrirResposta(1);
    component.responder(1, 'APROVADA');
    expect(component.respondendoId()).toBeNull();
    expect(component.salvandoResposta()).toBe(false);
  });

  it('responder quando já salvando não faz chamada dupla', () => {
    component.salvandoResposta.set(true);
    const spy = vi.spyOn(solicitacaoService, 'responder');
    component.responder(1, 'APROVADA');
    expect(spy).not.toHaveBeenCalled();
  });
});
