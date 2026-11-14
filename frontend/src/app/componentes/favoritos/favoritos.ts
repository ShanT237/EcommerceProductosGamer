import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Producto } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { ToastService } from '../../servicios/toast.service';
import { WishlistService } from '../../servicios/wishlist.service';
import { TarjetaProducto } from '../tarjeta-producto/tarjeta-producto';

/** Pantalla `/favoritos`: los productos que el comprador guardó en su lista de deseos. */
@Component({
  selector: 'app-favoritos',
  imports: [RouterLink, TarjetaProducto],
  templateUrl: './favoritos.html',
  styleUrl: './favoritos.css',
})
export class Favoritos {
  protected wishlist = inject(WishlistService);
  private toast = inject(ToastService);
  private router = inject(Router);

  protected quitar(p: Producto) {
    this.wishlist.alternar(p).subscribe({
      next: () => this.toast.ok('Quitado de tus favoritos.'),
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  protected reintentar() {
    this.wishlist.cargar().subscribe({ error: () => undefined });
  }

  protected ver(p: Producto) {
    this.router.navigate(['/catalogo'], { queryParams: { producto: p.id } });
  }
}
