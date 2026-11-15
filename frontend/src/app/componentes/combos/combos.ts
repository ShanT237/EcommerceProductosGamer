import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Combo } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { AuthService } from '../../servicios/auth.service';
import { ComboService } from '../../servicios/combo.service';
import { precio } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';

/** Pantalla `/combos`: combos con descuento. El comprador elige cantidad y los reserva; el vendedor solo los ve. */
@Component({
  selector: 'app-combos',
  templateUrl: './combos.html',
  styleUrl: './combos.css',
})
export class Combos implements OnInit {
  private servicio = inject(ComboService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private router = inject(Router);
  protected precio = precio;

  protected combos = signal<Combo[]>([]);
  protected cargando = signal(true);
  protected error = signal('');
  /** Cantidad elegida por combo (1 por defecto). */
  protected cantidades = signal<Record<string, number>>({});
  /** Combo que se está comprando (evita dobles clics). */
  protected comprando = signal<string | null>(null);

  protected esComprador = computed(() => this.auth.sesion()?.rol === 'COMPRADOR');

  ngOnInit() {
    this.cargar();
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.listar().subscribe({
      next: (l) => { this.combos.set(l); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  /** Unidades máximas que se pueden pedir: las del producto con menos stock. */
  protected limite(c: Combo): number {
    return c.productos.length ? Math.min(...c.productos.map((p) => p.stock)) : 0;
  }

  protected cantidadDe(c: Combo): number {
    return this.cantidades()[c.id] ?? 1;
  }

  protected cambiarCantidad(c: Combo, delta: number) {
    const nueva = Math.min(Math.max(1, this.cantidadDe(c) + delta), Math.max(1, this.limite(c)));
    this.cantidades.update((m) => ({ ...m, [c.id]: nueva }));
  }

  protected ahorro(c: Combo): number {
    return c.precioSinDescuento - c.precioConDescuento;
  }

  protected comprar(c: Combo) {
    if (this.comprando() || this.limite(c) <= 0) return;
    this.comprando.set(c.id);
    this.servicio.comprar(c.id, this.cantidadDe(c)).subscribe({
      next: (r) => {
        this.comprando.set(null);
        this.toast.ok(`Reservaste el combo "${c.nombre}" (${r.compras.length} productos). Confírmalo en Mis compras.`);
        this.router.navigateByUrl('/mis-compras');
      },
      error: (e) => { this.comprando.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
