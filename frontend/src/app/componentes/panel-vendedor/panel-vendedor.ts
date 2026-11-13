import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Exclusividad, Gama, MetricasResenas, Producto, ProductoStockBajo, ProductoVendido, ReporteVentas } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ProductoService } from '../../servicios/producto.service';
import { ReporteService } from '../../servicios/reporte.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';
import { precio } from '../../servicios/formato';
import { ArteProducto } from '../arte-producto/arte-producto';
import { TarjetaProducto } from '../tarjeta-producto/tarjeta-producto';

type Pestana = 'resumen' | 'productos' | 'promociones' | 'publicar';

@Component({
  selector: 'app-panel-vendedor',
  imports: [FormsModule, ArteProducto, TarjetaProducto],
  templateUrl: './panel-vendedor.html',
  styleUrl: './panel-vendedor.css',
  host: { '(document:keydown.escape)': 'cerrarEdicion()' },
})
export class PanelVendedor implements OnInit {
  private auth = inject(AuthService);
  private productos = inject(ProductoService);
  private reportes = inject(ReporteService);
  private toast = inject(ToastService);
  protected precio = precio;

  protected pestana = signal<Pestana>('resumen');
  protected pestanas: Array<{ id: Pestana; etiqueta: string }> = [
    { id: 'resumen', etiqueta: 'Resumen' },
    { id: 'productos', etiqueta: 'Mis productos' },
    { id: 'promociones', etiqueta: 'Promociones' },
    { id: 'publicar', etiqueta: 'Publicar' },
  ];

  // ---- resumen ----
  protected ventas = signal<ReporteVentas | null>(null);
  protected resenas = signal<MetricasResenas | null>(null);
  protected masVendidos = signal<ProductoVendido[]>([]);
  protected stockBajo = signal<ProductoStockBajo[]>([]);
  protected maxUnidades = computed(() => Math.max(1, ...this.masVendidos().map((p) => p.unidades)));
  protected barrasResenas = computed(() => {
    const r = this.resenas();
    const total = Math.max(1, r?.total ?? 0);
    return [5, 4, 3, 2, 1].map((n) => ({ n, cuantas: r?.distribucion?.[String(n)] ?? 0, pct: ((r?.distribucion?.[String(n)] ?? 0) / total) * 100 }));
  });

  // ---- mis productos ----
  protected misProductos = computed(() => {
    const yo = this.auth.sesion()?.id;
    return this.productos.catalogo().filter((p) => p.vendedorId === yo);
  });
  protected editando = signal<Producto | null>(null);
  protected editNombre = signal('');
  protected editDescripcion = signal('');
  protected editPrecio = signal<number | null>(null);
  protected guardando = signal(false);

  // ---- publicar ----
  protected nombre = signal('');
  protected descripcion = signal('');
  protected valor = signal<number | null>(null);
  protected stock = signal<number | null>(1);
  protected gama = signal<Gama>('MEDIA');
  protected exclusividad = signal<Exclusividad>('NORMAL');
  protected publicando = signal(false);
  protected errorForm = signal('');

  protected vistaPrevia = computed<Producto>(() => ({
    id: 'previa',
    vendedorId: this.auth.sesion()?.id ?? '',
    nombre: this.nombre() || 'Tu producto',
    descripcion: this.descripcion() || 'Así se verá tu producto en el catálogo.',
    precio: this.valor() ?? 0,
    stock: this.stock() ?? 0,
    gama: this.gama(),
    exclusividad: this.exclusividad(),
    categoria: null,
  }));

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

  ngOnInit() {
    this.cargarCatalogo();
    this.cargarResumen();
  }

  protected cargarCatalogo() {
    this.productos.cargarCatalogo().subscribe({ error: (e) => this.toast.error(mensajeDeError(e)) });
  }

