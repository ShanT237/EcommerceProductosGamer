import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { Gama, Pagina, Pedido, Producto } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { CarritoService } from '../../servicios/carrito.service';
import { CompraService } from '../../servicios/compra.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';
import { WishlistService } from '../../servicios/wishlist.service';
import { mensajeDeError } from '../../servicios/api.util';
import {descuentoTotal, fechaDia, precio, precioTachado, precioVigente} from '../../servicios/formato';
import { ArteProducto } from '../arte-producto/arte-producto';
import { TarjetaProducto } from '../tarjeta-producto/tarjeta-producto';

@Component({
  selector: 'app-catalogo',
  imports: [FormsModule, TarjetaProducto, ArteProducto],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css',
  host: { '(document:keydown.escape)': 'cerrar()' },
})
export class Catalogo implements OnInit {
  private productos = inject(ProductoService);
  private compras = inject(CompraService);
  private carrito = inject(CarritoService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  protected wishlist = inject(WishlistService);
  protected auth = inject(AuthService);
  protected precio = precio;
  protected precioVigente = precioVigente;
  protected precioTachado = precioTachado;
  protected fechaDia = fechaDia;

  // ---- filtros (búsqueda predictiva: se consulta 300 ms después de dejar de escribir) ----
  protected texto = signal('');
  protected categoria = signal('');
  protected gama = signal<Gama | ''>('');
  protected precioMin = signal<number | null>(null);
  protected precioMax = signal<number | null>(null);
  protected soloPreventa = signal(false);
  protected gamas: Array<{ valor: Gama | ''; etiqueta: string }> = [
    { valor: '', etiqueta: 'Todas' },
    { valor: 'BAJA', etiqueta: 'Baja' },
    { valor: 'MEDIA', etiqueta: 'Media' },
    { valor: 'ALTA', etiqueta: 'Alta' },
  ];

  // ---- resultados ----
  protected pagina = signal(0);
  protected resultado = signal<Pagina<Producto> | null>(null);
  protected cargando = signal(false);
  protected error = signal('');
  protected tamano = 6; // 6 = múltiplo de 1, 2 y 3 columnas (cuadrícula Bootstrap)
  private ultimaPeticion = 0;
  private urlRevisada = false;
  private busquedas = new Subject<void>();

  // Sugerencias del filtro de categoría: se juntan de las páginas que ya llegaron del backend
  // (no se pide el catálogo completo; la paginación sigue siendo la única consulta).
  private categoriasVistas = signal<ReadonlySet<string>>(new Set());
  protected categorias = computed(() => [...this.categoriasVistas()].sort());

  // ---- paginación: todo sale de la respuesta del backend (pagina empieza en 0) ----
  protected filtrosAbiertos = signal(false); // en celular los filtros van plegados
  protected totalPaginas = computed(() => this.resultado()?.totalPaginas ?? 0);
  protected textoPagina = computed(() => {
    const r = this.resultado();
    return r && r.totalPaginas > 0 ? `Página ${r.pagina + 1} de ${r.totalPaginas}` : '';
  });
  protected hayAnterior = computed(() => (this.resultado()?.pagina ?? 0) > 0);
  protected haySiguiente = computed(() => {
    const r = this.resultado();
    return !!r && r.pagina < r.totalPaginas - 1;
  });

  protected paginas = computed(() => {
    const total = this.resultado()?.totalPaginas ?? 0;
    const actual = this.pagina();
    const inicio = Math.max(0, Math.min(actual - 3, total - 7));
    return Array.from({ length: Math.min(total, 7) }, (_, i) => inicio + i);
  });

  // ---- detalle / compra ----
  protected seleccionado = signal<Producto | null>(null);
  protected cantidad = signal(1);
  /** Cupón opcional de la compra directa: viaja en el POST del pedido (se consume al reservar). */
  protected codigoCupon = signal('');
  /** Compra directa = pedido de un solo ítem, reservado hasta que se paga o se cancela. */
  protected reserva = signal<Pedido | null>(null);
  protected procesando = signal(false);
  protected esComprador = computed(() => this.auth.rol() === 'COMPRADOR');
  protected total = computed(() => {
    const p = this.seleccionado();
    return (p ? precioVigente(p) : 0) * this.cantidad(); // con la promoción vigente, como se cobrará
  });
  // El estado de favoritos vive en WishlistService (viene del servidor), no aquí.
  protected esFavorito = computed(() => {
    const p = this.seleccionado();
    return !!p && this.wishlist.ids().has(p.id);
  });

  constructor() {
    this.busquedas.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.pagina.set(0);
      this.cargar();
    });
  }

  ngOnInit() {
    this.cargar(); // única consulta al abrir: una página (page + size) al backend
  }

  protected cambiaFiltro() { this.busquedas.next(); }

  protected elegirGama(g: Gama | '') {
    this.gama.set(g);
    this.cambiaFiltro();
  }

  protected limpiar() {
    this.texto.set(''); this.categoria.set(''); this.gama.set('');
    this.precioMin.set(null); this.precioMax.set(null); this.soloPreventa.set(false);
    this.pagina.set(0);
    this.cargar();
  }

  protected irA(p: number) {
    const total = this.resultado()?.totalPaginas ?? 0;
    if (this.cargando() || p < 0 || p >= total || p === this.pagina()) return;
    this.pagina.set(p);
    this.cargar();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected cargar() {
    const id = ++this.ultimaPeticion;
    this.cargando.set(true);
    this.error.set('');
    this.productos
      .buscar(
        {
          texto: this.texto(), categoria: this.categoria(), gama: this.gama(),
          precioMin: this.precioMin(), precioMax: this.precioMax(), soloPreventa: this.soloPreventa(),
        },
        this.pagina(),
        this.tamano,
      )
      .subscribe({
        next: (r) => {
          if (id !== this.ultimaPeticion) return; // llegó una respuesta vieja: se ignora
          this.resultado.set(r);
          this.cargando.set(false);
          const nuevas = r.contenido.map((p) => p.categoria).filter((c): c is string => !!c);
          if (nuevas.some((c) => !this.categoriasVistas().has(c))) {
            this.categoriasVistas.set(new Set([...this.categoriasVistas(), ...nuevas]));
          }
          if (!this.urlRevisada) {
            this.urlRevisada = true;
            this.abrirDesdeUrl();
          }
        },
        error: (e) => {
          if (id !== this.ultimaPeticion) return;
          this.cargando.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }

  // ---------- modal de detalle ----------
  protected abrir(p: Producto) {
    this.seleccionado.set(p);
    this.cantidad.set(1);
    this.codigoCupon.set('');
    this.reserva.set(null);
  }

  protected cerrar() {
    if (this.procesando()) return;
    this.seleccionado.set(null);
    this.reserva.set(null);
  }

  protected cambiarCantidad(delta: number) {
    const max = this.seleccionado()?.stock ?? 1;
    this.cantidad.set(Math.min(Math.max(1, this.cantidad() + delta), Math.max(max, 1)));
  }

  protected alCarrito() {
    const p = this.seleccionado();
    if (!p) return;
    this.carrito.agregar(p, this.cantidad());
    this.toast.ok('Añadido al carrito.');
    this.cerrar();
  }

  protected reservar() {
    const p = this.seleccionado();
    if (!p) return;
    this.procesando.set(true);
    this.compras.crearPedido([{ productoId: p.id, cantidad: this.cantidad() }], this.codigoCupon()).subscribe({
      next: (pedido) => {
        this.reserva.set(pedido);
        this.codigoCupon.set(''); // ya se consumió con el pedido: no se reenvía si se cancela y se vuelve a reservar
        this.procesando.set(false);
        this.toast.info('Compra reservada. Confirma el pago para completarla.');
      },
      error: (e) => {
        this.procesando.set(false);
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  protected async pagar() {
    const c = this.reserva();
    if (!c || this.procesando()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Confirmar pago',
      mensaje: `Vas a pagar ${precio(c.total)} por ${this.seleccionado()?.nombre ?? 'tu compra'} (${this.unidadesDe(c)} u.). Es un pago simulado.`,
      confirmar: 'Pagar',
      peligro: false,
    });
    if (!acepto || this.reserva()?.pedidoId !== c.pedidoId) return;
    this.procesando.set(true);
    this.compras.confirmarPedido(c.pedidoId).subscribe({
      next: () => {
        this.procesando.set(false);
        this.toast.ok('¡Pago simulado exitoso! Tu compra está completada.');
        this.seleccionado.set(null);
        this.reserva.set(null);
        this.cargar();
        this.router.navigateByUrl('/mis-compras');
      },
      error: (e) => {
        this.procesando.set(false);
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  protected async cancelarReserva() {
    const c = this.reserva();
    if (!c || this.procesando()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Cancelar reserva',
      mensaje: '¿Cancelar esta reserva? Se liberarán las unidades reservadas.',
      confirmar: 'Cancelar reserva',
      cancelar: 'Volver',
    });
    if (!acepto || this.reserva()?.pedidoId !== c.pedidoId) return;
    this.procesando.set(true);
    this.compras.cancelarPedido(c.pedidoId).subscribe({
      next: () => {
        this.procesando.set(false);
        this.reserva.set(null);
        this.toast.info('Reserva cancelada.');
        this.cargar();
      },
      error: (e) => {
        this.procesando.set(false);
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  protected unidadesDe(pedido: Pedido): number {
    return pedido.compras.reduce((suma, c) => suma + c.cantidad, 0);
  }

  /** Corazón del modal de detalle. */
  protected alternarFavorito() {
    const p = this.seleccionado();
    if (p) this.alternarDe(p);
  }

  /** Corazón de una tarjeta de la rejilla (también lo usa el modal). */
  protected alternarDe(p: Producto) {
    const era = this.wishlist.esFavorito(p.id);
    this.wishlist.alternar(p).subscribe({
      next: () => this.toast.ok(era ? 'Quitado de tus favoritos.' : 'Añadido a tus favoritos.'),
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  /** Si la URL trae ?producto=<id> (viene de Favoritos), abre directamente ese producto. */
  private abrirDesdeUrl() {
    const id = this.ruta.snapshot.queryParamMap.get('producto');
    if (!id) return;
    const p =
      this.resultado()?.contenido.find((x) => x.id === id) ??
      this.wishlist.productos().find((x) => x.id === id);
    if (p) this.abrir(p);
    // Limpia el parámetro para que al cerrar y recargar no se reabra solo.
    this.router.navigate([], { queryParams: { producto: null }, queryParamsHandling: 'merge', replaceUrl: true });
  }

  protected readonly descuentoTotal = descuentoTotal;
}