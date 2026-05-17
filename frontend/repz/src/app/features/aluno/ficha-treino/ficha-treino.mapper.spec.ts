import { describe, it, expect } from 'vitest';
import { TreinoResponse } from '@core/services';
import {
  extrairFoco,
  formatarData,
  mapearFichaAtiva,
  mapearHistorico,
} from './ficha-treino.mapper';

function treino(parcial: Partial<TreinoResponse>): TreinoResponse {
  return {
    id: 1,
    nome: 'Treino A — Peito',
    divisao: 'A',
    ativo: true,
    exercicios: [],
    ...parcial,
  } as TreinoResponse;
}

describe('formatarData', () => {
  it('retorna travessão quando vazio', () => {
    expect(formatarData(undefined)).toBe('—');
    expect(formatarData(null)).toBe('—');
  });

  it('remove a hora e mantém dd/MM/yyyy', () => {
    expect(formatarData('22/04/2026 10:30:00')).toBe('22/04/2026');
  });

  it('converte ISO yyyy-MM-dd para dd/MM/yyyy', () => {
    expect(formatarData('2026-07-22')).toBe('22/07/2026');
  });
});

describe('extrairFoco', () => {
  it('pega o texto após o travessão', () => {
    expect(extrairFoco('Treino A — Peito e Tríceps', 'A')).toBe('Peito e Tríceps');
  });

  it('usa fallback quando não há nome', () => {
    expect(extrairFoco(undefined, 'B')).toBe('Divisão B');
  });
});

describe('mapearFichaAtiva', () => {
  it('retorna null para lista vazia', () => {
    expect(mapearFichaAtiva([])).toBeNull();
  });

  it('agrega divisões ordenadas e formata exercícios', () => {
    const ficha = mapearFichaAtiva([
      treino({
        divisao: 'B',
        nome: 'Treino B — Costas',
        objetivo: 'Hipertrofia',
        personalNome: 'Marina',
        exercicios: [],
      }),
      treino({
        divisao: 'A',
        nome: 'Treino A — Peito',
        objetivo: 'Hipertrofia',
        personalNome: 'Marina',
        exercicios: [
          {
            id: 1,
            nomeExercicio: 'Supino',
            grupoMuscular: 'Peito',
            series: 4,
            repeticoes: '8-10',
            cargaKg: 70,
            descansoSegundos: 90,
            observacao: 'Controlar a descida',
          },
        ],
      }),
    ]);

    expect(ficha).not.toBeNull();
    expect(ficha!.letras).toEqual(['A', 'B']);
    expect(ficha!.nome).toBe('Hipertrofia');
    expect(ficha!.personal).toBe('Marina');
    const ex = ficha!.treinos['A'].exercicios[0];
    expect(ex.carga).toBe('70 kg');
    expect(ex.descanso).toBe('90s');
    expect(ex.obs).toBe('Controlar a descida');
  });

  it('usa placeholders quando faltam dados do exercício', () => {
    const ficha = mapearFichaAtiva([
      treino({
        exercicios: [{ id: 9, nomeExercicio: 'Prancha' }],
      }),
    ]);
    const ex = ficha!.treinos['A'].exercicios[0];
    expect(ex.grupo).toBe('—');
    expect(ex.carga).toBe('—');
    expect(ex.descanso).toBe('—');
    expect(ex.obs).toBeUndefined();
  });
});

describe('mapearHistorico', () => {
  it('mapeia datas e objetivo', () => {
    const hist = mapearHistorico([
      treino({
        nome: 'Adaptação',
        objetivo: '',
        dataInclusao: '01/12/2025 09:00:00',
        dataAlteracao: '13/04/2026 18:00:00',
      }),
    ]);
    expect(hist).toEqual([
      {
        nome: 'Adaptação',
        objetivo: '—',
        criadaEm: '01/12/2025',
        encerradaEm: '13/04/2026',
      },
    ]);
  });
});
