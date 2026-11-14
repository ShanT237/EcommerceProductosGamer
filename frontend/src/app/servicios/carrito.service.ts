import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { EMPTY, Observable, catchError, concatMap, defer, finalize, from, map, of, switchMap, takeWhile, tap, toArray } from 'rxjs';
import { Compra, Producto } from '../modelos/modelos';
import { mensajeDeError } from './api.util';
import { AuthService } from './auth.service';
import { CompraService } from './compra.service';

export interface ItemCarrito { producto: Producto; cantidad: number }
export interface FalloPago { producto: Producto; mensaje: string }
export interface ResultadoPago { completadas: Compra[]; fallo: FalloPago | null }

type IntentoOk = { ok: true; item: ItemCarrito; compra: Compra };
type IntentoFallido = { ok: false; item: ItemCarrito; mensaje: string };
type Intento = IntentoOk | IntentoFallido;

/**
 * Carrito de compra simulado. Vive solo en el frontend (no persiste al recargar).
 * Pagar = crear y confirmar cada compra con los endpoints existentes, ítem por ítem y en orden.
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  private compras = inject(CompraService);
  private auth = inject(AuthService);

  private _items = signal<ItemCarrito[]>([]);
  private _pagando = signal(false);

  readonly items = this._items.asReadonly();
  readonly pagando = this._pagando.asReadonly();
  readonly unidades = computed(() => this._items().reduce((suma, i) => suma + i.cantidad, 0));
  readonly subtotal = computed(() => this._items().reduce((suma, i) => suma + i.producto.precio * i.cantidad, 0));

  // computed compara por valor: el efecto solo corre si cambia de verdad la persona (no por renovar el token).
  private usuarioId = computed(() => this.auth.sesion()?.id ?? null);

  constructor() {
    effect(() => {
      this.usuarioId();
      untracked(() => this._items.set([]));
    });
  }

  /** Máximo de unidades que se pueden pedir: un exclusivo solo admite una. */
  limiteDe(p: Producto): number {
    return p.exclusividad === 'EXCLUSIVO' ? Math.min(1, p.stock) : p.stock;
  }

  agregar(p: Producto, cantidad = 1): void {
    const limite = this.limiteDe(p);
    if (limite < 1) return;

    const existente = this._items().find((i) => i.producto.id === p.id);
    const total = Math.min(Math.trunc(cantidad) + (existente?.cantidad ?? 0), limite);
    if (!(total >= 1)) return;

    if (existente) {
      this._items.update((lista) => lista.map((i) => (i.producto.id === p.id ? { producto: p, cantidad: total } : i)));
    } else {
      this._items.update((lista) => [...lista, { producto: p, cantidad: total }]);
    }
  }

  cambiarCantidad(productoId: string, cantidad: number): void {
    const item = this._items().find((i) => i.producto.id === productoId);
    if (!item) return;

    const nueva = Math.min(Math.trunc(cantidad), this.limiteDe(item.producto));
    if (!(nueva >= 1)) {
      this.quitar(productoId);
      return;
    }
    this._items.update((lista) => lista.map((i) => (i.producto.id === productoId ? { ...i, cantidad: nueva } : i)));
  }

  quitar(productoId: string): void {
    this._items.update((lista) => lista.filter((i) => i.producto.id !== productoId));
  }

  vaciar(): void {
    this._items.set([]);
  }

  /**
   * Paga los ítems en orden y se detiene en el primer error. Los ítems pagados salen del carrito;
   * el fallido y los pendientes se conservan. Si ya hay un pago en curso (o no hay ítems) no emite nada.
   */
  pagar(cupon?: string): Observable<ResultadoPago> {
    return defer(() => {
      const pendientes = this._items();
      if (this._pagando() || pendientes.length === 0) return EMPTY;

      this._pagando.set(true);
      return from(pendientes).pipe(
        concatMap((item) => this.pagarItem(item, cupon)),
        takeWhile((intento) => intento.ok, true),
        toArray(),
        tap((intentos) => {
          const pagados = new Set(intentos.filter((i): i is IntentoOk => i.ok).map((i) => i.item.producto.id));
          this._items.update((lista) => lista.filter((i) => !pagados.has(i.producto.id)));
        }),
        map((intentos) => this.armarResultado(intentos)),
        finalize(() => this._pagando.set(false)),
      );
    });
  }

  /** Reserva (POST) y confirma (PUT). Si la confirmación falla, cancela la reserva para devolver el stock. */
  private pagarItem(item: ItemCarrito, cupon?: string): Observable<Intento> {
    const fallido = (e: unknown): IntentoFallido => ({ ok: false, item, mensaje: mensajeDeError(e) });

    return this.compras.realizar(item.producto.id, item.cantidad).pipe(
      switchMap((reserva) => {
        // Con cupón: aplicarlo (la compra sigue PENDIENTE) y luego confirmar.
        const paso$ = cupon
          ? this.compras.aplicarCupon(reserva.id, cupon).pipe(switchMap(() => this.compras.confirmar(reserva.id)))
          : this.compras.confirmar(reserva.id);

        return paso$.pipe(
          map((confirmada): Intento => ({ ok: true, item, compra: confirmada })),
          // Si falla el cupón o la confirmación, se cancela la reserva para devolver el stock.
          catchError((e) =>
            this.compras.cancelar(reserva.id).pipe(
              catchError(() => of(null)),
              map((): Intento => fallido(e)),
            ),
          ),
        );
      }),
      catchError((e) => of<Intento>(fallido(e))),
    );
  }

  private armarResultado(intentos: Intento[]): ResultadoPago {
    const completadas: Compra[] = [];
    let fallo: FalloPago | null = null;
    for (const intento of intentos) {
      if (intento.ok) completadas.push(intento.compra);
      else fallo = { producto: intento.item.producto, mensaje: intento.mensaje };
    }
    return { completadas, fallo };
  }
}
