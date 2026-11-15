import { Component, DestroyRef, ElementRef, effect, inject, viewChild } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ConfirmacionService } from '../../servicios/confirmacion.service';

/** Aviso modal de confirmación. Se monta una sola vez en `app.html`; lo abre `ConfirmacionService.pedir()`. */
@Component({
  selector: 'app-confirmacion',
  templateUrl: './confirmacion.html',
  styleUrl: './confirmacion.css',
})
export class Confirmacion {
  protected servicio = inject(ConfirmacionService);
  private botonCancelar = viewChild<ElementRef<HTMLButtonElement>>('botonCancelar');

  constructor() {
    // El foco empieza en "Cancelar": un Enter distraído nunca borra nada.
    effect(() => this.botonCancelar()?.nativeElement.focus());

    // Escape y Tab se escuchan en la fase de captura del documento: así el aviso se queda con la tecla
    // y no se cierra también el modal (catálogo, edición…) que haya detrás.
    const documento = inject(DOCUMENT);
    const alTeclear = (e: KeyboardEvent) => {
      if (!this.servicio.actual()) return;
      if (e.key === 'Escape') {
        e.stopPropagation();
        e.preventDefault();
        this.cancelar();
      } else if (e.key === 'Tab') {
        // Foco atrapado entre los dos botones del aviso.
        const botones = Array.from(documento.querySelectorAll<HTMLButtonElement>('.dialogo .acciones button'));
        if (botones.length === 0) return;
        const i = botones.indexOf(documento.activeElement as HTMLButtonElement);
        const siguiente = e.shiftKey ? (i <= 0 ? botones.length - 1 : i - 1) : (i + 1) % botones.length;
        e.preventDefault();
        botones[siguiente].focus();
      }
    };
    documento.addEventListener('keydown', alTeclear, true);
    inject(DestroyRef).onDestroy(() => documento.removeEventListener('keydown', alTeclear, true));
  }

  protected aceptar() { this.servicio.responder(true); }

  protected cancelar() {
    if (this.servicio.actual()) this.servicio.responder(false);
  }
}
