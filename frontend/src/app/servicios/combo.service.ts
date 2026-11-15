import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { Combo, ComboCreado, ModificarCombo, NuevoCombo, ResultadoCompraCombo } from '../modelos/modelos';

/** Acceso a la API de combos: catálogo para compradores y gestión para el vendedor dueño. */
@Injectable({ providedIn: 'root' })
export class ComboService {
  private http = inject(HttpClient);

  /** GET /combos: combos comprables (todos sus productos visibles). */
  listar(): Observable<Combo[]> {
    return this.http.get<Combo[]>(`${API_URL}/combos`);
  }

  /** GET /combos/mios: combos del vendedor autenticado (incluye los que tienen productos pausados). */
  mios(): Observable<Combo[]> {
    return this.http.get<Combo[]>(`${API_URL}/combos/mios`);
  }

  crear(c: NuevoCombo): Observable<ComboCreado> {
    return this.http.post<ComboCreado>(`${API_URL}/combos`, c);
  }

  agregarProducto(id: string, productoId: string): Observable<ComboCreado> {
    return this.http.put<ComboCreado>(`${API_URL}/combos/${id}/productos/${productoId}`, {});
  }

  quitarProducto(id: string, productoId: string): Observable<ComboCreado> {
    return this.http.delete<ComboCreado>(`${API_URL}/combos/${id}/productos/${productoId}`);
  }

  /** PUT /combos/{id}: cambia nombre, descripción y descuento (los productos se gestionan aparte). */
  modificar(id: string, c: ModificarCombo): Observable<ComboCreado> {
    return this.http.put<ComboCreado>(`${API_URL}/combos/${id}`, c);
  }

  /** DELETE /combos/{id}: elimina el combo (422 si tiene regalos pendientes). */
  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${API_URL}/combos/${id}`);
  }

  /** POST /combos/{id}/comprar: crea una compra PENDIENTE por producto del combo (descuento ya aplicado). */
  comprar(id: string, cantidad: number): Observable<ResultadoCompraCombo> {
    return this.http.post<ResultadoCompraCombo>(`${API_URL}/combos/${id}/comprar`, { cantidad });
  }
}
