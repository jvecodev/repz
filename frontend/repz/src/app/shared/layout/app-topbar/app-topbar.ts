import { Component, inject, input } from '@angular/core';
import { LayoutService } from '@core/services/layout';
import { ThemeService } from '@core/services/theme';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss',
})
export class AppTopbar {
  protected readonly layout = inject(LayoutService);
  protected readonly theme = inject(ThemeService);

  /** Trilha de navegação. O último item é destacado como atual. */
  readonly crumbs = input<string[]>([]);
}
