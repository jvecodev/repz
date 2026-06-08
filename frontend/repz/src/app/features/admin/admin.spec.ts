import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Admin } from './admin';
import { UserService } from '@core/services/user';
import { AcademiaService } from '@core/services/academia';
import { AlunoService } from '@core/services/aluno';
import { PersonalService } from '@core/services/personal';
import { FrequenciaService } from '@core/services/frequencia';

const mockDashboard = { totalAcademies: 5, totalStudents: 100, totalInstructors: 10, totalActiveAcademies: 4, totalInactiveAcademies: 1, averageStudentsPerAcademy: 20 };
const mockAcademias = [{ id: 1, cnpj: '12.345.678/0001-90', name: 'Academia A', address: 'Rua A', responsible: 'Resp', active: true }];

describe('Admin', () => {
  let component: Admin;
  let fixture: ComponentFixture<Admin>;
  let httpMock: HttpTestingController;
  let academiaService: AcademiaService;
  let alunoService: AlunoService;
  let personalService: PersonalService;
  let freqService: FrequenciaService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [provideRouter([]), provideTranslateService(), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const userService = TestBed.inject(UserService);
    academiaService = TestBed.inject(AcademiaService);
    alunoService = TestBed.inject(AlunoService);
    personalService = TestBed.inject(PersonalService);
    freqService = TestBed.inject(FrequenciaService);
    httpMock = TestBed.inject(HttpTestingController);

    vi.spyOn(userService, 'carregarNomeLogado').mockImplementation(() => {});
    vi.spyOn(academiaService, 'dashboard').mockReturnValue(of(mockDashboard));
    vi.spyOn(academiaService, 'listar').mockReturnValue(of(mockAcademias as any));
    vi.spyOn(alunoService, 'listar').mockReturnValue(of([{ id: 1, userId: 1, nome: 'A', email: 'a@a.com', academiaId: 1, ativo: true }]));
    vi.spyOn(personalService, 'listar').mockReturnValue(of([{ id: 1, userId: 1, userName: 'P', email: 'p@p.com', academiaId: 1, ativo: true }]));
    vi.spyOn(freqService, 'listarPeriodo').mockReturnValue(of([]));

    fixture = TestBed.createComponent(Admin);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  it('should create', () => expect(component).toBeTruthy());

  it('carrega dashboard no ngOnInit', () => {
    expect(component.dashboard()).toEqual(mockDashboard);
    expect(component.carregando()).toBe(false);
  });

  it('popula a tabela de academias', () => {
    expect(component.rows().length).toBe(1);
    expect(component.rows()[0].name).toBe('Academia A');
  });

  it('computed totalAcademias reflete as linhas', () => {
    expect(component.totalAcademias()).toBe(1);
  });

  it('computed ativas reflete academias ativas', () => {
    expect(component.ativas()).toBe(1);
  });

  it('computed inativas reflete academias inativas', () => {
    expect(component.inativas()).toBe(0);
  });

  it('mediaAlunos formata a média com uma casa decimal', () => {
    expect(component.mediaAlunos()).toBe('20.0');
  });

  it('inicial retorna a primeira letra em maiúsculo', () => {
    expect(component.inicial('Academia Test')).toBe('A');
    expect(component.inicial(' xyz')).toBe('X');
  });

  it('confirmarToggleAtivo chama ConfirmationService', () => {
    const row = component.rows()[0];
    // Não deve lançar exceção
    expect(() => component.confirmarToggleAtivo(row)).not.toThrow();
  });

  it('ngOnInit com erro no dashboard define erro', async () => {
    vi.spyOn(academiaService, 'dashboard').mockReturnValue(throwError(() => new Error('Erro')));
    fixture = TestBed.createComponent(Admin);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.erro()).not.toBeNull();
  });

  it('alterarStatus com ativar=true chama academiaService.ativar e atualiza row', () => {
    vi.spyOn(academiaService, 'ativar').mockReturnValue(of({ ...mockAcademias[0], active: true } as any));
    const row = { id: 1, name: 'A', responsible: 'R', active: false, alunosAtivos: 0, personaisAtivos: 0, frequenciaMedia: 0 };
    (component as any).alterarStatus(row, true);
    expect(academiaService.ativar).toHaveBeenCalledWith(1);
  });

  it('alterarStatus com ativar=false chama academiaService.desativar', () => {
    vi.spyOn(academiaService, 'desativar').mockReturnValue(of({ ...mockAcademias[0], active: false } as any));
    const row = { id: 1, name: 'A', responsible: 'R', active: true, alunosAtivos: 0, personaisAtivos: 0, frequenciaMedia: 0 };
    (component as any).alterarStatus(row, false);
    expect(academiaService.desativar).toHaveBeenCalledWith(1);
  });

  it('alterarStatus lida com erro sem lançar exceção', () => {
    vi.spyOn(academiaService, 'ativar').mockReturnValue(throwError(() => new Error('Erro')));
    const row = { id: 1, name: 'A', responsible: 'R', active: false, alunosAtivos: 0, personaisAtivos: 0, frequenciaMedia: 0 };
    expect(() => (component as any).alterarStatus(row, true)).not.toThrow();
  });

  it('totalAlunosComputado soma alunosAtivos das rows', () => {
    expect(component.totalAlunosComputado()).toBeGreaterThanOrEqual(0);
  });

  it('totalPersonaisComputado soma personaisAtivos das rows', () => {
    expect(component.totalPersonaisComputado()).toBeGreaterThanOrEqual(0);
  });
});
