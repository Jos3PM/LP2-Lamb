import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { HttpFeedbackService } from './http-feedback.service';

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const feedback = inject(HttpFeedbackService);
  return next(request).pipe(catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse && error.status === 401 &&
        !request.url.includes('/auth/login') && !request.url.includes('/auth/status')) {
      feedback.show('La sesión expiró. Inicia sesión nuevamente.');
      void router.navigateByUrl('/');
    } else if (error instanceof HttpErrorResponse && error.status === 403) {
      feedback.show('Tu cuenta no tiene permiso para realizar esta operación.');
    }
    return throwError(() => error);
  }));
};
