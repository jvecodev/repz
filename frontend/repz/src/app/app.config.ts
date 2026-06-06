import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

import {
  provideClientHydration,
  withEventReplay
} from '@angular/platform-browser';

import { providePrimeNG } from 'primeng/config';
import Lara from '@primeng/themes/lara';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import { authInterceptor } from './core/interceptors/auth-interceptor';

import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideClientHydration(withEventReplay()),

    providePrimeNG({
      theme: {
        preset: Lara,
        options: {
          darkModeSelector: '[data-theme="dark"]'
        },
      },
    }),

    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    provideTranslateService({
      fallbackLang: 'pt-BR',
    }),
    provideTranslateHttpLoader({
      prefix: '/assets/i18n/',
      suffix: '.json',
    }),
  ],
};