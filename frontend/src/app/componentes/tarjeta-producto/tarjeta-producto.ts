import { Component, ElementRef, inject, input, output } from '@angular/core';
import { Producto } from '../../modelos/modelos';
import { precio } from '../../servicios/formato';
import { ArteProducto } from '../arte-producto/arte-producto';

/** Tarjeta del catálogo con inclinación 3D que sigue al mouse y un brillo que lo acompaña. */
@Component({
  selector: 'app-tarjeta-producto',
  imports: [ArteProducto],
  templateUrl: './tarjeta-producto.html',
  styleUrl: './tarjeta-producto.css',
})
export class TarjetaProducto {
  producto = input.required<Producto>();
  ver = output<Producto>();

  private elemento = inject<ElementRef<HTMLElement>>(ElementRef);
  protected precio = precio;

  protected inclinar(e: MouseEvent) {
    const caja = this.elemento.nativeElement.getBoundingClientRect();
    const x = (e.clientX - caja.left) / caja.width;
    const y = (e.clientY - caja.top) / caja.height;
    const el = this.elemento.nativeElement;
    el.style.setProperty('--ry', `${(x - 0.5) * 14}deg`);
    el.style.setProperty('--rx', `${(0.5 - y) * 14}deg`);
    el.style.setProperty('--mx', `${x * 100}%`);
    el.style.setProperty('--my', `${y * 100}%`);
  }

  protected soltar() {
    const el = this.elemento.nativeElement;
    el.style.setProperty('--ry', '0deg');
    el.style.setProperty('--rx', '0deg');
  }
}
