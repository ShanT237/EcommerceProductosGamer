import { HttpErrorResponse } from '@angular/common/http';
import { ErrorApi } from '../modelos/modelos';

/** Texto por defecto según el código HTTP, para cuando el backend no manda un `mensaje`. */
const MENSAJE_POR_ESTADO: Record<number, string> = {
  400: 'Los datos enviados no son válidos.',
  401: 'Tu sesión no es válida. Inicia sesión de nuevo.',
  403: 'No tienes permiso para hacer esto.',
  404: 'No se encontró lo que buscabas.',
  409: 'Esa información ya existe o cambió. Revisa e inténtalo de nuevo.',
  422: 'La operación no cumple las reglas del negocio.',
  500: 'Error del servidor. Inténtalo de nuevo en unos minutos.',
};

/** Convierte cualquier error HTTP en un mensaje claro para mostrarle a la persona. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'No hay conexión con el servidor. ¿Está encendido el backend?';
    // El mensaje del backend (reglas de negocio, validaciones) siempre tiene prioridad.
    const cuerpo = error.error as ErrorApi | null;
    if (cuerpo?.mensaje) return cuerpo.mensaje;
    const porDefecto = MENSAJE_POR_ESTADO[error.status];
    if (porDefecto) return porDefecto;
  }
  return 'Ocurrió un error inesperado.';
}
