import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { mensajeDeError } from '../../servicios/api.util';
import { ChatService, MensajeChatUi } from '../../servicios/chat.service';
import { fechaHora } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';

const MAXIMO_CARACTERES = 2000;
/** Distancia al final (px) por debajo de la cual se considera que la persona está leyendo lo último. */
const MARGEN_FINAL_PX = 120;

/**
 * Pantalla `/chat` y `/chat/:id`: lista de conversaciones y el hilo seleccionado, con envío optimista,
 * «Visto», historial paginado hacia atrás y aviso de reconexión. En pantallas estrechas muestra un solo panel.
 */
@Component({
  selector: 'app-chat-en-vivo',
  imports: [RouterLink],
  templateUrl: './chat-en-vivo.html',
  styleUrl: './chat-en-vivo.css',
})
export class ChatEnVivo {
  protected servicio = inject(ChatService);
  private toast = inject(ToastService);
  private ruta = inject(ActivatedRoute);
  private destruccion = inject(DestroyRef);

  protected fechaHora = fechaHora;
  protected readonly maximo = MAXIMO_CARACTERES;

  // No está habilitado withComponentInputBinding: el id se lee de la ruta.
  protected idActivo = toSignal(this.ruta.paramMap.pipe(map((p) => p.get('id'))), {
    initialValue: this.ruta.snapshot.paramMap.get('id'),
  });

  protected cargandoLista = signal(true);
  protected cargandoHilo = signal(false);
  protected cargandoAnteriores = signal(false);
  protected errorHilo = signal(false);
  protected borrador = signal('');

  protected conversacionActiva = computed(
    () => this.servicio.conversaciones().find((c) => c.id === this.idActivo()) ?? null,
  );
  protected mensajes = computed<MensajeChatUi[]>(() => {
    const id = this.idActivo();
    return id ? (this.servicio.mensajes()[id] ?? []) : [];
  });
  protected hayMasAnteriores = computed(() => {
    const id = this.idActivo();
    return id ? (this.servicio.hayMasAnteriores()[id] ?? false) : false;
  });
  protected conectado = computed(() => this.servicio.estado() === 'conectado');
  protected puedeEnviar = computed(() => this.borrador().trim().length > 0 && !this.errorHilo());

  private hilo = viewChild<ElementRef<HTMLElement>>('hilo');
  private pegadoAlFinal = true;
  private ultimoVisto: string | null = null;
  private restaurandoScroll = false;

  constructor() {
    // Se refresca al entrar por si llegaron conversaciones nuevas desde que se inició sesión.
    this.servicio.cargarConversaciones().subscribe({
      next: () => this.cargandoLista.set(false),
      error: (e) => {
        this.cargandoLista.set(false);
        this.toast.error(mensajeDeError(e));
      },
    });

    effect(() => {
      const id = this.idActivo();
      untracked(() => this.abrir(id));
    });

    effect(() => {
      const lista = this.mensajes();
      untracked(() => this.alCambiarMensajes(lista));
    });

    this.destruccion.onDestroy(() => this.servicio.abrirConversacion(null));
  }

  private abrir(id: string | null) {
    this.errorHilo.set(false);
    this.borrador.set('');
    this.pegadoAlFinal = true;
    this.ultimoVisto = null;
    this.servicio.abrirConversacion(id);
    if (!id) {
      this.cargandoHilo.set(false);
      return;
    }
    this.cargandoHilo.set(true);
    this.servicio.cargarHistorial(id).subscribe({
      next: () => {
        if (this.idActivo() === id) this.cargandoHilo.set(false);
      },
      error: (e) => {
        if (this.idActivo() !== id) return;
        this.cargandoHilo.set(false);
        this.errorHilo.set(true); // sin permiso o inexistente: no se muestra nada del hilo
        this.toast.error(mensajeDeError(e));
      },
    });
    this.servicio.marcarLeidos(id).subscribe({ error: () => undefined });
  }

  /** Auto-scroll al llegar o enviar un mensaje, solo si la persona ya estaba cerca del final (o el mensaje es suyo). */
  private alCambiarMensajes(lista: MensajeChatUi[]) {
    const ultimo = lista.length > 0 ? lista[lista.length - 1] : null;
    const idUltimo = ultimo?.id ?? null;
    const cambioElFinal = idUltimo !== this.ultimoVisto;
    this.ultimoVisto = idUltimo;
    if (!cambioElFinal || this.restaurandoScroll) return;
    const esMio = ultimo !== null && ultimo.remitenteId === this.servicio.usuarioId();
    if (this.pegadoAlFinal || esMio) this.irAlFinal();
  }

  private irAlFinal() {
    requestAnimationFrame(() => {
      const el = this.hilo()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }

  protected alScroll() {
    const el = this.hilo()?.nativeElement;
    if (!el) return;
    this.pegadoAlFinal = el.scrollHeight - el.scrollTop - el.clientHeight < MARGEN_FINAL_PX;
  }

  /** Trae los mensajes anteriores al primero confirmado y conserva la posición de lectura. */
  protected cargarAnteriores() {
    const id = this.idActivo();
    const primero = this.mensajes().find((m) => m.estadoEnvio === 'enviado');
    if (!id || !primero || this.cargandoAnteriores()) return;

    const el = this.hilo()?.nativeElement;
    const alturaAntes = el?.scrollHeight ?? 0;
    const desplazamientoAntes = el?.scrollTop ?? 0;
    this.cargandoAnteriores.set(true);
    this.restaurandoScroll = true;

    const terminar = () => {
      this.cargandoAnteriores.set(false);
      requestAnimationFrame(() => {
        if (el) el.scrollTop = el.scrollHeight - alturaAntes + desplazamientoAntes;
        this.restaurandoScroll = false;
      });
    };
    this.servicio.cargarHistorial(id, { antesDe: primero.id }).subscribe({
      next: terminar,
      error: (e) => {
        terminar();
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  protected alEscribir(evento: Event) {
    this.borrador.set((evento.target as HTMLTextAreaElement).value);
  }

  /** Enter envía; Shift+Enter inserta un salto de línea. */
  protected alTecla(evento: KeyboardEvent) {
    if (evento.key !== 'Enter' || evento.shiftKey || evento.isComposing) return;
    evento.preventDefault();
    this.enviar();
  }

  protected enviar() {
    const id = this.idActivo();
    const texto = this.borrador().trim();
    if (!id || !texto || this.errorHilo()) return;
    this.borrador.set('');
    this.pegadoAlFinal = true;
    // Si falla, el mensaje queda como «No enviado · Reintentar»: el error ya se ve en el propio hilo.
    this.servicio.enviar(id, texto).subscribe({ error: () => undefined });
  }

  protected reintentar(m: MensajeChatUi) {
    this.servicio.reintentar(m.conversacionId, m.clienteId);
  }

  protected esMio(m: MensajeChatUi): boolean {
    return m.remitenteId === this.servicio.usuarioId();
  }

  protected etiquetaContexto(tipo: 'PRODUCTO' | 'ORDEN'): string {
    return tipo === 'PRODUCTO' ? 'Producto' : 'Orden';
  }
}
