import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Rol } from './models';

export const rolGuard = (...roles: Rol[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.token || !auth.perfil()) return router.createUrlTree(['/login']);
  const rol = auth.rol();
  if (rol && roles.includes(rol)) return true;
  return router.createUrlTree([auth.inicio(rol)]);
};

export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.token && auth.perfil() ? router.createUrlTree([auth.inicio()]) : true;
};

export const mfaGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.mfa ? true : router.createUrlTree(['/login']);
};

