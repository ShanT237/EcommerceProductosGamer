import {Component, inject, signal} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CarritoService } from '../../servicios/carrito.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { precio, precioTachado, precioVigente } from '../../servicios/formato';
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
  private confirmacion = inject(ConfirmacionService);
  private router = inject(Router);
  protected precio = precio;
  protected precioVigente = precioVigente;
  protected precioTachado = precioTachado;
  protected cupon = signal('');

  protected async pagar() {
    if (this.carrito.pagando() || this.carrito.items().length === 0) return;
    const cupon = this.cupon().trim().toUpperCase();
    const acepto = await this.confirmacion.pedir({
      titulo: 'Confirmar pago',
      mensaje:
        `Vas a pagar ${this.carrito.unidades()} unidad(es) por ${precio(this.carrito.subtotal())}` +
        (cupon ? ` con el cupón ${cupon}` : '') +
        '. Es un pago simulado.',
      confirmar: 'Pagar',
      peligro: false,
    });
    if (!acepto) return;
    this.carrito.pagar(cupon || undefined).subscribe((r) => {
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