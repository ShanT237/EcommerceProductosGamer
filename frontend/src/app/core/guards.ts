import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../servicios/auth.service';
import { Rol } from '../modelos/modelos';

/** Solo deja pasar a usuarios con sesión; si no, los manda al login recordando a dónde iban. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  auth.verificarSesion(); // en cada navegación, no solo al recargar la página
  return auth.autenticado() ? true : router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};

/** Evita que una persona con sesión activa vuelva al formulario de login. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) return true;
  return router.createUrlTree([auth.rol() === 'VENDEDOR' ? '/vendedor' : '/catalogo']);
};

/** Solo deja pasar a los roles indicados (COMPRADOR y/o VENDEDOR). */
export const rolGuard = (...roles: Rol[]): CanActivateFn => (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  auth.verificarSesion();
  if (!auth.autenticado()) return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  const actual = auth.rol();
  return actual !== null && roles.includes(actual) ? true : router.createUrlTree(['/']);
};
