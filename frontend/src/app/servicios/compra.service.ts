import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';
import { Compra, MovimientoPuntos } from '../modelos/modelos';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private http = inject(HttpClient);

  listar() { return this.http.get<Compra[]>(`${API_URL}/compras`); }
  realizar(productoId: string, cantidad: number) {
    return this.http.post<Compra>(`${API_URL}/compras`, { productoId, cantidad });
  }
  confirmar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/confirmar`, {}); }
  cancelar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/cancelar`, {}); }
  reembolsar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/reembolso`, {}); }
  aplicarCupon(id: string, codigo: string) {
    return this.http.put(`${API_URL}/compras/${id}/cupon`, { codigo });
  }
  resenar(idCompra: string, valorCalificacion: number, comentario: string) {
    return this.http.post(`${API_URL}/resenas`, { idCompra, valorCalificacion, comentario });
  }
  valorarVendedor(compraId: string, estrellas: number, comentario: string) {
    return this.http.post(`${API_URL}/valoraciones-vendedor`, { compraId, estrellas, comentario });
  }
  historialPuntos() { return this.http.get<MovimientoPuntos[]>(`${API_URL}/puntos/historial`); }
  redimirPuntos(cantidad: number) {
    return this.http.post<{ usuarioId: string; saldo: number }>(`${API_URL}/puntos/redimir`, { cantidad });
  }
}
