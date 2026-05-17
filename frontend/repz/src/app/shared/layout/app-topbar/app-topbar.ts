import { Component, inject, input } from '@angular/core';
import { LayoutService } from '@core/services/layout';
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

  /** Trilha de navegação. O último item é destacado como atual. */
  readonly crumbs = input<string[]>([]);
}
