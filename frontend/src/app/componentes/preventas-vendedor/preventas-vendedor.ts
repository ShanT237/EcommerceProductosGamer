import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Preventa } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { mensajeDeError } from '../../servicios/api.util';
import { fechaDia } from '../../servicios/formato';
import { PreventaService } from '../../servicios/preventa.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';

@Component({
  selector: 'app-preventas-vendedor',
  imports: [FormsModule],
  templateUrl: './preventas-vendedor.html',
  styleUrl: './preventas-vendedor.css',
})
export class PreventasVendedor implements OnInit {
  private auth = inject(AuthService);
  private productos = inject(ProductoService);
  protected servicio = inject(PreventaService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  protected fechaDia = fechaDia;
  protected productoId = signal('');
  protected fechaLanzamiento = signal('');
  protected descripcion = signal('');
  protected guardando = signal(false);
  protected error = signal('');
  /** Id de la preventa que se está editando; `null` cuando el formulario programa una nueva. */
  protected editandoId = signal<string | null>(null);
  /** Id de la preventa que se está cancelando (bloquea su botón mientras dura la petición). */
  protected cancelandoId = signal<string | null>(null);

  protected productoEditando = computed(() => {
    const id = this.editandoId();
    if (!id) return undefined;
    const preventa = this.preventasPropias().find((item) => item.preventa.id === id);
    return preventa?.producto;
  });

  protected misProductos = computed(() => {
    const vendedorId = this.auth.sesion()?.id;
    return this.productos.catalogo().filter((producto) => producto.vendedorId === vendedorId);
  });

  protected preventasPropias = computed(() => {
    const idsProductos = new Set(this.misProductos().map((producto) => producto.id));
    return [...this.servicio.futuras().values()]
      .filter((preventa) => idsProductos.has(preventa.productoId))
      .sort((a, b) => a.fechaLanzamiento.localeCompare(b.fechaLanzamiento))
      .map((preventa) => ({
        preventa,
        producto: this.misProductos().find((producto) => producto.id === preventa.productoId),
      }));
  });

  protected productosDisponibles = computed(() => {
    const enPreventa = new Set(this.preventasPropias().map(({ preventa }) => preventa.productoId));
    return this.misProductos().filter((producto) => !enPreventa.has(producto.id));
  });

  ngOnInit() {
    this.productos.cargarCatalogo().subscribe({
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
    this.servicio.cargar().subscribe({
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  protected fechaMinima(): string {
    const manana = new Date();
    manana.setDate(manana.getDate() + 1);
    const anio = manana.getFullYear();
    const mes = String(manana.getMonth() + 1).padStart(2, '0');
    const dia = String(manana.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  /** Envía el formulario: programa una preventa nueva o, si se está editando una, guarda sus cambios. */
  protected programar() {
    this.error.set('');
    const editandoId = this.editandoId();
    if (!editandoId && !this.productoId()) {
      this.error.set('Elige un producto.');
      return;
    }
    if (!this.fechaLanzamiento() || this.fechaLanzamiento() < this.fechaMinima()) {
      this.error.set('La fecha de lanzamiento debe ser, como mínimo, mañana.');
      return;
    }
    const descripcion = this.descripcion().trim();
    if (!descripcion) {
      this.error.set('Escribe una descripción para la preventa.');
      return;
    }
    if (this.guardando()) return;

    this.guardando.set(true);
    const peticion = editandoId
      ? this.servicio.modificar(editandoId, this.fechaLanzamiento(), descripcion)
      : this.servicio.programar(this.productoId(), this.fechaLanzamiento(), descripcion);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.limpiarFormulario();
        this.toast.ok(editandoId ? 'Preventa actualizada correctamente.' : 'Preventa programada correctamente.');
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }

  /** Carga la preventa en el formulario para modificar su fecha y su descripción. */
  protected editar(preventa: Preventa) {
    this.error.set('');
    this.editandoId.set(preventa.id);
    this.productoId.set(preventa.productoId);
    this.fechaLanzamiento.set(preventa.fechaLanzamiento);
    this.descripcion.set(preventa.descripcion);
  }

  protected cancelarEdicion() {
    this.limpiarFormulario();
  }

  protected async cancelar(preventa: Preventa, nombreProducto: string) {
    if (this.cancelandoId()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Cancelar preventa',
      mensaje: `¿Cancelar la preventa de "${nombreProducto}"? El producto quedará disponible para comprar de inmediato.`,
      confirmar: 'Cancelar preventa',
      cancelar: 'Conservar',
    });
    if (!acepto || this.cancelandoId()) return;

    this.cancelandoId.set(preventa.id);
    this.servicio.cancelar(preventa.id).subscribe({
      next: () => {
        this.cancelandoId.set(null);
        if (this.editandoId() === preventa.id) this.limpiarFormulario();
        this.toast.ok(`Preventa de "${nombreProducto}" cancelada.`);
      },
      error: (e) => {
        this.cancelandoId.set(null);
        this.toast.error(mensajeDeError(e));
      },
    });
  }

  private limpiarFormulario() {
    this.editandoId.set(null);
    this.productoId.set('');
    this.fechaLanzamiento.set('');
    this.descripcion.set('');
    this.error.set('');
  }
}
