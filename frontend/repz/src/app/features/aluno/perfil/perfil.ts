import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlunoDetalheResponse, AlunoMeUpdateRequest, AlunoService, AuthService } from '@core/services';
import { AppShell } from '@shared/layout';
import { AvatarUpload } from '@shared/avatar-upload';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    AvatarUpload,
    TranslatePipe,
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
export class Perfil implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly service = inject(AlunoService);
  private readonly i18n = inject(TranslateService);

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly aluno = signal<AlunoDetalheResponse | null>(null);
  readonly editando = signal(false);

  formNome = '';
  formTelefone = '';
  novaSenha = '';
  confirmarSenha = '';

  readonly emailSessao = computed(() => this.auth.sessao()?.email ?? '—');

  readonly inicial = computed(() => {
    const nome = this.aluno()?.nome ?? this.emailSessao();
    return (nome.trim()[0] ?? 'A').toUpperCase();
  });

  ngOnInit(): void {
    this.service.meuPerfil().subscribe({
      next: (a) => {
        this.aluno.set(a);
        this.formNome = a.nome ?? '';
        this.formTelefone = a.telefone ?? '';
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set(this.i18n.instant('PROFILE.LOAD_ERROR'));
      },
    });
  }

  abrirEdicao(): void {
    this.novaSenha = '';
    this.confirmarSenha = '';
    this.aviso.set(null);
    this.editando.set(true);
  }

  cancelarEdicao(): void {
    const a = this.aluno();
    this.formNome = a?.nome ?? '';
    this.formTelefone = a?.telefone ?? '';
    this.novaSenha = '';
    this.confirmarSenha = '';
    this.editando.set(false);
    this.aviso.set(null);
  }

  salvar(): void {
    if (this.salvando()) return;
    if (!this.formNome.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('PROFILE.NAME_REQUIRED'));
      return;
    }
    if (this.novaSenha && this.novaSenha.length < 5) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('PROFILE.PASSWORD_MIN'));
      return;
    }
    if (this.novaSenha !== this.confirmarSenha) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('VALIDATION.PASSWORD_MISMATCH'));
      return;
    }

    this.aviso.set(null);
    this.salvando.set(true);

    const req: AlunoMeUpdateRequest = {
      nome: this.formNome.trim(),
      telefone: this.formTelefone.trim() || undefined,
      senha: this.novaSenha.trim() || undefined,
    };

    this.service.atualizarMeuPerfil(req).subscribe({
      next: (a) => {
        this.aluno.set(a);
        this.formNome = a.nome ?? '';
        this.formTelefone = a.telefone ?? '';
        this.novaSenha = '';
        this.confirmarSenha = '';
        this.salvando.set(false);
        this.editando.set(false);
        this.avisoSeverity.set('success');
        this.aviso.set(this.i18n.instant('PROFILE.SAVE_SUCCESS'));
        setTimeout(() => this.aviso.set(null), 3500);
      },
      error: (err) => {
        this.salvando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('PROFILE.SAVE_ERROR'));
      },
    });
  }
}
