import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from '../modelos/modelos';

/** Convierte cualquier error HTTP en un mensaje claro para mostrarle a la persona. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'No hay conexión con el servidor. ¿Está encendido el backend?';
    const cuerpo = error.error as ErrorApi | null;
    if (cuerpo?.mensaje) return cuerpo.mensaje;
    if (error.status === 401) return 'Tu sesión no es válida. Inicia sesión de nuevo.';
    if (error.status === 403) return 'No tienes permiso para hacer esto.';
    if (error.status === 404) return 'No se encontró lo que buscabas.';
  }
  return 'Ocurrió un error inesperado.';
}
