import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LanguageService } from '@core/services/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  // Instancia o serviço de idioma no boot para aplicar o idioma salvo.
  private readonly language = inject(LanguageService);
  protected readonly title = signal('repz');
}
