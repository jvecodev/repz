import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { PersonalService, UserService } from '@core/services';
import type { PersonalResponse } from '@core/services';
import { AppShell } from '@shared/layout';
import { AvatarUpload } from '@shared/avatar-upload';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-personal-perfil',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    AvatarUpload,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
  ],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class PersonalPerfil implements OnInit {
  private readonly personalService = inject(PersonalService);
  private readonly userService = inject(UserService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly perfil = signal<PersonalResponse | null>(null);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');

  nome = '';
  email = '';
  especialidade = '';
  novaSenha = '';
  confirmarSenha = '';

  ngOnInit(): void {
    this.personalService.meuPerfil().subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.nome = p.userName;
        this.email = p.email;
        this.especialidade = p.especialidade ?? '';
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set('Não foi possível carregar o perfil.');
      },
    });
  }

  salvar(): void {
    if (this.salvando()) return;
    if (!this.nome.trim() || !this.email.trim() || !this.especialidade.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set('Nome, e-mail e especialidade são obrigatórios.');
      return;
    }
    if (this.novaSenha && this.novaSenha.length < 5) {
      this.avisoSeverity.set('error');
      this.aviso.set('A nova senha deve ter ao menos 5 caracteres.');
      return;
    }
    if (this.novaSenha !== this.confirmarSenha) {
      this.avisoSeverity.set('error');
      this.aviso.set('As senhas não coincidem.');
      return;
    }

    this.aviso.set(null);
    this.salvando.set(true);

    forkJoin([
      this.userService.atualizarMeuPerfil({
        name: this.nome.trim(),
        email: this.email.trim(),
        senha: this.novaSenha.trim() || undefined,
      }),
      this.personalService.atualizarMeuPerfil({ especialidade: this.especialidade.trim() }),
    ]).subscribe({
      next: ([, personal]) => {
        this.perfil.set({
          ...personal,
          userName: this.nome.trim(),
          email: this.email.trim(),
        });
        this.novaSenha = '';
        this.confirmarSenha = '';
        this.salvando.set(false);
        this.avisoSeverity.set('success');
        this.aviso.set('Perfil atualizado com sucesso!');
      },
      error: (err) => {
        this.salvando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Erro ao salvar perfil.');
      },
    });
  }
}
