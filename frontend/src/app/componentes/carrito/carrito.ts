import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CarritoService } from '../../servicios/carrito.service';
import { precio } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';
import { ArteProducto } from '../arte-producto/arte-producto';

/** Pantalla `/carrito`: revisa los productos y ejecuta el pago simulado. */
@Component({
  selector: 'app-carrito',
  imports: [RouterLink, ArteProducto],
  templateUrl: './carrito.html',
  styleUrl: './carrito.css',
})
export class Carrito {
  protected carrito = inject(CarritoService);
  private toast = inject(ToastService);
  private router = inject(Router);
  protected precio = precio;

  protected pagar() {
    this.carrito.pagar().subscribe((r) => {
      if (!r.fallo) {
        this.toast.ok(`¡Compra simulada exitosa! ${r.completadas.length} producto(s) pagado(s).`);
        this.router.navigateByUrl('/mis-compras');
        return;
      }
      const anteriores = r.completadas.length > 0 ? ' Los pagos anteriores quedaron completados.' : '';
      this.toast.error(`No se pudo comprar "${r.fallo.producto.nombre}": ${r.fallo.mensaje}.${anteriores}`);
    });
  }
}
