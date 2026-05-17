import { AvaliacaoFisicaResponse } from '@core/services';

export type Metrica = 'peso' | 'imc' | 'gordura';

export interface AvaliacaoVM {
  id: number;
  data: string; // dd/MM/yyyy
  peso?: number;
  altura?: number;
  imc?: number;
  gordura?: number;
  cintura?: number;
  quadril?: number;
  braco?: number;
  coxa?: number;
  deltaPeso?: number; // variação vs avaliação anterior (cronológica)
  personal?: string;
}

/**
 * Normaliza para dd/MM/yyyy. Trata os dois formatos do backend:
 *  - lista:   "17/05/2026 18:21:22"  (@JsonFormat dd/MM/yyyy HH:mm:ss)
 *  - gráfico: "2026-02-22T18:21:22"  (LocalDateTime ISO)
 */
export function formatarData(valor?: string | null): string {
  if (!valor) return '—';
  const soData = valor.split('T')[0].split(' ')[0];
  if (soData.includes('/')) return soData;
  if (soData.includes('-')) {
    const [a, m, d] = soData.split('-');
    return `${d}/${m}/${a}`;
  }
  return soData;
}

function chave(valor?: string | null): number {
  // "dd/MM/yyyy HH:mm:ss" -> timestamp comparável
  if (!valor) return 0;
  const [data, hora = '00:00:00'] = valor.split(' ');
  const [d, m, a] = data.split('/').map(Number);
  const [h, min, s] = hora.split(':').map(Number);
  return new Date(a, (m ?? 1) - 1, d ?? 1, h ?? 0, min ?? 0, s ?? 0).getTime();
}

/**
 * Histórico em ordem cronológica decrescente (mais recente primeiro),
 * com a variação de peso em relação à avaliação imediatamente anterior.
 */
export function mapearHistorico(itens: AvaliacaoFisicaResponse[]): AvaliacaoVM[] {
  const asc = [...(itens ?? [])].sort((a, b) => chave(a.dataAvaliacao) - chave(b.dataAvaliacao));

  const vms: AvaliacaoVM[] = asc.map((a, i) => {
    const anterior = asc[i - 1];
    const deltaPeso =
      a.pesoKg != null && anterior?.pesoKg != null
        ? Number((a.pesoKg - anterior.pesoKg).toFixed(1))
        : undefined;
    return {
      id: a.id,
      data: formatarData(a.dataAvaliacao),
      peso: a.pesoKg,
      altura: a.alturaCm,
      imc: a.imc != null ? Number(a.imc.toFixed(1)) : undefined,
      gordura: a.percentualGordura,
      cintura: a.cinturaCm,
      quadril: a.quadrilCm,
      braco: a.bracoCm,
      coxa: a.coxaCm,
      deltaPeso,
      personal: a.personalNome,
    };
  });

  return vms.reverse(); // mais recente primeiro
}
