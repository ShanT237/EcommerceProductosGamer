import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Cupon } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { CarritoService, ItemCarrito } from '../../servicios/carrito.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { CuponService } from '../../servicios/cupon.service';
import { precio, precioTachado, precioVigente } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';
import { ArteProducto } from '../arte-producto/arte-producto';

/** Pantalla `/carrito`: revisa los productos, aplica un cupón (vista previa) y ejecuta el pago simulado. */
@Component({
  selector: 'app-carrito',
  imports: [FormsModule, RouterLink, ArteProducto],
  templateUrl: './carrito.html',
  styleUrl: './carrito.css',
})
export class Carrito {
  protected carrito = inject(CarritoService);
  private cupones = inject(CuponService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  private router = inject(Router);
  protected precio = precio;
  protected precioVigente = precioVigente;
  protected precioTachado = precioTachado;

  // ---- cupón: "Aplicar" solo consulta (vista previa); el uso se gasta al crear el pedido al pagar ----
  protected codigoCupon = signal('');
  protected cupon = signal<Cupon | null>(null);
  protected errorCupon = signal('');
  protected consultandoCupon = signal(false);

  /** Un cupón global descuenta todo el carrito; uno con dueño, solo los productos de ese vendedor. */
  private aplicaA(c: Cupon, i: ItemCarrito): boolean {
    return !c.vendedorId || i.producto.vendedorId === c.vendedorId;
  }

  /** Descuento estimado (el backend calcula el definitivo al crear el pedido). */
  protected descuento = computed(() => {
    const c = this.cupon();
    if (!c) return 0;
    const base = this.carrito
      .items()
      .filter((i) => this.aplicaA(c, i))
      .reduce((suma, i) => suma + precioVigente(i.producto) * i.cantidad, 0);
    return Math.round((base * c.porcentaje) / 100);
  });
  protected total = computed(() => this.carrito.subtotal() - this.descuento());
  /** El cupón ya no descuenta ningún producto del carrito (p. ej. se quitó lo del vendedor dueño): no se envía al pagar. */
  protected cuponSinEfecto = computed(() => {
    const c = this.cupon();
    return !!c && !this.carrito.items().some((i) => this.aplicaA(c, i));
  });

  protected aplicarCupon() {
    const codigo = this.codigoCupon().trim();
    this.errorCupon.set('');
    if (!codigo) return this.errorCupon.set('Escribe el código del cupón.');
    if (this.consultandoCupon() || this.carrito.pagando()) return;

    this.consultandoCupon.set(true);
    this.cupones.consultar(codigo).subscribe({
      next: (c) => {
        this.consultandoCupon.set(false);
        if (!this.carrito.items().some((i) => this.aplicaA(c, i))) {
          this.errorCupon.set('Ese cupón no aplica a ningún producto de tu carrito: es de otro vendedor.');
          return;
        }
        this.cupon.set(c);
      },
      error: (e) => {
        this.consultandoCupon.set(false);
        this.errorCupon.set(mensajeDeError(e));
      },
    });
  }

  protected quitarCupon() {
    this.cupon.set(null);
    this.codigoCupon.set('');
    this.errorCupon.set('');
  }

  protected async pagar() {
    if (this.carrito.pagando() || this.carrito.items().length === 0) return;
    const cupon = this.cupon();
    const codigo = cupon && !this.cuponSinEfecto() ? cupon.codigo : undefined;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Confirmar pago',
      mensaje: `Vas a pagar ${this.carrito.unidades()} unidad(es) por ${precio(this.total())}${codigo ? ` (con el cupón ${codigo})` : ''}. Es un pago simulado.`,
      confirmar: 'Pagar',
      peligro: false,
    });
    if (!acepto) return;
    this.carrito.pagar(codigo).subscribe((r) => {
      if (!r.error) {
        this.toast.ok(`¡Compra simulada exitosa! ${r.compras.length} producto(s) pagado(s).`);
        this.quitarCupon();
        this.router.navigateByUrl('/mis-compras');
        return;
      }
      if (r.pedidoSinCancelar) {
        // La cancelación también falló: no se puede asegurar que el stock se liberó ni que el pedido no siga pendiente.
        this.toast.error(
          `No se pudo completar el pago: ${r.error}. Además no se pudo cancelar el pedido: puede seguir pendiente con el stock reservado. ` +
            'Reintenta la cancelación o revísalo en Mis compras.',
        );
        return;
      }
      this.toast.error(`No se pudo completar el pago: ${r.error}. No se cobró nada y tu carrito sigue igual.`);
    });
  }

  protected reintentarCancelacion() {
    this.carrito.reintentarCancelacion().subscribe((desenlace) => {
      if (desenlace === 'CANCELADO') {
        this.toast.ok('Pedido cancelado y stock liberado. Tu carrito sigue igual: puedes volver a pagar.');
      } else if (desenlace === 'PAGADO') {
        this.toast.ok('El pedido sí se había confirmado: tu compra quedó pagada.');
        this.quitarCupon();
        this.router.navigateByUrl('/mis-compras');
      } else {
        this.toast.error('Sigue sin poder cancelarse el pedido. Inténtalo de nuevo o revísalo en Mis compras.');
      }
    });
  }
}