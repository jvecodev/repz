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
    path: 'academia',
    loadComponent: () => import('./features/academia/academia').then((m) => m.Academia),
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
    path: 'personal/aluno/:id/ficha-treino',
    loadComponent: () =>
      import('./features/aluno/ficha-treino/ficha-treino').then((m) => m.FichaTreino),
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

  { path: '**', redirectTo: 'auth' },
];
