import { Component, inject, input, output } from '@angular/core';
import { AppSidebar } from '../app-sidebar/app-sidebar';
import { AppTopbar } from '../app-topbar/app-topbar';
import { LayoutService } from '@core/services/layout';

/**
 * Casca de layout reutilizável: sidebar + topbar + área de conteúdo.
 * Uso:
 *   <app-shell ativo="treino" [crumbs]="['Aluno','Meu treino']"
 *              [nome]="nome()" [subtitulo]="objetivo()">
 *     ...conteúdo da página...
 *   </app-shell>
 */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [AppSidebar, AppTopbar],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  protected readonly layout = inject(LayoutService);

  readonly nome = input<string>('Usuário');
  readonly subtitulo = input<string>('');
  readonly ativo = input<string>('');
  readonly crumbs = input<string[]>([]);

  readonly ctaTitulo = input<string>('SIDEBAR.CTA_TITLE');
  readonly ctaDescricao = input<string>('SIDEBAR.CTA_DESC');
  readonly ctaLabel = input<string>('SIDEBAR.CTA_LABEL');
  readonly mostrarCta = input<boolean>(true);

  readonly ctaClick = output<void>();
}
