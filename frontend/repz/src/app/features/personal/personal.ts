import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, SolicitacaoFichaService } from '@core/services';
import type { SolicitacaoFichaResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';

@Component({
  selector: 'app-personal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    ButtonModule,
    CardModule,
    MessageModule,
    SkeletonModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './personal.html',
  styleUrl: './personal.scss',
})
export class Personal implements OnInit {
  private readonly solicitacaoService = inject(SolicitacaoFichaService);

  readonly solicitacoes = signal<SolicitacaoFichaResponse[]>([]);
  readonly carregandoSol = signal(true);

  // Estado de resposta por solicitação
  readonly respondendoId = signal<number | null>(null);
  readonly respostaTexto = signal('');
  readonly salvandoResposta = signal(false);
  readonly avisoResposta = signal<string | null>(null);

  ngOnInit(): void {
    this.solicitacaoService.listarParaPersonal('PENDENTE').subscribe({
      next: (lista) => {
        this.solicitacoes.set(lista);
        this.carregandoSol.set(false);
      },
      error: () => this.carregandoSol.set(false),
    });
  }

  abrirResposta(id: number): void {
    this.respondendoId.set(id);
    this.respostaTexto.set('');
    this.avisoResposta.set(null);
  }

  fecharResposta(): void {
    this.respondendoId.set(null);
  }

  responder(id: number, status: 'APROVADA' | 'REJEITADA'): void {
    if (this.salvandoResposta()) return;
    this.avisoResposta.set(null);
    this.salvandoResposta.set(true);

    this.solicitacaoService
      .responder(id, { status, resposta: this.respostaTexto().trim() || undefined })
      .subscribe({
        next: () => {
          this.salvandoResposta.set(false);
          this.respondendoId.set(null);
          // Remove da lista local
          this.solicitacoes.update((lista) => lista.filter((s) => s.id !== id));
        },
        error: (err) => {
          this.salvandoResposta.set(false);
          this.avisoResposta.set(err?.error?.message ?? 'Erro ao responder.');
        },
      });
  }
}
