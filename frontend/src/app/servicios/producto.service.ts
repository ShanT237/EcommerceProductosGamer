import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_URL } from '../config';
import { FiltroCatalogo, NuevoProducto, Pagina, Producto } from '../modelos/modelos';

@Injectable({ providedIn: 'root' })
export class ProductoService {
  private http = inject(HttpClient);

  /** Catálogo completo en memoria (sirve para mostrar nombres en compras y "mis productos"). */
  readonly catalogo = signal<Producto[]>([]);

  buscar(filtro: FiltroCatalogo, pagina = 0, tamano = 8) {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (filtro.texto?.trim()) params = params.set('texto', filtro.texto.trim());
    if (filtro.categoria?.trim()) params = params.set('categoria', filtro.categoria.trim());
    if (filtro.gama) params = params.set('gama', filtro.gama);
    if (filtro.precioMin != null) params = params.set('precioMin', filtro.precioMin);
    if (filtro.precioMax != null) params = params.set('precioMax', filtro.precioMax);
    if (filtro.soloPreventa) params = params.set('soloPreventa', true);
    return this.http.get<Pagina<Producto>>(`${API_URL}/productos`, { params });
  }

  cargarCatalogo() {
    return this.buscar({}, 0, 100).pipe(tap((p) => this.catalogo.set(p.contenido)));
  }

  publicar(p: NuevoProducto) {
    return this.http.post<Producto>(`${API_URL}/productos`, p);
  }

  modificarPrecio(id: string, nuevoPrecio: number) {
    return this.http.put(`${API_URL}/productos/${id}/precio`, { nuevoPrecio });
  }

  editarInformacion(id: string, nombre: string, descripcion: string) {
    return this.http.put(`${API_URL}/productos/${id}/informacion`, { nombre, descripcion });
  }

  cambiarVisibilidad(id: string, activo: boolean) {
    return this.http.put(`${API_URL}/productos/${id}/visibilidad`, { activo });
  }

  eliminar(id: string) {
    return this.http.delete<void>(`${API_URL}/productos/${id}`);
  }

  anadirWishlist(productoId: string) {
    return this.http.post(`${API_URL}/wishlists`, { productoId });
  }

  quitarWishlist(productoId: string) {
    return this.http.delete(`${API_URL}/wishlists/${productoId}`);
  }

  configurarPromocion(productoId: string, porcentaje: number, desde: string, hasta: string) {
    return this.http.post(`${API_URL}/promociones`, { productoId, porcentaje, desde, hasta });
  }

  aplicarDescuento(productoId: string, porcentaje: number) {
    return this.http.post<{ id: string; nombre: string; nuevoPrecio: number }>(`${API_URL}/descuentos`, { productoId, porcentaje });
  }
}
