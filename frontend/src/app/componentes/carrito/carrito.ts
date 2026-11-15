import { Component, inject } from '@angular/core';
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

  protected async pagar() {
    if (this.carrito.pagando() || this.carrito.items().length === 0) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Confirmar pago',
      mensaje: `Vas a pagar ${this.carrito.unidades()} unidad(es) por ${precio(this.carrito.subtotal())}. Es un pago simulado.`,
      confirmar: 'Pagar',
      peligro: false,
    });
    if (!acepto) return;
    this.carrito.pagar().subscribe((r) => {
      if (!r.error) {
        this.toast.ok(`¡Compra simulada exitosa! ${r.compras.length} producto(s) pagado(s).`);
        this.router.navigateByUrl('/mis-compras');
        return;
      }
      this.toast.error(`No se pudo completar el pago: ${r.error}. No se cobró nada y tu carrito sigue igual.`);
    });
  }
}
