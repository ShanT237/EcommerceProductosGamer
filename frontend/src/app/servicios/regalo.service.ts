import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { NuevoRegalo, ProgresoRegalos, Regalo } from '../modelos/modelos';

/** Acceso a la API de regalos: el comprador los elige y confirma; el vendedor los crea y envía. */
@Injectable({ providedIn: 'root' })
export class RegaloService {
  private http = inject(HttpClient);

  /** GET /regalos/progreso: compras completadas, regalos por elegir y cuántas compras faltan. */
  progreso(): Observable<ProgresoRegalos> {
    return this.http.get<ProgresoRegalos>(`${API_URL}/regalos/progreso`);
  }

  /** GET /regalos/disponibles: regalos que se pueden elegir (estado DISPONIBLE). */
  disponibles(): Observable<Regalo[]> {
    return this.http.get<Regalo[]>(`${API_URL}/regalos/disponibles`);
  }

  /** GET /regalos/mios: regalos que el comprador ya eligió. */
  mios(): Observable<Regalo[]> {
    return this.http.get<Regalo[]>(`${API_URL}/regalos/mios`);
  }

  /** GET /regalos/por-enviar: regalos ELEGIDOS de los productos o combos del vendedor. */
  porEnviar(): Observable<Regalo[]> {
    return this.http.get<Regalo[]>(`${API_URL}/regalos/por-enviar`);
  }

  /** POST /regalos (vendedor): ofrece un regalo de un producto o combo propio. */
  crear(r: NuevoRegalo): Observable<Regalo> {
    return this.http.post<Regalo>(`${API_URL}/regalos`, r);
  }

  /** PUT /regalos/{id}/asignar (comprador): elige el regalo (422 si aún no alcanza la meta). */
  asignar(id: string): Observable<Regalo> {
    return this.http.put<Regalo>(`${API_URL}/regalos/${id}/asignar`, {});
  }

  /** PUT /regalos/{id}/enviar (vendedor dueño): pasa el regalo de ELEGIDO a ENVIADO. */
  enviar(id: string): Observable<Regalo> {
    return this.http.put<Regalo>(`${API_URL}/regalos/${id}/enviar`, {});
  }

  /** PUT /regalos/{id}/cancelar (vendedor dueño): cancela el regalo desde cualquier estado previo a ENTREGADO. */
  cancelar(id: string): Observable<Regalo> {
    return this.http.put<Regalo>(`${API_URL}/regalos/${id}/cancelar`, {});
  }

  /** PUT /regalos/{id}/entrega (comprador destinatario): confirma que lo recibió. */
  confirmarEntrega(id: string): Observable<Regalo> {
    return this.http.put<Regalo>(`${API_URL}/regalos/${id}/entrega`, {});
  }
}
