import { Component, inject, input } from '@angular/core';
import { LayoutService } from '@core/services/layout';
import { ThemeService } from '@core/services/theme';
import { LanguageService } from '@core/services/language.service';
import { ButtonModule } from 'primeng/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [ButtonModule, TranslatePipe],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss',
})
export class AppTopbar {
  protected readonly layout = inject(LayoutService);
  protected readonly theme = inject(ThemeService);
  protected readonly language = inject(LanguageService);

  /** Trilha de navegação. O último item é destacado como atual. */
  readonly crumbs = input<string[]>([]);
}
