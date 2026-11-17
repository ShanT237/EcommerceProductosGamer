import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../servicios/auth.service';
import { API_URL } from '../config';
import { ErrorApi } from '../modelos/modelos';
import { CODIGO_CUENTA_INACTIVA, MENSAJE_CUENTA_INACTIVA } from '../servicios/api.util';

/**
 * Agrega el JWT a cada petición hacia la API y cierra la sesión si el token ya no sirve (401).
 * Si el 401 trae el código CUENTA_INACTIVA (la cuenta se dio de baja con el token aún vigente),
 * el aviso explica ese motivo en lugar del genérico de sesión vencida.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const esApi = req.url.startsWith(API_URL);
  // Antes de cada petición: si el token se borró o venció, no se ejecuta la acción; se cierra la sesión.
  if (esApi && !req.url.endsWith('/auth/login') && !auth.verificarSesion()) {
    return throwError(() => new HttpErrorResponse({ status: 401, url: req.url }));
  }
  const token = auth.token();
  const peticion = token && esApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      const esLogin = req.url.endsWith('/auth/login');
      if (error.status === 401 && esApi && !esLogin && token && auth.token() === token) {
        const cuentaInactiva = (error.error as ErrorApi | null)?.codigo === CODIGO_CUENTA_INACTIVA;
        auth.cerrarSesion(true, cuentaInactiva ? MENSAJE_CUENTA_INACTIVA : undefined);
      }
      return throwError(() => error);
    }),
  );
};