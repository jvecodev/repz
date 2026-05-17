import { describe, it, expect } from 'vitest';
import { AvaliacaoFisicaResponse, DadoGrafico } from '@core/services';
import {
  formatarData,
  mapearHistorico,
  pontosGrafico,
} from './avaliacao-fisica.mapper';

function av(p: Partial<AvaliacaoFisicaResponse>): AvaliacaoFisicaResponse {
  return {
    id: 1,
    alunoId: 1,
    alunoNome: 'Lucas',
    dataAvaliacao: '01/01/2026 10:00:00',
    ...p,
  } as AvaliacaoFisicaResponse;
}

describe('formatarData', () => {
  it('mantém só a data (formato da lista)', () => {
    expect(formatarData('22/04/2026 10:30:00')).toBe('22/04/2026');
    expect(formatarData(undefined)).toBe('—');
  });

  it('converte ISO do gráfico para dd/MM/yyyy', () => {
    expect(formatarData('2026-02-22T18:21:22.603216')).toBe('22/02/2026');
  });
});

describe('mapearHistorico', () => {
  it('ordena do mais recente para o mais antigo (RF29)', () => {
    const out = mapearHistorico([
      av({ id: 1, dataAvaliacao: '01/02/2026 10:00:00', pesoKg: 80 }),
      av({ id: 2, dataAvaliacao: '01/04/2026 10:00:00', pesoKg: 78 }),
      av({ id: 3, dataAvaliacao: '01/03/2026 10:00:00', pesoKg: 79 }),
    ]);
    expect(out.map((o) => o.id)).toEqual([2, 3, 1]);
  });

  it('calcula a variação de peso vs avaliação anterior cronológica', () => {
    const out = mapearHistorico([
      av({ id: 1, dataAvaliacao: '01/01/2026 10:00:00', pesoKg: 80 }),
      av({ id: 2, dataAvaliacao: '01/02/2026 10:00:00', pesoKg: 78.5 }),
    ]);
    // out[0] = mais recente (id 2): 78.5 - 80 = -1.5
    expect(out[0].deltaPeso).toBe(-1.5);
    expect(out[1].deltaPeso).toBeUndefined();
  });

  it('arredonda o IMC para 1 casa', () => {
    const out = mapearHistorico([av({ imc: 24.6789 })]);
    expect(out[0].imc).toBe(24.7);
  });
});

describe('pontosGrafico', () => {
  const dados: DadoGrafico[] = [
    { data: '01/01/2026 10:00:00', peso: 80, imc: 26, percentualGordura: 20 },
    { data: '01/02/2026 10:00:00', peso: 78, imc: 25, percentualGordura: 18 },
    { data: '01/03/2026 10:00:00', peso: 76, imc: 24, percentualGordura: 16 },
  ];

  it('gera um ponto por avaliação com x distribuído de 0 a 100', () => {
    const p = pontosGrafico(dados, 'peso');
    expect(p).toHaveLength(3);
    expect(p[0].x).toBe(0);
    expect(p[2].x).toBe(100);
  });

  it('maior valor fica mais alto no SVG (y menor)', () => {
    const p = pontosGrafico(dados, 'peso');
    // peso 80 (maior) deve ter y menor que peso 76 (menor)
    expect(p[0].y).toBeLessThan(p[2].y);
  });

  it('ignora métrica ausente e retorna [] se não houver dados', () => {
    expect(pontosGrafico([{ data: 'x', peso: 1 }], 'gordura')).toEqual([]);
    expect(pontosGrafico([], 'peso')).toEqual([]);
  });
});
