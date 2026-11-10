import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { FiaApiService } from './fia-api.service';
import { HttpFeedbackService } from './http-feedback.service';

export const adminGuard: CanActivateFn = async () => {
  const api = inject(FiaApiService);
  const router = inject(Router);
  const feedback = inject(HttpFeedbackService);
  try {
    const status = await api.authStatus();
    if (status.authenticated && status.role === 'ADMIN') return true;
    feedback.show(status.authenticated ? 'Este módulo es exclusivo para administración.' : 'Inicia sesión para continuar.');
  } catch { feedback.show('No se pudo verificar la sesión. Inicia sesión nuevamente.'); }
  return router.createUrlTree(['/']);
};
