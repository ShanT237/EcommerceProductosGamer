import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Combo, Producto } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { AuthService } from '../../servicios/auth.service';
import { ComboService } from '../../servicios/combo.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { precio } from '../../servicios/formato';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';

/** Pestaña "Combos" del panel del vendedor: crear combos y añadir o quitar sus productos. */
@Component({
  selector: 'app-combos-vendedor',
  imports: [FormsModule],
  templateUrl: './combos-vendedor.html',
  styleUrl: './combos-vendedor.css',
})
export class CombosVendedor implements OnInit {
  private servicio = inject(ComboService);
  private productos = inject(ProductoService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  protected precio = precio;

  protected combos = signal<Combo[]>([]);
  protected cargando = signal(true);
  protected error = signal('');

  /** Productos propios (el catálogo en memoria que carga el panel). */
  protected misProductos = computed(() => {
    const yo = this.auth.sesion()?.id;
    return this.productos.catalogo().filter((p) => p.vendedorId === yo);
  });

  // ---- formulario de creación ----
  protected nombre = signal('');
  protected descripcion = signal('');
  protected descuento = signal<number | null>(null);
  protected seleccion = signal<ReadonlySet<string>>(new Set());
  protected errorForm = signal('');
  protected creando = signal(false);

  // ---- edición de un combo existente ----
  /** Producto elegido en el selector "Añadir" de cada combo (por id de combo). */
  protected porAnadir = signal<Record<string, string>>({});
  /** Combo que tiene una operación en curso (evita dobles clics). */
  protected ocupado = signal<string | null>(null);

  /** Combo cuyos datos se están editando (solo uno a la vez). */
  protected editando = signal<Combo | null>(null);
  protected editNombre = signal('');
  protected editDescripcion = signal('');
  protected editDescuento = signal<number | null>(null);
  protected errorEdicion = signal('');
  protected guardando = signal(false);

  ngOnInit() {
    this.cargar();
    if (this.productos.catalogo().length === 0) {
      this.productos.cargarCatalogo().subscribe({ error: () => undefined });
    }
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.mios().subscribe({
      next: (l) => { this.combos.set(l); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected alternar(id: string) {
    this.seleccion.update((s) => {
      const nueva = new Set(s);
      if (!nueva.delete(id)) nueva.add(id);
      return nueva;
    });
  }

  protected crear() {
    this.errorForm.set('');
    const descuento = this.descuento();
    if (!this.nombre().trim()) return this.errorForm.set('El nombre es obligatorio.');
    if (!this.descripcion().trim()) return this.errorForm.set('La descripción es obligatoria.');
    if (descuento == null || descuento < 0 || descuento >= 100) {
      return this.errorForm.set('El descuento debe ser mayor o igual a 0 y menor que 100.');
    }
    if (this.seleccion().size < 2) return this.errorForm.set('Elige al menos dos productos para el combo.');
    if (this.creando()) return;

    this.creando.set(true);
    this.servicio.crear({
      nombre: this.nombre().trim(),
      descripcion: this.descripcion().trim(),
      descuento,
      productoIds: [...this.seleccion()],
    }).subscribe({
      next: (c) => {
        this.creando.set(false);
        this.toast.ok(`Combo "${c.nombre}" creado.`);
        this.nombre.set('');
        this.descripcion.set('');
        this.descuento.set(null);
        this.seleccion.set(new Set());
        this.cargar();
      },
      error: (e) => { this.creando.set(false); this.errorForm.set(mensajeDeError(e)); },
    });
  }

  /** Productos propios que todavía no están en el combo. */
  protected disponiblesPara(c: Combo): Producto[] {
    const enCombo = new Set(c.productos.map((p) => p.id));
    return this.misProductos().filter((p) => !enCombo.has(p.id));
  }

  protected elegirAnadir(comboId: string, evento: Event) {
    const valor = (evento.target as HTMLSelectElement).value;
    this.porAnadir.update((m) => ({ ...m, [comboId]: valor }));
  }

  protected agregar(c: Combo) {
    const productoId = this.porAnadir()[c.id];
    if (!productoId || this.ocupado()) return;
    this.ocupado.set(c.id);
    this.servicio.agregarProducto(c.id, productoId).subscribe({
      next: () => {
        this.ocupado.set(null);
        this.porAnadir.update((m) => ({ ...m, [c.id]: '' }));
        this.toast.ok('Producto añadido al combo.');
        this.cargar();
      },
      error: (e) => { this.ocupado.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected quitar(c: Combo, productoId: string) {
    if (this.ocupado()) return;
    this.ocupado.set(c.id);
    this.servicio.quitarProducto(c.id, productoId).subscribe({
      next: () => {
        this.ocupado.set(null);
        this.toast.ok('Producto quitado del combo.');
        this.cargar();
      },
      // Un combo no puede quedar con menos de dos productos: el backend responde 422 con el motivo.
      error: (e) => { this.ocupado.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected editar(c: Combo) {
    if (this.guardando()) return;
    this.editando.set(c);
    this.editNombre.set(c.nombre);
    this.editDescripcion.set(c.descripcion);
    this.editDescuento.set(c.descuento);
    this.errorEdicion.set('');
  }

  protected cancelarEdicion() {
    if (!this.guardando()) this.editando.set(null);
  }

  protected guardarEdicion() {
    const c = this.editando();
    if (!c || this.guardando()) return;
    this.errorEdicion.set('');
    const descuento = this.editDescuento();
    if (!this.editNombre().trim()) return this.errorEdicion.set('El nombre es obligatorio.');
    if (!this.editDescripcion().trim()) return this.errorEdicion.set('La descripción es obligatoria.');
    if (descuento == null || descuento < 0 || descuento >= 100) {
      return this.errorEdicion.set('El descuento debe ser mayor o igual a 0 y menor que 100.');
    }

    this.guardando.set(true);
    this.servicio.modificar(c.id, {
      nombre: this.editNombre().trim(),
      descripcion: this.editDescripcion().trim(),
      descuento,
    }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.editando.set(null);
        this.toast.ok('Combo actualizado.');
        this.cargar();
      },
      error: (e) => { this.guardando.set(false); this.errorEdicion.set(mensajeDeError(e)); },
    });
  }

  protected async eliminar(c: Combo) {
    if (this.ocupado()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Eliminar combo',
      mensaje: `¿Eliminar "${c.nombre}"? Tus productos no se borran, solo deja de existir el combo.`,
      confirmar: 'Eliminar',
    });
    if (!acepto) return;

    this.ocupado.set(c.id);
    this.servicio.eliminar(c.id).subscribe({
      next: () => {
        this.ocupado.set(null);
        if (this.editando()?.id === c.id) this.editando.set(null);
        this.toast.ok('Combo eliminado.');
        this.cargar();
      },
      // El backend responde 422 si el combo tiene regalos pendientes de entrega.
      error: (e) => { this.ocupado.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
