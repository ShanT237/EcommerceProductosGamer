import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { Cupon, NuevoCupon } from '../modelos/modelos';

/**
 * Acceso a la API de cupones: el comprador consulta un código antes de pagar (vista previa, no gasta ningún uso)
 * y el vendedor crea y lista los suyos. El cupón se consume al crear el pedido (`POST /pedidos`).
 */
@Injectable({ providedIn: 'root' })
export class CuponService {
  private http = inject(HttpClient);

  /** GET /cupones/{codigo} (COMPRADOR): 404 si no existe; 422 si está vencido, agotado o ya lo usó esta persona. */
  consultar(codigo: string): Observable<Cupon> {
    return this.http.get<Cupon>(`${API_URL}/cupones/${encodeURIComponent(codigo.trim())}`);
  }

  /** GET /cupones (VENDEDOR): los cupones del vendedor autenticado. */
  misCupones(): Observable<Cupon[]> {
    return this.http.get<Cupon[]>(`${API_URL}/cupones`);
  }

  /** POST /cupones (VENDEDOR): crea un cupón que solo descuenta los productos del vendedor (409 si el código ya existe). */
  crear(c: NuevoCupon): Observable<Cupon> {
    return this.http.post<Cupon>(`${API_URL}/cupones`, c);
  }
}
