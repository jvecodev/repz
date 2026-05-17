import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [FormsModule, ButtonModule, CardModule, InputTextModule, MessageModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss',
})
export class Auth {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly credenciais = { email: '', password: '' };
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);

  realizarLogin(): void {
    if (this.carregando()) return;
    this.erro.set(null);
    this.carregando.set(true);

    this.authService.login(this.credenciais.email, this.credenciais.password).subscribe({
      next: () => {
        this.carregando.set(false);
        this.redirecionarPorPerfil(this.authService.getUserRole());
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(
          err?.status === 401 || err?.status === 400
            ? 'E-mail ou senha inválidos.'
            : 'Não foi possível entrar. Tente novamente.',
        );
      },
    });
  }

  private redirecionarPorPerfil(role: string | null): void {
    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/admin']);
        break;
      case 'ACADEMIA':
        this.router.navigate(['/academia']);
        break;
      case 'PERSONAL':
        this.router.navigate(['/personal']);
        break;
      case 'USUARIO':
        this.router.navigate(['/aluno/ficha-treino']);
        break;
      default:
        this.erro.set('Perfil de usuário desconhecido.');
    }
  }
}
