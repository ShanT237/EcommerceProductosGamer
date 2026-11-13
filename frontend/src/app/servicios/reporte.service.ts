import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';
import { MetricasResenas, ProductoStockBajo, ProductoVendido, ReporteGlobal, ReporteVentas } from '../modelos/modelos';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private http = inject(HttpClient);

  ventas() { return this.http.get<ReporteVentas>(`${API_URL}/reportes/ventas`); }
  metricasResenas() { return this.http.get<MetricasResenas>(`${API_URL}/reportes/metricas-resenas`); }
  masVendidos(top = 5, vendedorId?: string) {
    let params = new HttpParams().set('top', top);
    if (vendedorId) params = params.set('vendedorId', vendedorId);
    return this.http.get<ProductoVendido[]>(`${API_URL}/reportes/mas-vendidos`, { params });
  }
  stockBajo(umbral = 5) {
    return this.http.get<ProductoStockBajo[]>(`${API_URL}/reportes/stock-bajo`, { params: new HttpParams().set('umbral', umbral) });
  }
  global() { return this.http.get<ReporteGlobal>(`${API_URL}/reportes/global`); }
}
