import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Combo, Cupon } from '../../modelos/modelos';
import { MENSAJE_INGRESAR_PARA_COMPRAR, mensajeDeError } from '../../servicios/api.util';
import { AuthService } from '../../servicios/auth.service';
import { ComboService } from '../../servicios/combo.service';
import { CuponService } from '../../servicios/cupon.service';
import { precio } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';

/** Pantalla `/combos`: combos con descuento. El comprador elige cantidad y los reserva; el vendedor solo los ve. */
@Component({
  selector: 'app-combos',
  imports: [FormsModule],
  templateUrl: './combos.html',
  styleUrl: './combos.css',
})
export class Combos implements OnInit {
  private servicio = inject(ComboService);
  private cupones = inject(CuponService);
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
  protected codigosCupon = signal<Record<string, string>>({});
  protected cuponesAplicados = signal<Record<string, Cupon>>({});
  protected erroresCupon = signal<Record<string, string>>({});
  protected validandoCupon = signal<string | null>(null);

  protected esComprador = computed(() => this.auth.sesion()?.rol === 'COMPRADOR');
  /** Visitante: ve los combos sin sesión; al intentar comprar se le pide ingresar. */
  protected esVisitante = computed(() => this.auth.sesion() === null);

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
    return c.precioSinDescuento - this.precioConCupon(c);
  }

  protected codigoCupon(c: Combo): string {
    return this.codigosCupon()[c.id] ?? '';
  }

  protected cambiarCodigoCupon(c: Combo, codigo: string) {
    this.codigosCupon.update((codigos) => ({ ...codigos, [c.id]: codigo }));
    this.erroresCupon.update((errores) => ({ ...errores, [c.id]: '' }));
    this.cuponesAplicados.update((aplicados) => {
      const actualizados = { ...aplicados };
      delete actualizados[c.id];
      return actualizados;
    });
  }

  protected cuponAplicado(c: Combo): Cupon | null {
    return this.cuponesAplicados()[c.id] ?? null;
  }

  protected precioConCupon(c: Combo): number {
    const cupon = this.cuponAplicado(c);
    return cupon ? c.precioConDescuento - (c.precioConDescuento * cupon.porcentaje / 100)
      : c.precioConDescuento;
  }

  protected aplicarCupon(c: Combo) {
    const codigo = this.codigoCupon(c).trim();
    this.erroresCupon.update((errores) => ({ ...errores, [c.id]: '' }));
    if (!codigo || this.validandoCupon() || this.comprando()) return;

    this.validandoCupon.set(c.id);
    this.cupones.consultar(codigo).subscribe({
      next: (cupon) => {
        this.validandoCupon.set(null);
        if (cupon.vendedorId && cupon.vendedorId !== c.vendedorId) {
          this.erroresCupon.update((errores) => ({
            ...errores,
            [c.id]: 'Ese cupón pertenece a otro vendedor y no aplica a este combo.',
          }));
          return;
        }
        this.cuponesAplicados.update((aplicados) => ({ ...aplicados, [c.id]: cupon }));
      },
      error: (e) => {
        this.validandoCupon.set(null);
        this.erroresCupon.update((errores) => ({ ...errores, [c.id]: mensajeDeError(e) }));
      },
    });
  }

  protected quitarCupon(c: Combo) {
    this.cambiarCodigoCupon(c, '');
  }

  protected comprar(c: Combo) {
    if (this.esVisitante()) {
      this.toast.info(MENSAJE_INGRESAR_PARA_COMPRAR);
      return;
    }
    if (this.comprando() || this.limite(c) <= 0) return;
    this.comprando.set(c.id);
    this.servicio.comprar(c.id, this.cantidadDe(c), this.cuponAplicado(c)?.codigo).subscribe({
      next: (r) => {
        this.comprando.set(null);
        const total = this.precioConCupon(c) * this.cantidadDe(c);
        this.toast.ok(`Reservaste el combo "${c.nombre}" por ${this.precio(total)} (${r.compras.length} productos). Confírmalo en Mis compras.`);
        this.router.navigateByUrl('/mis-compras');
      },
      error: (e) => { this.comprando.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}