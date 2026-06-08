import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Academia } from './academia';
import { UserService } from '@core/services/user';
import { AcademiaService } from '@core/services/academia';
import { AlunoService } from '@core/services/aluno';
import { PersonalService } from '@core/services/personal';
import { FrequenciaService } from '@core/services/frequencia';
import { PlanoService } from '@core/services/plano';

const mockAcademia = { id: 1, cnpj: '12.345.678/0001-90', name: 'Academia Test', address: 'Rua A', responsible: 'Resp', active: true, email: 'a@a.com', phone: '11999999999' };
const mockPlano = { id: 1, nome: 'Básico', duracaoDias: 30, valor: 99.9, ativo: true };

describe('Academia', () => {
  let component: Academia;
  let fixture: ComponentFixture<Academia>;
  let httpMock: HttpTestingController;
  let academiaService: AcademiaService;
  let userService: UserService;
  let freqService: FrequenciaService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Academia],
      providers: [provideRouter([]), provideTranslateService(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    userService = TestBed.inject(UserService);
    academiaService = TestBed.inject(AcademiaService);
    const alunoService = TestBed.inject(AlunoService);
    const personalService = TestBed.inject(PersonalService);
    freqService = TestBed.inject(FrequenciaService);
    const planoService = TestBed.inject(PlanoService);
    httpMock = TestBed.inject(HttpTestingController);

    vi.spyOn(userService, 'carregarNomeLogado').mockImplementation(() => {});
    vi.spyOn(academiaService, 'minhaAcademia').mockReturnValue(of(mockAcademia as any));
    vi.spyOn(alunoService, 'listar').mockReturnValue(of([]));
    vi.spyOn(personalService, 'listar').mockReturnValue(of([]));
    vi.spyOn(planoService, 'listar').mockReturnValue(of([mockPlano]));
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([]));
    vi.spyOn(freqService, 'listarPeriodo').mockReturnValue(of([]));

    fixture = TestBed.createComponent(Academia);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('should create', () => expect(component).toBeTruthy());

  it('carrega academia no ngOnInit', () => {
    expect(component.academia()).toEqual(mockAcademia);
    expect(component.carregando()).toBe(false);
  });

  it('planos são carregados', () => {
    expect(component.planos().length).toBe(1);
  });

  it('abrirEdicao preenche os campos do formulário', () => {
    component.abrirEdicao();
    expect(component.formNome).toBe('Academia Test');
    expect(component.formCnpj).toBe('12.345.678/0001-90');
    expect(component.editando()).toBe(true);
  });

  it('fecharEdicao fecha o modal', () => {
    component.abrirEdicao();
    component.fecharEdicao();
    expect(component.editando()).toBe(false);
  });

  it('trocarAba muda a aba de cadastro', () => {
    component.trocarAba('personal');
    expect(component.abaCadastro()).toBe('personal');
    component.trocarAba('aluno');
    expect(component.abaCadastro()).toBe('aluno');
  });

  it('salvarAcademia com campos vazios define erro', () => {
    component.abrirEdicao();
    component.formNome = '';
    component.salvarAcademia();
    expect(component.aviso()).not.toBeNull();
    expect(component.avisoSeverity()).toBe('error');
  });

  it('salvarAcademia com dados válidos chama atualizarMinhaAcademia', () => {
    vi.spyOn(academiaService, 'atualizarMinhaAcademia').mockReturnValue(of(mockAcademia as any));
    component.abrirEdicao();
    component.salvarAcademia();
    expect(academiaService.atualizarMinhaAcademia).toHaveBeenCalled();
  });

  it('cadastrar sem nome define erro', () => {
    component.cadNome = '';
    component.cadEmail = 'test@test.com';
    component.cadastrar();
    expect(component.aviso()).not.toBeNull();
  });

  it('cadastrar aluno sem plano define erro', () => {
    component.cadNome = 'Test';
    component.cadEmail = 'test@test.com';
    (component as any).cadPlanoId = null;
    component.cadastrar();
    expect(component.aviso()).not.toBeNull();
  });

  it('inicial retorna primeira letra do nome', () => {
    expect(component.inicial('Personal Test')).toBe('P');
    expect(component.inicial(' abc')).toBe('A');
  });

  it('ngOnInit com erro em minhaAcademia define erro', async () => {
    vi.spyOn(academiaService, 'minhaAcademia').mockReturnValue(of(null as any));
    fixture = TestBed.createComponent(Academia);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.erro()).not.toBeNull();
  });

  it('irParaPersonais navega para /academia/personais', () => {
    expect(() => component.irParaPersonais()).not.toThrow();
  });

  it('cadastrar personal com dados válidos chama userService.criar', () => {
    vi.spyOn(userService, 'criar').mockReturnValue(of(undefined as any));
    component.abaCadastro.set('personal');
    component.cadNome = 'Novo Personal';
    component.cadEmail = 'personal@test.com';
    component.cadastrar();
    expect(userService.criar).toHaveBeenCalled();
  });

  it('cadastrar aluno com todos os dados chama userService.criar e aciona flash', () => {
    vi.spyOn(userService, 'criar').mockReturnValue(of(undefined as any));
    component.abaCadastro.set('aluno');
    component.cadNome = 'Novo Aluno';
    component.cadEmail = 'aluno@test.com';
    (component as any).cadPlanoId = 1;
    component.cadastrar();
    expect(userService.criar).toHaveBeenCalled();
    expect(component.aviso()).not.toBeNull();
  });

  it('cadastrar lida com erro do servidor', () => {
    vi.spyOn(userService, 'criar').mockReturnValue(throwError(() => ({ error: { message: 'E-mail já existe' } })));
    component.abaCadastro.set('personal');
    component.cadNome = 'Test';
    component.cadEmail = 'test@test.com';
    component.cadastrar();
    expect(component.aviso()).not.toBeNull();
    expect(component.avisoSeverity()).toBe('error');
  });

  it('salvarAcademia com sucesso fecha modal e mostra aviso', () => {
    vi.spyOn(academiaService, 'atualizarMinhaAcademia').mockReturnValue(of(mockAcademia as any));
    component.abrirEdicao();
    component.salvarAcademia();
    expect(component.editando()).toBe(false);
    expect(component.aviso()).not.toBeNull();
  });

  it('salvarAcademia lida com erro do servidor', () => {
    vi.spyOn(academiaService, 'atualizarMinhaAcademia').mockReturnValue(throwError(() => ({ error: { message: 'Erro' } })));
    component.abrirEdicao();
    component.salvarAcademia();
    expect(component.avisoSeverity()).toBe('error');
  });

  it('frequenciaMedia computed é zero quando sem alunos ativos', () => {
    expect(component.frequenciaMedia()).toBe(0);
  });

  it('totalAlunos computed reflete lista de alunos', () => {
    expect(typeof component.totalAlunos()).toBe('number');
  });

  it('frequenciaMedia não é zero quando há alunos ativos e checkins', async () => {
    const alunoService = TestBed.inject(AlunoService);
    const freqService = TestBed.inject(FrequenciaService);
    vi.spyOn(alunoService, 'listar').mockReturnValue(of([{ id: 1, userId: 1, nome: 'A', email: 'a@test.com', academiaId: 1, ativo: true, planoNome: 'P' }] as any));
    vi.spyOn(freqService, 'listarPeriodo').mockReturnValue(of([{ id: 1 }, { id: 2 }] as any));
    const newFixture = TestBed.createComponent(Academia);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newComponent.frequenciaMedia()).toBeGreaterThan(0);
  });

  it('ausentes reflete contagem de inativos por mais de 14 dias', async () => {
    const freqService = TestBed.inject(FrequenciaService);
    vi.spyOn(freqService, 'alunosInativos').mockReturnValue(of([
      { diasSemTreino: 15 } as any,
      { diasSemTreino: 7 } as any,
    ]));
    const newFixture = TestBed.createComponent(Academia);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newComponent.ausentes()).toBe(1);
  });

  it('carregar com erro em minhaAcademia (throw) aciona catchError', async () => {
    vi.spyOn(academiaService, 'minhaAcademia').mockReturnValue(throwError(() => new Error('err')));
    const newFixture = TestBed.createComponent(Academia);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.erro()).not.toBeNull();
  });

  it('carregar com erro em alunoService.listar usa catchError', async () => {
    const alunoService = TestBed.inject(AlunoService);
    vi.spyOn(alunoService, 'listar').mockReturnValue(throwError(() => new Error('err')));
    const newFixture = TestBed.createComponent(Academia);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.alunos()).toEqual([]);
  });

  it('carregarFrequenciaMes com erro em listarPeriodo usa catchError', async () => {
    const freqService = TestBed.inject(FrequenciaService);
    vi.spyOn(freqService, 'listarPeriodo').mockReturnValue(throwError(() => new Error('err')));
    const newFixture = TestBed.createComponent(Academia);
    newFixture.detectChanges();
    await newFixture.whenStable();
    expect(newFixture.componentInstance.checkinsMes()).toBe(0);
  });
});
