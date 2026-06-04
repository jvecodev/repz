import { CommonModule } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FileService } from '@core/services';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

/**
 * Avatar com upload de foto de perfil. Busca a foto atual do usuário logado
 * (GET /api/files/me) e permite trocá-la (POST /api/files/upload). Quando não há
 * foto, exibe as iniciais informadas como fallback.
 */
@Component({
  selector: 'app-avatar-upload',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './avatar-upload.html',
  styleUrl: './avatar-upload.scss',
})
export class AvatarUpload implements OnInit {
  private readonly fileService = inject(FileService);
  private readonly i18n = inject(TranslateService);

  @Input() iniciais = '?';
  @Input() tamanho: 'md' | 'lg' | 'xl' = 'xl';

  readonly fotoUrl = signal<string | null>(null);
  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    this.fileService.minhaFoto().subscribe((url) => this.fotoUrl.set(url));
  }

  selecionar(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.erro.set(this.i18n.instant('AVATAR.INVALID_TYPE'));
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.erro.set(this.i18n.instant('AVATAR.TOO_LARGE'));
      return;
    }

    this.erro.set(null);
    this.enviando.set(true);
    this.fileService.upload(file).subscribe({
      next: (url) => {
        this.fotoUrl.set(url);
        this.enviando.set(false);
      },
      error: () => {
        this.erro.set(this.i18n.instant('AVATAR.UPLOAD_ERROR'));
        this.enviando.set(false);
      },
    });
    input.value = '';
  }
}
