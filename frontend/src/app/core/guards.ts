import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../servicios/auth.service';
import { Rol } from '../modelos/modelos';

/** Solo deja pasar a usuarios con sesión; si no, los manda al login recordando a dónde iban. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado() ? true : router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};

/** Solo deja pasar al rol indicado (COMPRADOR o VENDEDOR). */
export const rolGuard = (rol: Rol): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) return router.createUrlTree(['/login']);
  return auth.rol() === rol ? true : router.createUrlTree(['/']);
};
