import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { API_URL } from '../config';
import { MensajeAsistente, RespuestaAsistente } from '../modelos/modelos';
import { AuthService } from './auth.service';

/** El backend rechaza (400) un historial de más de 10 mensajes. */
const MAX_HISTORIAL = 10;

/** Chat con el asistente de IA: el endpoint depende del rol de la sesión (comprador o vendedor). */
@Injectable({ providedIn: 'root' })
export class AsistenteService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  preguntar(mensaje: string, historial: MensajeAsistente[]): Observable<RespuestaAsistente> {
    const rol = this.auth.rol();
    if (!rol) return throwError(() => new Error('Sin sesión'));
    const destino = rol === 'VENDEDOR' ? 'vendedor' : 'comprador';
    return this.http.post<RespuestaAsistente>(`${API_URL}/asistente/${destino}`, {
      mensaje,
      historial: historial.slice(-MAX_HISTORIAL),
    });
  }
}
