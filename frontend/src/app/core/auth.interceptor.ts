import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const r = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(r).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && token && !req.url.includes('/auth/logout')) {
        auth.cerrarSesion(e.error?.mensaje ?? 'Tu sesión terminó. Ingresa nuevamente.', false);
      }
      return throwError(() => e);
    }),
  );
};

/** Mensaje legible de un error de la API. */
export function mensajeError(e: unknown, porDefecto = 'No se pudo completar la operación'): string {
  const err = e as HttpErrorResponse;
  if (err?.status === 0) return 'No hay conexión con el servidor de FiadoPe.';
  return err?.error?.mensaje ?? porDefecto;
}

export function erroresCampos(e: unknown): Record<string, string> {
  return (e as HttpErrorResponse)?.error?.errores ?? {};
}

