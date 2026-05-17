import { AvaliacaoFisicaResponse, DadoGrafico } from '@core/services';

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

export interface PontoGrafico {
  x: number;
  y: number;
  rotulo: string;
  valor: number;
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
  const asc = [...(itens ?? [])].sort(
    (a, b) => chave(a.dataAvaliacao) - chave(b.dataAvaliacao),
  );

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

const VALOR: Record<Metrica, (d: DadoGrafico) => number | undefined> = {
  peso: (d) => d.peso,
  imc: (d) => d.imc,
  gordura: (d) => d.percentualGordura,
};

/**
 * Converte os pontos do gráfico em coordenadas SVG (viewBox 0 0 100 100),
 * para a métrica escolhida. Retorna [] se houver menos de 1 ponto.
 */
export function pontosGrafico(
  dados: DadoGrafico[],
  metrica: Metrica,
): PontoGrafico[] {
  const validos = (dados ?? [])
    .map((d) => ({ data: d.data, valor: VALOR[metrica](d) }))
    .filter((d): d is { data: string; valor: number } => d.valor != null);

  if (validos.length === 0) return [];

  const valores = validos.map((d) => d.valor);
  const min = Math.min(...valores);
  const max = Math.max(...valores);
  const span = max - min || 1;
  const n = validos.length;

  return validos.map((d, i) => ({
    x: n === 1 ? 50 : (i / (n - 1)) * 100,
    y: 100 - ((d.valor - min) / span) * 90 - 5, // margem de 5%
    rotulo: formatarData(d.data),
    valor: d.valor,
  }));
}

export function linhaPolyline(pontos: PontoGrafico[]): string {
  return pontos.map((p) => `${p.x},${p.y}`).join(' ');
}
