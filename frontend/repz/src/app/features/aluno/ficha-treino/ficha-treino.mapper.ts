import { TreinoResponse } from '@core/services';

// ---------------------------------------------------------------
// View models da tela de ficha de treino
// ---------------------------------------------------------------
export interface ExercicioVM {
  nome: string;
  grupo: string;
  series: number | string;
  reps: string;
  carga: string;
  descanso: string;
  obs?: string;
}

export interface DivisaoVM {
  letra: string;
  nome: string;
  foco: string;
  exercicios: ExercicioVM[];
}

export interface FichaVM {
  alunoNome: string;
  nome: string;
  objetivo: string;
  criadaEm: string;
  atualizadaEm: string;
  validadeAte: string;
  ativa: boolean;
  personal: string;
  personalId: number | null;
  observacoes: string;
  letras: string[];
  treinos: Record<string, DivisaoVM>;
}

export interface HistoricoVM {
  nome: string;
  objetivo: string;
  criadaEm: string;
  encerradaEm: string;
}

// ---------------------------------------------------------------
// Funções puras de mapeamento (sem Angular — fáceis de testar)
// ---------------------------------------------------------------

/** Backend já devolve datas formatadas (dd/MM/yyyy [HH:mm:ss]); só normaliza. */
export function formatarData(valor?: string | null): string {
  if (!valor) return '—';
  const semHora = valor.split(' ')[0];
  if (semHora.includes('/')) return semHora;
  if (semHora.includes('-')) {
    const [a, m, d] = semHora.split('-');
    return `${d}/${m}/${a}`;
  }
  return semHora;
}

/** Extrai o "foco" do nome da divisão ("Treino A — Peito" -> "Peito"). */
export function extrairFoco(nome: string | undefined, letra: string): string {
  if (!nome) return `Divisão ${letra}`;
  const partes = nome.split(/[—-]/);
  return partes.length > 1 ? partes[partes.length - 1].trim() : nome.trim();
}

/** Lista de divisões ativas -> FichaVM agregada (ou null se vazia). */
export function mapearFichaAtiva(treinos: TreinoResponse[]): FichaVM | null {
  if (!treinos || treinos.length === 0) return null;

  const ordenados = [...treinos].sort((a, b) => (a.divisao ?? '').localeCompare(b.divisao ?? ''));
  const primeiro = ordenados[0];

  const treinosMap: Record<string, DivisaoVM> = {};
  const letras: string[] = [];

  for (const t of ordenados) {
    const letra = (t.divisao ?? '?').trim().toUpperCase();
    letras.push(letra);
    treinosMap[letra] = {
      letra,
      nome: t.nome ?? `Treino ${letra}`,
      foco: extrairFoco(t.nome, letra),
      exercicios: (t.exercicios ?? []).map((e) => ({
        nome: e.nomeExercicio,
        grupo: e.grupoMuscular ?? '—',
        series: e.series ?? '—',
        reps: e.repeticoes ?? '—',
        carga: e.cargaKg != null ? `${e.cargaKg} kg` : '—',
        descanso: e.descansoSegundos != null ? `${e.descansoSegundos}s` : '—',
        obs: e.observacao || undefined,
      })),
    };
  }

  return {
    alunoNome: primeiro.alunoNome ?? 'Aluno',
    nome: primeiro.objetivo || 'Ficha de treino',
    objetivo: primeiro.objetivo || '',
    criadaEm: formatarData(primeiro.dataInclusao),
    atualizadaEm: formatarData(primeiro.dataAlteracao),
    validadeAte: formatarData(primeiro.validadeAte),
    ativa: ordenados.some((t) => t.ativo),
    personal: primeiro.personalNome ?? '—',
    personalId: primeiro.personalId ?? null,
    observacoes: primeiro.observacoes || 'Sem observações registradas.',
    letras,
    treinos: treinosMap,
  };
}

/** Lista de divisões inativas -> linhas do histórico. */
export function mapearHistorico(treinos: TreinoResponse[]): HistoricoVM[] {
  return (treinos ?? []).map((t) => ({
    nome: t.nome,
    objetivo: t.objetivo || '—',
    criadaEm: formatarData(t.dataInclusao),
    encerradaEm: formatarData(t.dataAlteracao),
  }));
}
