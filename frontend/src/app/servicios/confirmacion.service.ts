import { Injectable, signal } from '@angular/core';

export interface OpcionesConfirmacion {
  titulo?: string;
  mensaje: string;
  /** Texto del botón que confirma. Por defecto "Aceptar". */
  confirmar?: string;
  /** Texto del botón que cancela. Por defecto "Cancelar". */
  cancelar?: string;
  /** `true` (por defecto) pinta el botón de confirmar en rojo, para acciones que borran algo. */
  peligro?: boolean;
}

/**
 * Reemplazo del `confirm()` nativo: abre un aviso dentro de la app y devuelve una promesa
 * que se resuelve en `true` si la persona acepta y en `false` si cancela, cierra o pulsa Escape.
 *
 *   if (!(await this.confirmacion.pedir({ mensaje: '¿Eliminar X?', confirmar: 'Eliminar' }))) return;
 *
 * El aviso lo dibuja `<app-confirmacion />`, que vive en `app.html`.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  readonly actual = signal<Required<OpcionesConfirmacion> | null>(null);
  private resolver: ((acepto: boolean) => void) | null = null;

  pedir(opciones: OpcionesConfirmacion): Promise<boolean> {
    this.resolver?.(false); // si ya había un aviso abierto, se da por cancelado
    this.actual.set({
      titulo: 'Confirmar',
      confirmar: 'Aceptar',
      cancelar: 'Cancelar',
      peligro: true,
      ...opciones,
    });
    return new Promise<boolean>((resolve) => (this.resolver = resolve));
  }

  responder(acepto: boolean) {
    const resolver = this.resolver;
    this.resolver = null;
    this.actual.set(null);
    resolver?.(acepto);
  }
}
