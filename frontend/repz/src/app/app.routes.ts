import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin-guard';
import { academiaGuard } from './core/guards/academia-guard';
import { alunoGuard } from './core/guards/aluno-guard';
import { personalGuard } from './core/guards/personal-guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },

  {
    path: 'auth',
    loadComponent: () => import('./features/auth/auth').then((m) => m.Auth),
  },

  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin').then((m) => m.Admin),
    canActivate: [adminGuard],
  },

  {
    path: 'admin/academias',
    loadComponent: () =>
      import('./features/admin/academias/academias').then((m) => m.AdminAcademias),
    canActivate: [adminGuard],
  },

  {
    path: 'admin/usuarios',
    loadComponent: () =>
      import('./features/admin/usuarios/usuarios').then((m) => m.AdminUsuarios),
    canActivate: [adminGuard],
  },

  {
    path: 'admin/perfil',
    loadComponent: () =>
      import('./features/admin/perfil/perfil').then((m) => m.AdminPerfil),
    canActivate: [adminGuard],
  },

  {
    path: 'academia',
    loadComponent: () => import('./features/academia/academia').then((m) => m.Academia),
    canActivate: [academiaGuard],
  },

  {
    path: 'academia/personais',
    loadComponent: () =>
      import('./features/academia/personais/personais').then((m) => m.AcademiaPersonais),
    canActivate: [academiaGuard],
  },

  {
    path: 'academia/alunos',
    loadComponent: () =>
      import('./features/academia/alunos/alunos').then((m) => m.AcademiaAlunos),
    canActivate: [academiaGuard],
  },

  {
    path: 'academia/perfil',
    loadComponent: () =>
      import('./features/academia/perfil/perfil').then((m) => m.AcademiaPerfil),
    canActivate: [academiaGuard],
  },

  {
    path: 'academia/planos',
    loadComponent: () =>
      import('./features/academia/planos/planos').then((m) => m.AcademiaPlanos),
    canActivate: [academiaGuard],
  },

  {
    path: 'academia/relatorios',
    loadComponent: () =>
      import('./features/academia/relatorios/relatorios').then((m) => m.AcademiaRelatorios),
    canActivate: [academiaGuard],
  },

  {
    path: 'personal',
    loadComponent: () => import('./features/personal/personal').then((m) => m.Personal),
    canActivate: [personalGuard],
  },

  {
    path: 'aluno',
    loadComponent: () => import('./features/aluno/aluno').then((m) => m.Aluno),
    canActivate: [alunoGuard],
  },

  {
    path: 'aluno/ficha-treino',
    loadComponent: () =>
      import('./features/aluno/ficha-treino/ficha-treino').then((m) => m.FichaTreino),
    canActivate: [alunoGuard],
  },

  {
    path: 'personal/aluno/:id',
    loadComponent: () =>
      import('./features/personal/aluno-detalhes/detalhes').then(
        (m) => m.PersonalAlunoDetalhes,
      ),
    canActivate: [personalGuard],
  },

  {
    path: 'personal/aluno/:id/ficha-treino',
    loadComponent: () =>
      import('./features/personal/ficha-treino/personal-ficha-treino').then(
        (m) => m.PersonalFichaTreino,
      ),
    canActivate: [personalGuard],
  },

  {
    path: 'personal/aluno/:id/avaliacao-nova',
    loadComponent: () =>
      import('./features/personal/avaliacao-nova/avaliacao-nova').then(
        (m) => m.PersonalAvaliacaoNova,
      ),
    canActivate: [personalGuard],
  },

  {
    path: 'aluno/evolucao',
    loadComponent: () =>
      import('./features/aluno/avaliacao-fisica/avaliacao-fisica').then((m) => m.AvaliacaoFisica),
    canActivate: [alunoGuard],
  },

  {
    path: 'personal/aluno/:id/avaliacoes',
    loadComponent: () =>
      import('./features/aluno/avaliacao-fisica/avaliacao-fisica').then((m) => m.AvaliacaoFisica),
    canActivate: [personalGuard],
  },

  {
    path: 'academia/aluno/:id/avaliacoes',
    loadComponent: () =>
      import('./features/aluno/avaliacao-fisica/avaliacao-fisica').then((m) => m.AvaliacaoFisica),
    canActivate: [academiaGuard],
  },

  {
    path: 'aluno/frequencia',
    loadComponent: () =>
      import('./features/aluno/frequencia/frequencia').then((m) => m.Frequencia),
    canActivate: [alunoGuard],
  },

  {
    path: 'aluno/perfil',
    loadComponent: () =>
      import('./features/aluno/perfil/perfil').then((m) => m.Perfil),
    canActivate: [alunoGuard],
  },

  {
    path: 'personal/alunos',
    loadComponent: () =>
      import('./features/personal/alunos/alunos').then((m) => m.PersonalAlunos),
    canActivate: [personalGuard],
  },

  {
    path: 'personal/perfil',
    loadComponent: () =>
      import('./features/personal/perfil/perfil').then((m) => m.PersonalPerfil),
    canActivate: [personalGuard],
  },

  { path: '**', redirectTo: 'auth' },
];
