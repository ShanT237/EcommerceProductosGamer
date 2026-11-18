import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, ReplaySubject, tap } from 'rxjs';
import { API_URL } from '../config';
import { abrirSse, EstadoConexion } from '../core/sse-client';
import {
  ConversacionChat,
  HistorialChat,
  LecturaChat,
  MensajeChat,
  TipoContextoChat,
} from '../modelos/modelos';
import { MENSAJE_CUENTA_INACTIVA } from './api.util';
import { AuthService } from './auth.service';

export type EstadoEnvio = 'enviando' | 'enviado' | 'fallido';

/** Mensaje tal como lo muestra la pantalla: el del servidor más su estado de envío local. */
export type MensajeChatUi = MensajeChat & { estadoEnvio: EstadoEnvio };

const LONGITUD_VISTA_PREVIA = 80;

/** Mismo orden que el servidor: por fecha de envío y, a igualdad, por id. */
function comparar(a: MensajeChat, b: MensajeChat): number {
  if (a.enviadoEn !== b.enviadoEn) return a.enviadoEn < b.enviadoEn ? -1 : 1;
  return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
}

/** Los confirmados van en orden del servidor; los pendientes (enviando/fallido) siempre al final. */
function ordenar(lista: MensajeChatUi[]): MensajeChatUi[] {
  const confirmados = lista.filter((m) => m.estadoEnvio === 'enviado').sort(comparar);
  const pendientes = lista.filter((m) => m.estadoEnvio !== 'enviado');
  return [...confirmados, ...pendientes];
}

/** `crypto.randomUUID` solo existe en contextos seguros (https/localhost): se cae a getRandomValues. */
function nuevoUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function pestanaVisible(): boolean {
  return typeof document !== 'undefined' && document.visibilityState === 'visible';
}

