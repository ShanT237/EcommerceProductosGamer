import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { MetaRecompensa, NuevaMeta } from '../modelos/modelos';

/** Acceso a la API de metas de recompensa: cualquier sesión las lee; el vendedor las gestiona. */
@Injectable({ providedIn: 'root' })
export class MetaService {
  private http = inject(HttpClient);

  /** GET /metas-recompensa: solo las metas activas, de menor a mayor umbral. */
  listar(): Observable<MetaRecompensa[]> {
    return this.http.get<MetaRecompensa[]>(`${API_URL}/metas-recompensa`);
  }

  /** POST /metas-recompensa (vendedor): crea una meta activa. */
  crear(m: NuevaMeta): Observable<MetaRecompensa> {
    return this.http.post<MetaRecompensa>(`${API_URL}/metas-recompensa`, m);
  }

  /** PUT /metas-recompensa/{id}/umbral (vendedor): cambia cuántas compras exige la meta. */
  actualizarUmbral(id: string, nuevoUmbral: number): Observable<MetaRecompensa> {
    return this.http.put<MetaRecompensa>(`${API_URL}/metas-recompensa/${id}/umbral`, { nuevoUmbral });
  }

  /** PUT /metas-recompensa/{id}/desactivar (vendedor): la meta deja de estar activa (nunca se borra). */
  desactivar(id: string): Observable<MetaRecompensa> {
    return this.http.put<MetaRecompensa>(`${API_URL}/metas-recompensa/${id}/desactivar`, {});
  }
}
