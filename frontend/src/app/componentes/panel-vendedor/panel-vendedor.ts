import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MetricasResenas,
  Producto,
  ProductoStockBajo,
  ProductoVendido,
  PromocionTemporada,
  ReporteVentas
} from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ProductoService } from '../../servicios/producto.service';
import { ReporteService } from '../../servicios/reporte.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';
import { descuentoTotal, estadoPromocion, precio, precioTachado, precioVigente } from '../../servicios/formato';
import { ArteProducto } from '../arte-producto/arte-producto';
import { FormularioPublicar } from '../formulario-publicar/formulario-publicar';
import { CombosVendedor } from '../combos-vendedor/combos-vendedor';
import { CuponesVendedor } from '../cupones-vendedor/cupones-vendedor';
import { RegalosVendedor } from '../regalos-vendedor/regalos-vendedor';
import { PreventasVendedor } from '../preventas-vendedor/preventas-vendedor';
import { MetasVendedor } from '../metas-vendedor/metas-vendedor';

type Pestana = 'resumen' | 'productos' | 'promociones' | 'publicar' | 'combos' | 'cupones' | 'regalos' | 'preventas' | 'metas';

@Component({
  selector: 'app-panel-vendedor',
  imports: [
    FormsModule,
    ArteProducto,
    FormularioPublicar,
    CombosVendedor,
    CuponesVendedor,
    RegalosVendedor,
    PreventasVendedor,
    MetasVendedor
  ],
  templateUrl: './panel-vendedor.html',
  styleUrl: './panel-vendedor.css',
  host: { '(document:keydown.escape)': 'cerrarEdicion()' },
})
export class PanelVendedor implements OnInit {
  private auth = inject(AuthService);
  private productos = inject(ProductoService);
  private reportes = inject(ReporteService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  protected precio = precio;
  protected precioVigente = precioVigente;
  protected precioTachado = precioTachado;
  protected descuentoTotal = descuentoTotal;
  protected estadoPromo = estadoPromocion;

  protected pestana = signal<Pestana>('resumen');
  protected pestanas: Array<{ id: Pestana; etiqueta: string }> = [
    { id: 'resumen', etiqueta: 'Resumen' },
    { id: 'productos', etiqueta: 'Mis productos' },
    { id: 'promociones', etiqueta: 'Promociones' },
    { id: 'publicar', etiqueta: 'Publicar' },
    { id: 'combos', etiqueta: 'Combos' },
    { id: 'cupones', etiqueta: 'Cupones' },
    { id: 'regalos', etiqueta: 'Regalos' },
    { id: 'preventas', etiqueta: 'Preventas' },
    { id: 'metas', etiqueta: 'Metas' },
  ];

  // ---- resumen ----
  protected ventas = signal<ReporteVentas | null>(null);
  protected resenas = signal<MetricasResenas | null>(null);
  protected masVendidos = signal<ProductoVendido[]>([]);
  protected stockBajo = signal<ProductoStockBajo[]>([]);
  protected promociones = signal<PromocionTemporada[]>([]);
  protected promoEditandoId = signal<string | null>(null);
  protected quitandoPromo = signal<string | null>(null);
  protected maxUnidades = computed(() => Math.max(1, ...this.masVendidos().map((p) => p.unidades)));
  protected barrasResenas = computed(() => {
    const r = this.resenas();
    const total = Math.max(1, r?.total ?? 0);
    return [5, 4, 3, 2, 1].map((n) => ({ n, cuantas: r?.distribucion?.[String(n)] ?? 0, pct: ((r?.distribucion?.[String(n)] ?? 0) / total) * 100 }));
  });

  // ---- mis productos ----
  // Viene de GET /productos/mios (incluye los pausados). El catálogo público los oculta,
  // y por eso un producto pausado desaparecía de aquí y no se podía reactivar.
  protected misProductos = computed(() => {
    const yo = this.auth.sesion()?.id;
    return this.productos.misProductos().filter((p) => p.vendedorId === yo);
  });
  protected conDescuento = computed(() => this.misProductos().filter((p) => !!p.precioOriginal));
  protected editando = signal<Producto | null>(null);
  protected editNombre = signal('');
  protected editDescripcion = signal('');
  protected editPrecio = signal<number | null>(null);
  protected guardando = signal(false);
  /** Id del producto cuya visibilidad se está cambiando (evita dobles clics en Pausar/Reactivar). */
  protected cambiandoVisibilidad = signal<string | null>(null);

  // ---- promociones por temporada y descuentos ----
  protected promoProducto = signal('');
  protected promoPorcentaje = signal<number | null>(null);
  protected promoDesde = signal('');
  protected promoHasta = signal('');
  protected guardandoPromo = signal(false);
  protected errorPromo = signal('');
  protected descProducto = signal('');
  protected descPorcentaje = signal<number | null>(null);
  protected aplicandoDesc = signal(false);
  protected errorDesc = signal('');
  /** Id del producto cuyo descuento se está quitando (evita dobles clics). */
  protected quitandoDesc = signal<string | null>(null);

  protected avisoPromo = computed(() => {
    const p = this.misProductos().find((x) => x.id === this.promoProducto());
    return p?.precioOriginal
      ? `Este producto ya tiene una oferta inmediata (hoy cuesta ${precio(p.precio)}). La promoción de temporada se aplica encima de ese precio.`
      : '';
  });

  protected avisoDescuento = computed(() => {
    const id = this.descProducto();
    const activa = this.promociones().find(
      (pr) => pr.productoId === id && estadoPromocion(pr.desde, pr.hasta) !== 'vencida',
    );
    return activa
      ? `Este producto tiene una promoción de temporada de -${activa.porcentaje}% (${activa.desde} a ${activa.hasta}). Las dos se acumulan en cadena.`
      : '';
  });

  ngOnInit() {
    this.cargarMisProductos();
    this.cargarResumen();
    this.cargarPromociones();
  }

  protected cargarPromociones() {
    this.productos.listarPromociones().subscribe({
      next: (l) => this.promociones.set(l),
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  protected cargarMisProductos() {
    this.productos.cargarMisProductos().subscribe({ error: (e) => this.toast.error(mensajeDeError(e)) });
  }

  protected cargarResumen() {
    const yo = this.auth.sesion()?.id;
    this.reportes.ventas().subscribe({ next: (v) => this.ventas.set(v), error: () => undefined });
    this.reportes.metricasResenas().subscribe({ next: (m) => this.resenas.set(m), error: () => undefined });
    this.reportes.masVendidos(5, yo).subscribe({ next: (l) => this.masVendidos.set(l), error: () => undefined });
    this.reportes.stockBajo(5).subscribe({ next: (l) => this.stockBajo.set(l), error: () => undefined });
  }

  // ---------- publicar (el formulario vive en FormularioPublicar) ----------
  protected alPublicar() {
    this.cargarMisProductos();
    this.pestana.set('productos');
  }

  // ---------- editar / pausar / eliminar ----------
  protected editar(p: Producto) {
    this.editando.set(p);
    this.editNombre.set(p.nombre);
    this.editDescripcion.set(p.descripcion);
    this.editPrecio.set(p.precio);
  }

  protected cerrarEdicion() { if (!this.guardando()) this.editando.set(null); }

  protected guardarEdicion() {
    const p = this.editando();
    if (!p) return;
    if (!this.editNombre().trim() || !this.editDescripcion().trim()) return this.toast.error('El nombre y la descripción son obligatorios.');
    const precioNuevo = this.editPrecio();
    if (precioNuevo == null || precioNuevo <= 0) return this.toast.error('El precio debe ser mayor a cero.');

    this.guardando.set(true);
    this.productos.editarInformacion(p.id, this.editNombre().trim(), this.editDescripcion().trim()).subscribe({
      next: () => {
        const despues = () => {
          this.guardando.set(false);
          this.editando.set(null);
          this.toast.ok('Producto actualizado.');
          this.cargarMisProductos();
        };
        if (!p.precioOriginal && precioNuevo !== p.precio) {
          this.productos.modificarPrecio(p.id, precioNuevo).subscribe({
            next: despues,
            error: (e) => { this.guardando.set(false); this.toast.error(mensajeDeError(e)); this.cargarMisProductos(); },
          });
        } else {
          despues();
        }
      },
      error: (e) => { this.guardando.set(false); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected visibilidad(p: Producto, activo: boolean) {
    if (this.cambiandoVisibilidad()) return;
    this.cambiandoVisibilidad.set(p.id);
    this.productos.cambiarVisibilidad(p.id, activo).subscribe({
      next: () => {
        this.cambiandoVisibilidad.set(null);
        this.toast.ok(activo ? 'Producto reactivado.' : 'Producto pausado.');
        this.cargarMisProductos();
      },
      // Si el estado en pantalla estaba desfasado (p. ej. "ya está pausado"), se recarga para corregirlo.
      error: (e) => {
        this.cambiandoVisibilidad.set(null);
        this.toast.error(mensajeDeError(e));
        this.cargarMisProductos();
      },
    });
  }

  protected async eliminar(p: Producto) {
    const acepto = await this.confirmacion.pedir({
      titulo: 'Eliminar producto',
      mensaje: `¿Eliminar "${p.nombre}"? Dejará de aparecer en el catálogo.`,
      confirmar: 'Eliminar',
    });
    if (!acepto) return;
    this.productos.eliminar(p.id).subscribe({
      next: () => { this.toast.ok('Producto eliminado.'); this.cargarMisProductos(); this.cargarResumen(); },
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  // ---------- promociones y descuentos ----------
  protected crearPromocion() {
    this.errorPromo.set('');
    if (!this.promoProducto()) return this.errorPromo.set('Elige un producto.');
    const pct = this.promoPorcentaje();
    if (pct == null || pct <= 0 || pct >= 100) return this.errorPromo.set('El porcentaje debe estar entre 0 y 100.');
    if (!this.promoDesde() || !this.promoHasta()) return this.errorPromo.set('Elige las fechas de inicio y de fin.');
    if (this.promoHasta() < this.promoDesde()) return this.errorPromo.set('La fecha de fin no puede ser anterior a la de inicio.');

    const editandoId = this.promoEditandoId();
    const peticion = editandoId
      ? this.productos.modificarPromocion(editandoId, pct, this.promoDesde(), this.promoHasta())
      : this.productos.configurarPromocion(this.promoProducto(), pct, this.promoDesde(), this.promoHasta());

    this.guardandoPromo.set(true);
    peticion.subscribe({
      next: () => {
        this.guardandoPromo.set(false);
        this.toast.ok(editandoId ? 'Promoción actualizada.' : 'Promoción de temporada configurada.');
        this.promoPorcentaje.set(null); this.promoDesde.set(''); this.promoHasta.set('');
        this.promoEditandoId.set(null);
        this.cargarPromociones();
        this.cargarMisProductos();
      },
      error: (e) => { this.guardandoPromo.set(false); this.errorPromo.set(mensajeDeError(e)); },
    });
  }

  protected editarPromocion(pr: PromocionTemporada) {
    this.errorPromo.set('');
    this.promoEditandoId.set(pr.id);
    this.promoProducto.set(pr.productoId);
    this.promoPorcentaje.set(pr.porcentaje);
    this.promoDesde.set(pr.desde);
    this.promoHasta.set(pr.hasta);
  }

  protected cancelarEdicionPromo() {
    this.promoEditandoId.set(null);
    this.promoProducto.set('');
    this.promoPorcentaje.set(null); this.promoDesde.set(''); this.promoHasta.set('');
    this.errorPromo.set('');
  }

  protected async quitarPromocion(pr: PromocionTemporada) {
    if (this.quitandoPromo()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Quitar promoción',
      mensaje: `¿Retirar la promoción de temporada de -${pr.porcentaje}% de "${pr.productoNombre}"? El producto volverá a su precio sin promoción.`,
      confirmar: 'Quitar promoción',
    });
    if (!acepto || this.quitandoPromo()) return;
    this.quitandoPromo.set(pr.id);
    this.productos.quitarPromocion(pr.id).subscribe({
      next: () => {
        this.quitandoPromo.set(null);
        if (this.promoEditandoId() === pr.id) this.cancelarEdicionPromo();
        this.toast.ok(`Promoción retirada de "${pr.productoNombre}".`);
        this.cargarPromociones();
        this.cargarMisProductos();
      },
      error: (e) => { this.quitandoPromo.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected promocionAplicada(p: Producto): PromocionTemporada | undefined {
    if (!p.porcentajePromocion) return undefined;
    return this.promociones().find((pr) => pr.productoId === p.id && pr.hasta === p.promocionHasta);
  }

  protected async aplicarDescuento() {
    this.errorDesc.set('');
    if (!this.descProducto()) return this.errorDesc.set('Elige un producto.');
    const pct = this.descPorcentaje();
    if (pct == null || pct <= 0 || pct >= 100) return this.errorDesc.set('El porcentaje debe estar entre 0 y 100.');

    const producto = this.misProductos().find((x) => x.id === this.descProducto());
    const acepto = await this.confirmacion.pedir({
      titulo: 'Aplicar oferta inmediata',
      mensaje: producto
        ? `"${producto.nombre}" pasará de ${precio(producto.precio)} a ${precio(Math.round(producto.precio * (1 - pct / 100)))} (-${pct}%) desde ya. Cambia el precio base del producto.`
        : `Se aplicará un descuento de -${pct}% desde ya. Cambia el precio base del producto.`,
      confirmar: 'Aplicar descuento',
      peligro: false,
    });
    if (!acepto) return;

    this.aplicandoDesc.set(true);
    this.productos.aplicarDescuento(this.descProducto(), pct).subscribe({
      next: (r) => {
        this.aplicandoDesc.set(false);
        this.toast.ok(`Descuento aplicado: "${r.nombre}" ahora cuesta ${precio(r.nuevoPrecio)}.`);
        this.descPorcentaje.set(null);
        this.cargarMisProductos();
      },
      error: (e) => { this.aplicandoDesc.set(false); this.errorDesc.set(mensajeDeError(e)); },
    });
  }

  protected async quitarDescuento(p: Producto) {
    if (this.quitandoDesc()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Quitar oferta',
      mensaje: `¿Retirar la oferta de "${p.nombre}"? Volverá a costar ${precio(p.precioOriginal ?? p.precio)}.`,
      confirmar: 'Quitar oferta',
    });
    if (!acepto || this.quitandoDesc()) return;
    this.quitandoDesc.set(p.id);
    this.productos.quitarDescuento(p.id).subscribe({
      next: (r) => {
        this.quitandoDesc.set(null);
        this.toast.ok(`Descuento retirado: "${r.nombre}" vuelve a costar ${precio(r.nuevoPrecio)}.`);
        this.cargarMisProductos();
      },
      error: (e) => { this.quitandoDesc.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
