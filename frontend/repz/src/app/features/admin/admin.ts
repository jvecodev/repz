import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AcademiaService, UserService } from '@core/services';
import type { AcademiaDashboardResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    AppShell,
    ButtonModule,
    CardModule,
    MessageModule,
    ProgressSpinnerModule,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly academiaService = inject(AcademiaService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly dashboard = signal<AcademiaDashboardResponse | null>(null);

  ngOnInit(): void {
    this.userService.carregarNomeLogado();

    this.academiaService.dashboard().subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar os indicadores.');
      },
    });
  }

  mediaAlunos(): string {
    const m = this.dashboard()?.averageStudentsPerAcademy ?? 0;
    return m.toFixed(1);
  }
}
