import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '@core/services';
import type { UserGetResponse, UserSelfUpdateRequest } from '@core/services';
import { AppShell } from '@shared/layout';
import { AvatarUpload } from '@shared/avatar-upload';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-academia-perfil',
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
export class AcademiaPerfil implements OnInit {
  protected readonly userService = inject(UserService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly perfil = signal<UserGetResponse | null>(null);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');

  nome = '';
  email = '';
  novaSenha = '';
  confirmarSenha = '';

  ngOnInit(): void {
    this.userService.meuPerfil().subscribe({
      next: (u) => {
        this.perfil.set(u);
        this.nome = u.name;
        this.email = u.email;
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
    if (!this.nome.trim() || !this.email.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set('Nome e e-mail são obrigatórios.');
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

    const req: UserSelfUpdateRequest = {
      name: this.nome.trim(),
      email: this.email.trim(),
      senha: this.novaSenha.trim() || undefined,
    };

    this.userService.atualizarMeuPerfil(req).subscribe({
      next: () => {
        this.perfil.update((p) => (p ? { ...p, name: req.name, email: req.email } : p));
        this.novaSenha = '';
        this.confirmarSenha = '';
        this.salvando.set(false);
        this.avisoSeverity.set('success');
        this.aviso.set('Perfil atualizado com sucesso!');
      },
      error: (err) => {
        this.salvando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? 'Não foi possível atualizar o perfil.');
      },
    });
  }
}
