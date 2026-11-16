import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config';
import { Notificacion } from '../modelos/modelos';
import { AuthService } from './auth.service';

/**
 * Notificaciones de la sesión activa, sea COMPRADOR (descuentos, stock y preventas de su wishlist,
 * metas y regalos) o VENDEDOR (stock bajo, ventas, reembolsos, reseñas y valoraciones).
 * Carga sola al iniciar sesión y se vacía al cambiar de usuario.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private _notificaciones = signal<Notificacion[]>([]);

  readonly notificaciones = this._notificaciones.asReadonly();
  readonly noLeidas = computed(() => this._notificaciones().filter((n) => !n.leida).length);

  // computed compara por valor: el efecto solo corre si cambia de verdad la persona (no por renovar el token).
  private usuarioId = computed(() => this.auth.sesion()?.id ?? null);

  constructor() {
    effect(() => {
      const id = this.usuarioId();
      untracked(() => {
        this._notificaciones.set([]);
        if (id) this.cargar().subscribe({ error: () => undefined });
      });
    });
  }

  /** GET /notificaciones: reemplaza la lista local (más reciente primero, como la manda el backend). */
  cargar(): Observable<Notificacion[]> {
    const solicitante = this.usuarioId();
    return this.http.get<Notificacion[]>(`${API_URL}/notificaciones`).pipe(
      tap((lista) => {
        // Si la sesión cambió mientras llegaba la respuesta, se descarta para no mostrar datos de otra persona.
        if (this.usuarioId() === solicitante) this._notificaciones.set(lista);
      }),
    );
  }

  /** PUT /notificaciones/{id}/leida: actualiza la notificación en la lista local con la respuesta del servidor. */
  marcarLeida(id: string): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${API_URL}/notificaciones/${id}/leida`, {}).pipe(
      tap((actualizada) =>
        this._notificaciones.update((lista) => lista.map((n) => (n.id === actualizada.id ? actualizada : n))),
      ),
    );
  }
}
