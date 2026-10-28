import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../servicios/auth.service';
import { API_URL } from '../config';

/** Agrega el JWT a cada petición hacia la API y cierra la sesión si el token ya no sirve (401). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const esApi = req.url.startsWith(API_URL);
  const peticion = token && esApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      const esLogin = req.url.endsWith('/auth/login');
      if (error.status === 401 && esApi && !esLogin && token) {
        auth.cerrarSesion(true);
      }
      return throwError(() => error);
    }),
  );
};
