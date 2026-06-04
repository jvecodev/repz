import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth';
import { ThemeService } from '@core/services/theme';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '@core/services/language.service';

type Etapa = 'login' | 'esqueci' | 'redefinir';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    TranslatePipe,
  ],
  templateUrl: './auth.html',
  styleUrl: './auth.scss',
})
export class Auth {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly i18n = inject(TranslateService);
  protected readonly theme = inject(ThemeService);
  protected readonly language = inject(LanguageService);

  readonly etapa = signal<Etapa>('login');
  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly sucesso = signal<string | null>(null);

  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(5)]],
  });

  readonly esqueciForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly redefinirForm = this.fb.group(
    {
      token: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(5)]],
      confirmarSenha: ['', [Validators.required]],
    },
    { validators: senhasIguaisValidator },
  );

  get lEmail() { return this.loginForm.get('email')!; }
  get lPassword() { return this.loginForm.get('password')!; }
  get eEmail() { return this.esqueciForm.get('email')!; }
  get rToken() { return this.redefinirForm.get('token')!; }
  get rNewPassword() { return this.redefinirForm.get('newPassword')!; }
  get rConfirmar() { return this.redefinirForm.get('confirmarSenha')!; }

  realizarLogin(): void {
    if (this.carregando()) return;
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) return;

    this.erro.set(null);
    this.sucesso.set(null);
    this.carregando.set(true);
    const { email, password } = this.loginForm.value;

    this.authService.login(email!, password!).subscribe({
      next: () => {
        this.carregando.set(false);
        this.redirecionarPorPerfil(this.authService.getUserRole());
      },
      error: (err) => {
        this.carregando.set(false);
        if (err?.status === 401 || err?.status === 400) {
          this.erro.set(this.i18n.instant('AUTH.MESSAGES.INVALID_CREDENTIALS'));
        } else if (err?.status === 403) {
          this.erro.set(this.i18n.instant('AUTH.MESSAGES.ACCESS_DENIED'));
        } else {
          this.erro.set(this.i18n.instant('AUTH.MESSAGES.LOGIN_ERROR'));
        }
      },
    });
  }

  enviarEsqueci(): void {
    if (this.carregando()) return;
    this.esqueciForm.markAllAsTouched();
    if (this.esqueciForm.invalid) return;

    this.erro.set(null);
    this.carregando.set(true);
    const { email } = this.esqueciForm.value;

    this.authService.forgotPassword(email!).subscribe({
      next: () => this.irParaRedefinir(email!),
      // Backend sempre retorna 204 por segurança; erro só acontece em falha de rede
      error: () => this.irParaRedefinir(email!),
    });
  }

  redefinirSenha(): void {
    if (this.carregando()) return;
    this.redefinirForm.markAllAsTouched();
    if (this.redefinirForm.invalid) return;

    this.erro.set(null);
    this.carregando.set(true);
    const { token, newPassword } = this.redefinirForm.value;

    this.authService.resetPassword(token!, newPassword!).subscribe({
      next: () => {
        this.carregando.set(false);
        this.irParaLogin(this.i18n.instant('AUTH.MESSAGES.RESET_SUCCESS'));
      },
      error: (err) => {
        this.carregando.set(false);
        if (err?.status === 400) {
          this.erro.set(this.i18n.instant('AUTH.MESSAGES.RESET_INVALID_CODE'));
        } else {
          this.erro.set(this.i18n.instant('AUTH.MESSAGES.RESET_ERROR'));
        }
      },
    });
  }

  irParaEsqueci(): void {
    this.erro.set(null);
    this.sucesso.set(null);
    this.etapa.set('esqueci');
  }

  irParaLogin(mensagem?: string): void {
    this.erro.set(null);
    this.sucesso.set(mensagem ?? null);
    this.loginForm.reset();
    this.esqueciForm.reset();
    this.redefinirForm.reset();
    this.etapa.set('login');
  }

  private irParaRedefinir(email: string): void {
    this.carregando.set(false);
    this.etapa.set('redefinir');
    this.sucesso.set(this.i18n.instant('AUTH.MESSAGES.CODE_SENT', { email }));
  }

  private redirecionarPorPerfil(role: string | null): void {
    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/admin']);
        break;
      case 'GERENTE':
        this.router.navigate(['/academia']);
        break;
      case 'PERSONAL':
        this.router.navigate(['/personal']);
        break;
      case 'ALUNO':
        this.router.navigate(['/aluno/ficha-treino']);
        break;
      default:
        this.erro.set(this.i18n.instant('AUTH.MESSAGES.UNKNOWN_ROLE'));
    }
  }
}

function senhasIguaisValidator(group: AbstractControl) {
  const nova = group.get('newPassword')?.value;
  const confirmar = group.get('confirmarSenha')?.value;
  return nova && confirmar && nova !== confirmar ? { senhasDiferentes: true } : null;
}