  protected cargarResumen() {
    const yo = this.auth.sesion()?.id;
    this.reportes.ventas().subscribe({ next: (v) => this.ventas.set(v), error: () => undefined });
    this.reportes.metricasResenas().subscribe({ next: (m) => this.resenas.set(m), error: () => undefined });
    this.reportes.masVendidos(5, yo).subscribe({ next: (l) => this.masVendidos.set(l), error: () => undefined });
    this.reportes.stockBajo(5).subscribe({ next: (l) => this.stockBajo.set(l), error: () => undefined });
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
          this.cargarCatalogo();
        };
        if (precioNuevo !== p.precio) {
          this.productos.modificarPrecio(p.id, precioNuevo).subscribe({
            next: despues,
            error: (e) => { this.guardando.set(false); this.toast.error(mensajeDeError(e)); this.cargarCatalogo(); },
          });
        } else {
          despues();
        }
      },
      error: (e) => { this.guardando.set(false); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected visibilidad(p: Producto, activo: boolean) {
    this.productos.cambiarVisibilidad(p.id, activo).subscribe({
      next: () => { this.toast.ok(activo ? 'Producto reactivado.' : 'Producto pausado.'); this.cargarCatalogo(); },
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  protected eliminar(p: Producto) {
    if (!confirm(`¿Eliminar "${p.nombre}"? Dejará de aparecer en el catálogo.`)) return;
    this.productos.eliminar(p.id).subscribe({
      next: () => { this.toast.ok('Producto eliminado.'); this.cargarCatalogo(); this.cargarResumen(); },
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

    this.guardandoPromo.set(true);
    this.productos.configurarPromocion(this.promoProducto(), pct, this.promoDesde(), this.promoHasta()).subscribe({
      next: () => {
        this.guardandoPromo.set(false);
        this.toast.ok('Promoción de temporada configurada.');
        this.promoPorcentaje.set(null); this.promoDesde.set(''); this.promoHasta.set('');
        this.cargarCatalogo();
      },
      error: (e) => { this.guardandoPromo.set(false); this.errorPromo.set(mensajeDeError(e)); },
    });
  }

  protected aplicarDescuento() {
    this.errorDesc.set('');
    if (!this.descProducto()) return this.errorDesc.set('Elige un producto.');
    const pct = this.descPorcentaje();
    if (pct == null || pct <= 0 || pct >= 100) return this.errorDesc.set('El porcentaje debe estar entre 0 y 100.');

    this.aplicandoDesc.set(true);
    this.productos.aplicarDescuento(this.descProducto(), pct).subscribe({
      next: (r) => {
        this.aplicandoDesc.set(false);
        this.toast.ok(`Descuento aplicado: "${r.nombre}" ahora cuesta ${precio(r.nuevoPrecio)}.`);
        this.descPorcentaje.set(null);
        this.cargarCatalogo();
      },
      error: (e) => { this.aplicandoDesc.set(false); this.errorDesc.set(mensajeDeError(e)); },
    });
  }

  // ---------- publicar ----------
  protected publicar() {
    this.errorForm.set('');
    if (!this.nombre().trim()) return this.errorForm.set('El nombre es obligatorio.');
    if (!this.descripcion().trim()) return this.errorForm.set('La descripción es obligatoria.');
    if (this.valor() == null || this.valor()! <= 0) return this.errorForm.set('El precio debe ser mayor a cero.');
    if (this.stock() == null || this.stock()! < 0) return this.errorForm.set('El stock no puede ser negativo.');

    this.publicando.set(true);
    this.productos
      .publicar({
        nombre: this.nombre().trim(),
        descripcion: this.descripcion().trim(),
        precio: this.valor()!,
        stock: this.stock()!,
        gama: this.gama(),
        exclusividad: this.exclusividad(),
      })
      .subscribe({
        next: () => {
          this.publicando.set(false);
          this.toast.ok('¡Producto publicado!');
          this.nombre.set(''); this.descripcion.set(''); this.valor.set(null); this.stock.set(1);
          this.cargarCatalogo();
          this.pestana.set('productos');
        },
        error: (e) => { this.publicando.set(false); this.errorForm.set(mensajeDeError(e)); },
      });
  }
}
