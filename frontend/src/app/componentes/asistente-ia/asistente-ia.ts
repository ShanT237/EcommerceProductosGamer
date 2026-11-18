import { Component, ElementRef, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { MensajeAsistente, Rol } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { AsistenteService } from '../../servicios/asistente.service';
import { AuthService } from '../../servicios/auth.service';

const SUGERENCIAS: Record<Rol, string[]> = {
  COMPRADOR: [
    'Recomiéndame algo según mis compras',
    '¿Qué combos me convienen?',
    'Busco un teclado económico',
  ],
  VENDEDOR: [
    '¿Cómo va mi dashboard?',
    '¿Cuáles son mis productos más vendidos?',
    '¿Qué productos tienen stock bajo?',
  ],
};

/** Chat flotante con el asistente de IA. Comprador: recomendaciones. Vendedor: análisis de su tienda. */
@Component({
  selector: 'app-asistente-ia',
  templateUrl: './asistente-ia.html',
  styleUrl: './asistente-ia.css',
})
export class AsistenteIa {
  private asistente = inject(AsistenteService);
  private auth = inject(AuthService);

  protected abierto = signal(false);
  /** Solo turnos completos (pregunta + respuesta); es lo que se manda como historial. */
  protected mensajes = signal<MensajeAsistente[]>([]);
  protected cargando = signal(false);
  protected error = signal('');
  protected texto = signal('');
  protected sugerencias = computed(() => SUGERENCIAS[this.auth.rol() ?? 'COMPRADOR']);

  private lista = viewChild<ElementRef<HTMLElement>>('lista');
  // computed compara por valor: el efecto solo corre si cambia de verdad la persona.
  private sesionId = computed(() => this.auth.sesion()?.id ?? null);

  constructor() {
    effect(() => {
      this.sesionId();
      untracked(() => this.reiniciar(true));
    });
  }

  /** Vacía la conversación; con `cerrar` también esconde el panel (cambio de persona o fin de sesión). */
  protected reiniciar(cerrar: boolean) {
    this.mensajes.set([]);
    this.error.set('');
    this.texto.set('');
    this.cargando.set(false);
    if (cerrar) this.abierto.set(false);
  }

  protected alEscribir(evento: Event) {
    this.texto.set((evento.target as HTMLTextAreaElement).value);
  }

  /** Enter envía; Shift+Enter inserta un salto de línea. */
  protected alEnter(evento: Event) {
    if ((evento as KeyboardEvent).shiftKey) return;
    evento.preventDefault();
    this.enviar();
  }
  protected enviar(entrada?: string) {
    const mensaje = (entrada ?? this.texto()).trim();
    if (!mensaje || this.cargando()) return;

    const historialPrevio = this.mensajes();
    this.mensajes.set([...historialPrevio, { rol: 'user', contenido: mensaje }]);
    this.texto.set('');
    this.error.set('');
    this.cargando.set(true);
    this.bajar();

    const sesion = this.sesionId();
    this.asistente.preguntar(mensaje, historialPrevio).subscribe({
      next: (r) => {
        if (sesion !== this.sesionId()) return; // la persona cambió mientras se esperaba
        this.mensajes.update((m) => [...m, { rol: 'assistant', contenido: r.respuesta }]);
        this.cargando.set(false);
        this.bajar();
      },
      error: (e) => {
        if (sesion !== this.sesionId()) return;
        // El turno fallido no queda en el historial; el texto vuelve al campo para reintentar.
        this.mensajes.set(historialPrevio);
        this.texto.set(mensaje);
        this.error.set(mensajeDeError(e));
        this.cargando.set(false);
        this.bajar();
      },
    });
  }

  private bajar() {
    setTimeout(() => {
      const el = this.lista()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
