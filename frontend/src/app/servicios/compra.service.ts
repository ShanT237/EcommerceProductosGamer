import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';
import { Compra, MovimientoPuntos, Pedido } from '../modelos/modelos';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private http = inject(HttpClient);

  listar() { return this.http.get<Compra[]>(`${API_URL}/compras`); }
  realizar(productoId: string, cantidad: number) {
    return this.http.post<Compra>(`${API_URL}/compras`, { productoId, cantidad });
  }
  /** POST /pedidos. El cupón es opcional y consume un solo uso por pedido; sin código el campo no se envía. */
  crearPedido(items: Array<{ productoId: string; cantidad: number }>, codigoCupon?: string) {
    const cupon = codigoCupon?.trim();
    return this.http.post<Pedido>(`${API_URL}/pedidos`, cupon ? { items, codigoCupon: cupon } : { items });
  }
  confirmarPedido(id: string) { return this.http.put<Pedido>(`${API_URL}/pedidos/${id}/confirmar`, {}); }
  cancelarPedido(id: string) { return this.http.put<Pedido>(`${API_URL}/pedidos/${id}/cancelar`, {}); }
  reembolsarPedido(id: string) { return this.http.put<Pedido>(`${API_URL}/pedidos/${id}/reembolso`, {}); }
  confirmar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/confirmar`, {}); }
  cancelar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/cancelar`, {}); }
  /** PUT /compras/{id}/descarga: marca la compra COMPLETADA de un producto digital como descargada (bloquea el reembolso). */
  descargar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/descarga`, {}); }
  reembolsar(id: string) { return this.http.put<Compra>(`${API_URL}/compras/${id}/reembolso`, {}); }
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
