import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { EMPTY, Observable, catchError, defer, finalize, map, of, switchMap, tap } from 'rxjs';
import { Compra, Producto } from '../modelos/modelos';
import { mensajeDeError } from './api.util';
import { AuthService } from './auth.service';
import { CompraService } from './compra.service';
import { precioVigente } from './formato';

export interface ItemCarrito { producto: Producto; cantidad: number }
/** `error` es `null` si el pago se completó; si no, trae el mensaje del backend y el carrito queda intacto. */
export interface ResultadoPago { compras: Compra[]; error: string | null }

/**
 * Carrito de compra simulado. Vive solo en el frontend (no persiste al recargar).
 * Pagar = crear un único pedido con todos los ítems y confirmarlo: todo o nada.
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
  readonly subtotal = computed(() => this._items().reduce((suma, i) => suma + precioVigente(i.producto) * i.cantidad, 0));

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
   * Paga todo el carrito como un solo pedido (POST /pedidos y luego PUT /confirmar). Es todo o nada:
   * si falla la creación o la confirmación el carrito queda intacto (y si falla confirmar se cancela el pedido
   * para devolver el stock). Si ya hay un pago en curso (o no hay ítems) no emite nada.
   * Con `cupon` el código viaja una sola vez, en el POST del pedido (el PUT de confirmar no lo lleva).
   */
  pagar(cupon?: string): Observable<ResultadoPago> {
    return defer(() => {
      const items = this._items();
      if (this._pagando() || items.length === 0) return EMPTY;

      this._pagando.set(true);
      const pedido$ = this.compras.crearPedido(items.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad })), cupon);
      return pedido$.pipe(
        switchMap((pedido) =>
          this.compras.confirmarPedido(pedido.pedidoId).pipe(
            map((confirmado): ResultadoPago => ({ compras: confirmado.compras, error: null })),
            // Si falla la confirmación se cancela el pedido para devolver el stock; se informa el error original.
            catchError((e) =>
              this.compras.cancelarPedido(pedido.pedidoId).pipe(
                catchError(() => of(null)),
                map((): ResultadoPago => ({ compras: [], error: mensajeDeError(e) })),
              ),
            ),
          ),
        ),
        catchError((e) => of<ResultadoPago>({ compras: [], error: mensajeDeError(e) })),
        tap((r) => {
          if (!r.error) this._items.set([]);
        }),
        finalize(() => this._pagando.set(false)),
      );
    });
  }
}