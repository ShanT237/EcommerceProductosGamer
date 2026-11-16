import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { EMPTY, Observable, catchError, defer, finalize, map, of, switchMap, tap } from 'rxjs';
import { Compra, Producto } from '../modelos/modelos';
import { mensajeDeError } from './api.util';
import { AuthService } from './auth.service';
import { CompraService } from './compra.service';
import { precioVigente } from './formato';

const PREFIJO_CLAVE = 'gamerzone.carrito.';

export interface ItemCarrito { producto: Producto; cantidad: number }
/**
 * `error` es `null` si el pago se completó; si no, trae el mensaje del backend y el carrito queda intacto.
 * `pedidoSinCancelar` trae el id del pedido solo cuando además falló su cancelación: en ese caso el pedido
 * puede seguir pendiente con el stock reservado y hay que reintentar la cancelación o revisarlo.
 */
export interface ResultadoPago { compras: Compra[]; error: string | null; pedidoSinCancelar?: string | null }

/** Estado real de un pedido tras reintentar su cancelación. */
export type DesenlacePedido = 'CANCELADO' | 'PAGADO' | 'PENDIENTE';

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
  private _pedidoSinCancelar = signal<string | null>(null);

  readonly items = this._items.asReadonly();
  readonly pagando = this._pagando.asReadonly();
  /** Pedido cuya cancelación falló: mientras exista no se permite pagar otra vez (duplicaría la reserva). */
  readonly pedidoSinCancelar = this._pedidoSinCancelar.asReadonly();
  readonly unidades = computed(() => this._items().reduce((suma, i) => suma + i.cantidad, 0));
  readonly subtotal = computed(() => this._items().reduce((suma, i) => suma + precioVigente(i.producto) * i.cantidad, 0));

  // computed compara por valor: el efecto solo corre si cambia de verdad la persona (no por renovar el token).
  private usuarioId = computed(() => this.auth.sesion()?.id ?? null);

  constructor() {
    effect(() => {
      const id = this.usuarioId();
      untracked(() => {
        this._items.set(id ? this.leer(id) : []);
        this._pedidoSinCancelar.set(null);
      });
    });

    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (e) => {
        const id = this.usuarioId();
        if (id && e.key === PREFIJO_CLAVE + id && !this._pagando()) this._items.set(this.leer(id));
      });
    }
  }

  private clave(id: string) { return PREFIJO_CLAVE + id; }

  private leer(id: string): ItemCarrito[] {
    try {
      const crudo = localStorage.getItem(this.clave(id));
      const lista: unknown = crudo ? JSON.parse(crudo) : [];
      if (!Array.isArray(lista)) return [];
      return lista.filter(
        (i): i is ItemCarrito =>
          !!i && typeof i.producto?.id === 'string' && Number.isInteger(i.cantidad) && i.cantidad >= 1,
      );
    } catch {
      return []; // JSON corrupto o sin almacenamiento
    }
  }

  private escribir(items: ItemCarrito[]): void {
    const id = this.usuarioId();
    if (!id) return;
    try {
      if (items.length) localStorage.setItem(this.clave(id), JSON.stringify(items));
      else localStorage.removeItem(this.clave(id));
    } catch { /* cuota llena o sin almacenamiento: el carrito sigue funcionando en memoria */ }
  }

  private fijar(items: ItemCarrito[]): void {
    this._items.set(items);
    this.escribir(items);
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

    this.fijar(
      existente
        ? this._items().map((i) => (i.producto.id === p.id ? { producto: p, cantidad: total } : i))
        : [...this._items(), { producto: p, cantidad: total }],
    );
  }

  cambiarCantidad(productoId: string, cantidad: number): void {
    const item = this._items().find((i) => i.producto.id === productoId);
    if (!item) return;
    const nueva = Math.min(Math.trunc(cantidad), this.limiteDe(item.producto));
    if (!(nueva >= 1)) return this.quitar(productoId);
    this.fijar(this._items().map((i) => (i.producto.id === productoId ? { ...i, cantidad: nueva } : i)));
  }

  quitar(productoId: string): void {
    this.fijar(this._items().filter((i) => i.producto.id !== productoId));
  }

  vaciar(): void {
    this.fijar([]);
  }

  sincronizar(catalogo: Producto[]): void {
    const porId = new Map(catalogo.map((p) => [p.id, p]));
    const actualizados = this._items().flatMap((i) => {
      const fresco = porId.get(i.producto.id);
      if (!fresco) return [i];
      const limite = this.limiteDe(fresco);
      return limite < 1 ? [] : [{ producto: fresco, cantidad: Math.min(i.cantidad, limite) }];
    });
    this.fijar(actualizados);
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
      const pendiente = this._pedidoSinCancelar();
      if (pendiente) {
        return of<ResultadoPago>({
          compras: [],
          error: 'Tienes un pedido anterior que no se pudo cancelar. Reintenta su cancelación o revísalo en Mis compras antes de pagar de nuevo',
          pedidoSinCancelar: pendiente,
        });
      }

      this._pagando.set(true);
      const pedido$ = this.compras.crearPedido(items.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad })), cupon);
      return pedido$.pipe(
        switchMap((pedido) =>
          this.compras.confirmarPedido(pedido.pedidoId).pipe(
            map((confirmado): ResultadoPago => ({ compras: confirmado.compras, error: null })),
            // Si falla la confirmación se cancela el pedido para devolver el stock. Si la cancelación también
            // falla NO se oculta: el pedido puede seguir pendiente con el stock reservado, así que se recuerda
            // su id (bloquea nuevos pagos) y se informa junto al error original.
            catchError((e) =>
              this.compras.cancelarPedido(pedido.pedidoId).pipe(
                map((): ResultadoPago => ({ compras: [], error: mensajeDeError(e), pedidoSinCancelar: null })),
                catchError(() => {
                  this._pedidoSinCancelar.set(pedido.pedidoId);
                  return of<ResultadoPago>({ compras: [], error: mensajeDeError(e), pedidoSinCancelar: pedido.pedidoId });
                }),
              ),
            ),
          ),
        ),
        catchError((e) => of<ResultadoPago>({ compras: [], error: mensajeDeError(e) })),
        tap((r) => {
          if (!r.error) this.fijar([])
        }),
        finalize(() => this._pagando.set(false)),
      );
    });
  }

  /**
   * Reintenta cancelar el pedido que quedó sin cancelar. Si el PUT vuelve a fallar, consulta el estado real
   * del pedido (GET /compras) para reconciliar: ya cancelado → libera el bloqueo; ya pagado (la confirmación
   * sí llegó al servidor) → vacía el carrito; sigue pendiente o no se pudo consultar → conserva el bloqueo.
   */
  reintentarCancelacion(): Observable<DesenlacePedido> {
    return defer(() => {
      const id = this._pedidoSinCancelar();
      if (!id || this._pagando()) return EMPTY;

      this._pagando.set(true);
      return this.compras.cancelarPedido(id).pipe(
        map((): DesenlacePedido => 'CANCELADO'),
        catchError(() =>
          this.compras.listar().pipe(
            map((todas): DesenlacePedido => {
              const lineas = todas.filter((c) => c.pedidoId === id);
              if (lineas.length > 0 && lineas.every((c) => c.estado === 'CANCELADA')) return 'CANCELADO';
              if (lineas.some((c) => c.estado === 'COMPLETADA' || c.estado === 'REEMBOLSADA')) return 'PAGADO';
              return 'PENDIENTE';
            }),
            catchError(() => of<DesenlacePedido>('PENDIENTE')),
          ),
        ),
        tap((desenlace) => {
          if (desenlace !== 'PENDIENTE') this._pedidoSinCancelar.set(null);
          if (desenlace === 'PAGADO') this.fijar([]);
        }),
        finalize(() => this._pagando.set(false)),
      );
    });
  }
}
