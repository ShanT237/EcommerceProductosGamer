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
  429: 'El asistente está recibiendo muchas solicitudes. Intenta de nuevo en unos segundos.',
  500: 'Error del servidor. Inténtalo de nuevo en unos minutos.',
  502: 'El servicio externo devolvió una respuesta inválida. Inténtalo de nuevo.',
  503: 'El servicio no está disponible en este momento. Inténtalo de nuevo en unos minutos.',
  504: 'El servicio tardó demasiado en responder. Inténtalo de nuevo.',
};

/** Código que manda el backend en un 401 cuando el token sigue vigente pero la cuenta fue dada de baja. */
export const CODIGO_CUENTA_INACTIVA = 'CUENTA_INACTIVA';

/** Aviso al cerrar la sesión de una cuenta que ya no está activa. */
export const MENSAJE_CUENTA_INACTIVA = 'Tu cuenta fue dada de baja o está inactiva. Cerramos tu sesión.';

/** Aviso para quien intenta comprar (o guardar favoritos) sin haber iniciado sesión. */
export const MENSAJE_INGRESAR_PARA_COMPRAR =
  'Para comprar necesitas iniciar sesión con una cuenta de comprador. Ingresa o crea tu cuenta.';

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
