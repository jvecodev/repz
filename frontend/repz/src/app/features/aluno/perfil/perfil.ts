import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AlunoDetalheResponse, AlunoService, AuthService } from '@core/services';
import { AppShell } from '@shared/layout';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [
    CommonModule,
    AppShell,
    CardModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly service = inject(AlunoService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aluno = signal<AlunoDetalheResponse | null>(null);

  readonly emailSessao = computed(() => this.auth.sessao()?.email ?? '—');

  ngOnInit(): void {
    const id = this.auth.sessao()?.id;
    if (!id) {
      this.carregando.set(false);
      this.erro.set('Não foi possível identificar sua sessão.');
      return;
    }
    // /api/alunos/me retorna o próprio perfil do aluno logado
    this.service.buscar(id).subscribe({
      next: (a) => {
        this.aluno.set(a);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar seu perfil.');
      },
    });
  }
}
