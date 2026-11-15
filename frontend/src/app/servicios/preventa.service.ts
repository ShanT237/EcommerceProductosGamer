import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config';
import { Preventa } from '../modelos/modelos';

@Injectable({ providedIn: 'root' })
export class PreventaService {
  private http = inject(HttpClient);

  readonly futuras = signal<ReadonlyMap<string, Preventa>>(new Map());
  readonly cargando = signal(false);
  readonly cargada = signal(false);

  cargar(): Observable<Preventa[]> {
    this.cargando.set(true);
    this.cargada.set(false);
    return this.http.get<Preventa[]>(`${API_URL}/preventas`).pipe(
      tap({
        next: (preventas) => {
          this.futuras.set(new Map(preventas.map((preventa) => [preventa.productoId, preventa])));
          this.cargada.set(true);
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      }),
    );
  }

  programar(productoId: string, fechaLanzamiento: string, descripcion: string): Observable<Preventa> {
    return this.http.post<Preventa>(`${API_URL}/preventas`, {
      productoId,
      fechaLanzamiento,
      descripcion,
    }).pipe(tap((preventa) => {
      this.futuras.update((actuales) => new Map(actuales).set(preventa.productoId, preventa));
      this.cargada.set(true);
    }));
  }
}
