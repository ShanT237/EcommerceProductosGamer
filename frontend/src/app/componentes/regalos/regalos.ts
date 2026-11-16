import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Combo, EstadoRegalo, MetaRecompensa, ProgresoRegalos, Regalo } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { ComboService } from '../../servicios/combo.service';
import { fechaHora } from '../../servicios/formato';
import { MetaService } from '../../servicios/meta.service';
import { ProductoService } from '../../servicios/producto.service';
import { RegaloService } from '../../servicios/regalo.service';
import { ToastService } from '../../servicios/toast.service';

/** Pantalla `/regalos`: el comprador ve su progreso, elige un regalo y confirma cuando lo recibe. */
@Component({
  selector: 'app-regalos',
  templateUrl: './regalos.html',
  styleUrl: './regalos.css',
})
export class Regalos implements OnInit {
  private servicio = inject(RegaloService);
  private productos = inject(ProductoService);
  private combosApi = inject(ComboService);
  private metasApi = inject(MetaService);
  private toast = inject(ToastService);
  protected fechaHora = fechaHora;

  protected progreso = signal<ProgresoRegalos | null>(null);
  protected disponibles = signal<Regalo[]>([]);
  protected mios = signal<Regalo[]>([]);
  protected combos = signal<Combo[]>([]);
  /** Metas activas, solo informativas: no cambian cuándo se desbloquea el regalo. */
  protected metas = signal<MetaRecompensa[]>([]);
  protected cargando = signal(true);
  protected error = signal('');
  /** Id del regalo con una petición en curso (bloquea los botones para evitar dobles clics). */
  protected ocupada = signal<string | null>(null);

  /** Cuántos regalos puede elegir ahora; sin ninguno, "Elegir" queda deshabilitado. */
  protected porElegir = computed(() => this.progreso()?.regalosDisponibles ?? 0);
  /** Porcentaje de avance hacia el próximo regalo (compras dentro del ciclo actual). */
  protected avance = computed(() => {
    const p = this.progreso();
    if (!p || p.comprasPorRegalo <= 0) return 0;
    return ((p.comprasPorRegalo - p.comprasParaSiguiente) / p.comprasPorRegalo) * 100;
  });

  ngOnInit() {
    this.cargar();
    // Los nombres de producto y combo se piden aparte: si fallan, solo se muestra un texto genérico.
    this.productos.cargarCatalogo().subscribe({ error: () => undefined });
    this.combosApi.listar().subscribe({ next: (c) => this.combos.set(c), error: () => undefined });
    this.metasApi.listar().subscribe({ next: (m) => this.metas.set(m), error: () => undefined });
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.consultar(
      () => this.cargando.set(false),
      (mensaje) => { this.error.set(mensaje); this.cargando.set(false); },
    );
  }

  /** Vuelve a pedir progreso y listas tras una acción, sin mostrar el esqueleto de carga. */
  private recargar() {
    this.consultar(() => undefined, (mensaje) => this.toast.error(mensaje));
  }

  private consultar(alTerminar: () => void, alFallar: (mensaje: string) => void) {
    forkJoin({
      progreso: this.servicio.progreso(),
      disponibles: this.servicio.disponibles(),
      mios: this.servicio.mios(),
    }).subscribe({
      next: (r) => {
        this.progreso.set(r.progreso);
        this.disponibles.set(r.disponibles);
        this.mios.set(r.mios);
        alTerminar();
      },
      error: (e) => alFallar(mensajeDeError(e)),
    });
  }

  protected nombreDe(r: Regalo): string {
    if (r.idProducto) {
      return this.productos.catalogo().find((p) => p.id === r.idProducto)?.nombre ?? 'Producto de regalo';
    }
    return this.combos().find((c) => c.id === r.idCombo)?.nombre ?? 'Combo de regalo';
  }

  protected nombreMeta(m: MetaRecompensa): string {
    return this.productos.catalogo().find((p) => p.id === m.productoRegaloId)?.nombre ?? 'Producto de regalo';
  }

  protected tipoDe(r: Regalo): string {
    return r.idCombo ? 'Combo' : 'Producto';
  }

  protected etiqueta(estado: EstadoRegalo): string {
    return estado === 'ELEGIDO' ? 'Por enviar' : estado.charAt(0) + estado.slice(1).toLowerCase();
  }

  protected elegir(r: Regalo) {
    if (this.ocupada() || this.porElegir() <= 0) return;
    this.ocupada.set(r.id);
    this.servicio.asignar(r.id).subscribe({
      next: () => {
        this.ocupada.set(null);
        this.toast.ok(`¡Elegiste "${this.nombreDe(r)}"! El vendedor lo enviará pronto.`);
        this.recargar();
      },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected confirmar(r: Regalo) {
    if (r.estado !== 'ENVIADO' || this.ocupada()) return;
    this.ocupada.set(r.id);
    this.servicio.confirmarEntrega(r.id).subscribe({
      next: () => {
        this.ocupada.set(null);
        this.toast.ok('Recepción confirmada. ¡Disfruta tu regalo!');
        this.recargar();
      },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
