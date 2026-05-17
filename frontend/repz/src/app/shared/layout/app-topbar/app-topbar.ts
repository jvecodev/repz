import { Component, inject, input } from '@angular/core';
import { LayoutService } from '@core/services/layout';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss',
})
export class AppTopbar {
  protected readonly layout = inject(LayoutService);

  /** Trilha de navegação. O último item é destacado como atual. */
  readonly crumbs = input<string[]>([]);
}