/**
 * Chat en vivo de la sesión activa (COMPRADOR o VENDEDOR): conversaciones, historial, envío con
 * mensaje optimista, no leídos y un único stream SSE por usuario. Arranca al iniciar sesión y se
 * vacía al cambiar o perder la sesión, para no mostrar nunca datos de otra persona.
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private _conversaciones = signal<ConversacionChat[]>([]);
  private _mensajes = signal<Record<string, MensajeChatUi[]>>({});
  private _hayMasAnteriores = signal<Record<string, boolean>>({});
  private _estado = signal<EstadoConexion>('desconectado');

  readonly conversaciones = this._conversaciones.asReadonly();
  readonly mensajes = this._mensajes.asReadonly();
  readonly hayMasAnteriores = this._hayMasAnteriores.asReadonly();
  readonly estado = this._estado.asReadonly();
  readonly noLeidasTotal = computed(() => this._conversaciones().reduce((suma, c) => suma + c.noLeidos, 0));

  // computed compara por valor: el efecto solo corre si cambia de verdad la persona (no por renovar el token).
  readonly usuarioId = computed(() => this.auth.sesion()?.id ?? null);

  private conexion: { cerrar: () => void } | null = null;
  private activa: string | null = null;

  constructor() {
    effect(() => {
      const id = this.usuarioId();
      untracked(() => {
        this.conexion?.cerrar();
        this.conexion = null;
        this.vaciar();
        if (!id) return;
        this.cargarConversaciones().subscribe({ error: () => undefined });
        this.conexion = abrirSse({
          url: `${API_URL}/chat/stream`,
          obtenerToken: () => (this.auth.verificarSesion() ? this.auth.token() : null),
          alEvento: (evento, datos) => this.alEvento(evento, datos),
          alEstado: (estado) => this._estado.set(estado),
          alReconectado: () => this.ponerAlDia(),
          alNoAutorizado: (inactiva) =>
            this.auth.cerrarSesion(true, inactiva ? MENSAJE_CUENTA_INACTIVA : undefined),
        });
      });
    });

    if (typeof document !== 'undefined') {
      // Al volver a la pestaña, lo que llegó mientras estaba oculta cuenta como leído.
      document.addEventListener('visibilitychange', () => {
        if (!pestanaVisible() || !this.activa) return;
        const conversacion = this._conversaciones().find((c) => c.id === this.activa);
        if (conversacion && conversacion.noLeidos > 0) this.marcarLeidos(conversacion.id).subscribe({ error: () => undefined });
      });
    }
  }

  // ---------------------------------------------------------------- REST

  /** GET /chat/conversaciones: reemplaza la lista local (más reciente primero). */
  cargarConversaciones(): Observable<ConversacionChat[]> {
    const solicitante = this.usuarioId();
    return this.http.get<ConversacionChat[]>(`${API_URL}/chat/conversaciones`).pipe(
      tap((lista) => {
        // Si la sesión cambió mientras llegaba la respuesta, se descarta para no mostrar datos de otra persona.
        if (this.usuarioId() === solicitante) this._conversaciones.set(lista);
      }),
    );
  }

  /** POST /chat/conversaciones (idempotente): inicia o recupera la conversación y la deja en la lista. */
  iniciar(tipo: TipoContextoChat, referenciaId: string, vendedorId?: string): Observable<ConversacionChat> {
    const solicitante = this.usuarioId();
    const cuerpo = vendedorId ? { tipo, referenciaId, vendedorId } : { tipo, referenciaId };
    return this.http.post<ConversacionChat>(`${API_URL}/chat/conversaciones`, cuerpo).pipe(
      tap((conversacion) => {
        if (this.usuarioId() !== solicitante) return;
        this._conversaciones.update((lista) => [conversacion, ...lista.filter((c) => c.id !== conversacion.id)]);
      }),
    );
  }

  /**
   * GET …/mensajes. Sin cursor trae lo más reciente; `antesDe` pagina hacia atrás y `despuesDe`
   * hace el catch-up hacia adelante. Mezcla sin duplicar por id y mantiene el orden cronológico.
   */
  cargarHistorial(
    conversacionId: string,
    opciones: { antesDe?: string; despuesDe?: string } = {},
  ): Observable<HistorialChat> {
    const solicitante = this.usuarioId();
    const params: Record<string, string> = {};
    if (opciones.antesDe) params['antesDe'] = opciones.antesDe;
    if (opciones.despuesDe) params['despuesDe'] = opciones.despuesDe;
    return this.http
      .get<HistorialChat>(`${API_URL}/chat/conversaciones/${conversacionId}/mensajes`, { params })
      .pipe(
        tap((respuesta) => {
          if (this.usuarioId() !== solicitante) return;
          this.mezclar(conversacionId, respuesta.mensajes);
          if (opciones.despuesDe) return; // el catch-up no dice nada sobre lo anterior
          const actual = this._hayMasAnteriores();
          if (opciones.antesDe || actual[conversacionId] === undefined) {
            this._hayMasAnteriores.set({ ...actual, [conversacionId]: respuesta.hayMas });
          }
        }),
      );
  }

  /**
   * Envía un mensaje: aparece al instante como «enviando» y se reemplaza por el real al responder;
   * si falla queda «fallido» (reintentable con el mismo clienteId, así el servidor no lo duplica).
   * El envío empieza al llamar, aunque nadie se suscriba.
   */
  enviar(conversacionId: string, contenido: string, clienteId: string = nuevoUuid()): Observable<MensajeChat> {
    const texto = contenido.trim();
    const optimista: MensajeChatUi = {
      id: clienteId,
      conversacionId,
      remitenteId: this.usuarioId() ?? '',
      clienteId,
      contenido: texto,
      enviadoEn: new Date().toISOString(),
      leidoEn: null,
      estadoEnvio: 'enviando',
    };
    this._mensajes.update((todos) => ({
      ...todos,
      [conversacionId]: ordenar([
        ...(todos[conversacionId] ?? []).filter((m) => m.clienteId !== clienteId),
        optimista,
      ]),
    }));
    return this.transmitir(conversacionId, clienteId, texto);
  }

  /** Reenvía un mensaje pendiente o fallido con **el mismo** clienteId. */
  reintentar(conversacionId: string, clienteId: string): void {
    const pendiente = (this._mensajes()[conversacionId] ?? []).find(
      (m) => m.clienteId === clienteId && m.estadoEnvio !== 'enviado',
    );
    if (!pendiente) return;
    this.cambiarEstado(conversacionId, clienteId, 'enviando', true);
    this.transmitir(conversacionId, clienteId, pendiente.contenido).subscribe({ error: () => undefined });
  }

  /** PUT …/leidos: limpia el contador local con la respuesta del servidor. */
  marcarLeidos(conversacionId: string): Observable<{ marcados: number }> {
    const solicitante = this.usuarioId();
    return this.http.put<{ marcados: number }>(`${API_URL}/chat/conversaciones/${conversacionId}/leidos`, {}).pipe(
      tap(() => {
        if (this.usuarioId() === solicitante) this.ponerNoLeidos(conversacionId, 0);
      }),
    );
  }

  /** Conversación abierta en pantalla (o null): sus mensajes entrantes se marcan leídos si la pestaña es visible. */
  abrirConversacion(id: string | null): void {
    this.activa = id;
  }

  // ---------------------------------------------------------------- Tiempo real

  private alEvento(evento: string, datos: string): void {
    try {
      if (evento === 'mensaje') this.alMensaje(JSON.parse(datos) as MensajeChat);
      else if (evento === 'leidos') this.alLeidos(JSON.parse(datos) as LecturaChat);
    } catch {
      // un evento mal formado no debe tumbar el stream
    }
  }

  private alMensaje(mensaje: MensajeChat): void {
    const existente = (this._mensajes()[mensaje.conversacionId] ?? []).some(
      (m) => m.id === mensaje.id && m.estadoEnvio === 'enviado',
    );
    if (existente) return;

    this.mezclar(mensaje.conversacionId, [mensaje]);

    if (!this._conversaciones().some((c) => c.id === mensaje.conversacionId)) {
      // Conversación nueva para esta persona: la lista del servidor ya trae su vista previa y su contador.
      this.cargarConversaciones().subscribe({ error: () => undefined });
      return;
    }
    this.registrarActividad(mensaje);

    if (mensaje.remitenteId === this.usuarioId()) return; // eco de mis propios envíos
    if (this.activa === mensaje.conversacionId && pestanaVisible()) {
      this.marcarLeidos(mensaje.conversacionId).subscribe({ error: () => undefined });
    } else {
      this._conversaciones.update((lista) =>
        lista.map((c) => (c.id === mensaje.conversacionId ? { ...c, noLeidos: c.noLeidos + 1 } : c)),
      );
    }
  }

  private alLeidos(lectura: LecturaChat): void {
    if (lectura.lectorId === this.usuarioId()) {
      this.ponerNoLeidos(lectura.conversacionId, 0); // mis otras pestañas limpian su contador
      return;
    }
    // La otra persona leyó: «Visto» en mis mensajes enviados de esa conversación.
    const yo = this.usuarioId();
    this._mensajes.update((todos) => {
      const lista = todos[lectura.conversacionId];
      if (!lista) return todos;
      return {
        ...todos,
        [lectura.conversacionId]: lista.map((m) =>
          m.remitenteId === yo && m.estadoEnvio === 'enviado' && !m.leidoEn ? { ...m, leidoEn: lectura.leidoEn } : m,
        ),
      };
    });
  }

  /** Tras reconectar: pone al día la lista, el hilo abierto y reenvía lo que no llegó a salir. */
  private ponerAlDia(): void {
    this.cargarConversaciones().subscribe({ error: () => undefined });
    if (this.activa) this.recuperar(this.activa);
    for (const [conversacionId, lista] of Object.entries(this._mensajes())) {
      for (const m of lista) {
        if (m.estadoEnvio === 'enviado') continue;
        this.cambiarEstado(conversacionId, m.clienteId, 'enviando', true);
        // El servidor es idempotente por (remitente, clienteId): un reenvío no duplica el mensaje.
        this.transmitir(conversacionId, m.clienteId, m.contenido).subscribe({ error: () => undefined });
      }
    }
  }

  /** Catch-up del hilo abierto: pide lo posterior al último mensaje confirmado, repitiendo mientras `hayMas`. */
  private recuperar(conversacionId: string): void {
    const solicitante = this.usuarioId();
    const ultimo = [...(this._mensajes()[conversacionId] ?? [])].reverse().find((m) => m.estadoEnvio === 'enviado');
    this.cargarHistorial(conversacionId, ultimo ? { despuesDe: ultimo.id } : {}).subscribe({
      next: (respuesta) => {
        if (ultimo && respuesta.hayMas && this.usuarioId() === solicitante) this.recuperar(conversacionId);
      },
      error: () => undefined,
    });
  }

  // ---------------------------------------------------------------- Internos

  private transmitir(conversacionId: string, clienteId: string, contenido: string): Observable<MensajeChat> {
    const solicitante = this.usuarioId();
    const salida = new ReplaySubject<MensajeChat>(1);
    this.http
      .post<MensajeChat>(`${API_URL}/chat/conversaciones/${conversacionId}/mensajes`, { clienteId, contenido })
      .subscribe({
        next: (mensaje) => {
          if (this.usuarioId() === solicitante) {
            this.mezclar(conversacionId, [mensaje]);
            this.registrarActividad(mensaje);
          }
          salida.next(mensaje);
          salida.complete();
        },
        error: (error: unknown) => {
          if (this.usuarioId() === solicitante) this.cambiarEstado(conversacionId, clienteId, 'fallido');
          salida.error(error);
        },
      });
    return salida.asObservable();
  }

  /** Mezcla mensajes del servidor sin duplicar: por id y, si había uno optimista, por clienteId. */
  private mezclar(conversacionId: string, nuevos: MensajeChat[]): void {
    if (nuevos.length === 0) return;
    this._mensajes.update((todos) => {
      const lista = [...(todos[conversacionId] ?? [])];
      for (const nuevo of nuevos) {
        const confirmado: MensajeChatUi = { ...nuevo, estadoEnvio: 'enviado' };
        let posicion = lista.findIndex((m) => m.id === nuevo.id);
        if (posicion === -1) {
          posicion = lista.findIndex((m) => m.clienteId === nuevo.clienteId && m.remitenteId === nuevo.remitenteId);
        }
        if (posicion === -1) lista.push(confirmado);
        else lista[posicion] = confirmado;
      }
      return { ...todos, [conversacionId]: ordenar(lista) };
    });
  }

  /**
   * Cambia el estado de envío de un mensaje pendiente. Nunca degrada uno ya confirmado
   * (p. ej. un reintento que llega tarde con error no debe tumbar un envío que sí salió).
   */
  private cambiarEstado(conversacionId: string, clienteId: string, estado: EstadoEnvio, soloPendiente = false): void {
    this._mensajes.update((todos) => {
      const lista = todos[conversacionId];
      if (!lista) return todos;
      return {
        ...todos,
        [conversacionId]: lista.map((m) =>
          m.clienteId === clienteId && m.estadoEnvio !== 'enviado' && (!soloPendiente || m.estadoEnvio !== estado)
            ? { ...m, estadoEnvio: estado }
            : m,
        ),
      };
    });
  }

  /** Vista previa y actividad de la conversación; la más reciente sube al principio. */
  private registrarActividad(mensaje: MensajeChat): void {
    this._conversaciones.update((lista) => {
      const actual = lista.find((c) => c.id === mensaje.conversacionId);
      if (!actual) return lista;
      const actualizada: ConversacionChat = {
        ...actual,
        vistaPrevia: mensaje.contenido.slice(0, LONGITUD_VISTA_PREVIA),
        ultimaActividad: mensaje.enviadoEn,
      };
      return [actualizada, ...lista.filter((c) => c.id !== mensaje.conversacionId)];
    });
  }

  private ponerNoLeidos(conversacionId: string, cantidad: number): void {
    this._conversaciones.update((lista) =>
      lista.map((c) => (c.id === conversacionId && c.noLeidos !== cantidad ? { ...c, noLeidos: cantidad } : c)),
    );
  }

  private vaciar(): void {
    this.activa = null;
    this._conversaciones.set([]);
    this._mensajes.set({});
    this._hayMasAnteriores.set({});
    this._estado.set('desconectado');
  }
}
