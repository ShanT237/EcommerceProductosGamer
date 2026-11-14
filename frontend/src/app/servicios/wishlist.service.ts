import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, catchError, defer, finalize, map, of, tap, throwError } from 'rxjs';
import { API_URL } from '../config';
import { Producto } from '../modelos/modelos';
import { mensajeDeError } from './api.util';
import { AuthService } from './auth.service';

/**
 * Favoritos del comprador. La fuente de verdad es el backend (`GET /api/wishlists`): el servicio
 * los carga solo cuando la sesión es de un COMPRADOR y se vacía al cambiar de usuario, así el
 * corazón del catálogo sigue marcado después de recargar la página.
 */
@Injectable({ providedIn: 'root' })
export class WishlistService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private _productos = signal<Producto[]>([]);
  private _ids = signal<ReadonlySet<string>>(new Set());
  private _cargando = signal(false);
  private _error = signal('');

  readonly productos = this._productos.asReadonly();
  readonly ids = this._ids.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  // computed compara por valor: el efecto solo corre si cambia de verdad la persona (no por renovar el token).
  private compradorId = computed(() => {
    const sesion = this.auth.sesion();
    return sesion?.rol === 'COMPRADOR' ? sesion.id : null;
  });

  constructor() {
    effect(() => {
      const id = this.compradorId();
      untracked(() => {
        this.fijar([]);
        this._error.set('');
        if (id) this.cargar().subscribe({ error: () => undefined });
      });
    });
  }

  esFavorito(id: string): boolean {
    return this._ids().has(id);
  }

  cargar(): Observable<Producto[]> {
    return defer(() => {
      this._cargando.set(true);
      this._error.set('');
      return this.http.get<Producto[]>(`${API_URL}/wishlists`).pipe(
        tap((lista) => this.fijar(lista)),
        tap({ error: (e) => this._error.set(mensajeDeError(e)) }),
        finalize(() => this._cargando.set(false)),
      );
    });
  }

  /** Añade el producto a favoritos si no lo está, o lo quita si ya lo estaba. */
  alternar(p: Producto): Observable<void> {
    if (this.esFavorito(p.id)) {
      return this.http.delete(`${API_URL}/wishlists/${p.id}`).pipe(
        tap(() => this.fijar(this._productos().filter((x) => x.id !== p.id))),
        map(() => undefined),
      );
    }
    return this.http.post(`${API_URL}/wishlists`, { productoId: p.id }).pipe(
      // 409 = el backend ya lo tenía como favorito: el estado local solo estaba desactualizado.
      catchError((e: HttpErrorResponse) => (e.status === 409 ? of(null) : throwError(() => e))),
      tap(() => this.fijar([...this._productos().filter((x) => x.id !== p.id), p])),
      map(() => undefined),
    );
  }

  private fijar(lista: Producto[]) {
    this._productos.set(lista);
    this._ids.set(new Set(lista.map((p) => p.id)));
  }
}
