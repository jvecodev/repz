import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '@core/services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint = req.url.includes('/api/auth/');
      const podeRenovar =
        error.status === 401 && !isAuthEndpoint && !!authService.getRefreshToken();

      if (!podeRenovar) {
        return throwError(() => error);
      }

      // Token expirado: tenta renovar e refaz a requisição original uma vez.
      return authService.refresh().pipe(
        switchMap((res) =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${res.token}` } })),
        ),
        catchError((refreshError) => {
          authService.logout();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
