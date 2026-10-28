import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { Compra, Gama, Pagina, Producto } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { CompraService } from '../../servicios/compra.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';
import { precio } from '../../servicios/formato';
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
  private toast = inject(ToastService);
  private router = inject(Router);
  protected auth = inject(AuthService);
  protected precio = precio;

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
  protected tamano = 8;
  private ultimaPeticion = 0;
  private busquedas = new Subject<void>();

  protected categorias = computed(() => {
    const todas = this.productos.catalogo().map((p) => p.categoria).filter((c): c is string => !!c);
    return [...new Set(todas)].sort();
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
  protected reserva = signal<Compra | null>(null);
  protected procesando = signal(false);
  protected favoritos = signal<ReadonlySet<string>>(new Set());
  protected esComprador = computed(() => this.auth.rol() === 'COMPRADOR');
  protected total = computed(() => (this.seleccionado()?.precio ?? 0) * this.cantidad());
  protected esFavorito = computed(() => {
    const p = this.seleccionado();
    return !!p && this.favoritos().has(p.id);
  });

  constructor() {
    this.busquedas.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.pagina.set(0);
      this.cargar();
    });
  }

  ngOnInit() {
    this.productos.cargarCatalogo().subscribe({ error: () => undefined });
    this.cargar();
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
    if (p < 0 || p >= total || p === this.pagina()) return;
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

  protected reservar() {
    const p = this.seleccionado();
    if (!p) return;
    this.procesando.set(true);
    this.compras.realizar(p.id, this.cantidad()).subscribe({
      next: (c) => {
        this.reserva.set(c);
        this.procesando.set(false);
        this.toast.info('Compra reservada. Confirma el pago para completarla.');
      },
      error: (e) => {
        this.procesando.set(false);
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  protected pagar() {
    const c = this.reserva();
    if (!c) return;
    this.procesando.set(true);
    this.compras.confirmar(c.id).subscribe({
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

  protected cancelarReserva() {
    const c = this.reserva();
    if (!c) return;
    this.procesando.set(true);
    this.compras.cancelar(c.id).subscribe({
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

  protected alternarFavorito() {
    const p = this.seleccionado();
    if (!p) return;
    const era = this.favoritos().has(p.id);
    const llamada = era ? this.productos.quitarWishlist(p.id) : this.productos.anadirWishlist(p.id);
    llamada.subscribe({
      next: () => {
        const nuevo = new Set(this.favoritos());
        if (era) nuevo.delete(p.id); else nuevo.add(p.id);
        this.favoritos.set(nuevo);
        this.toast.ok(era ? 'Quitado de tu lista de deseos.' : 'Añadido a tu lista de deseos.');
      },
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }
}
