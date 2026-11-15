import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../servicios/auth.service';
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

  protected fechaDia = fechaDia;
  protected productoId = signal('');
  protected fechaLanzamiento = signal('');
  protected descripcion = signal('');
  protected guardando = signal(false);
  protected error = signal('');

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

  protected programar() {
    this.error.set('');
    if (!this.productoId()) {
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
    this.servicio.programar(this.productoId(), this.fechaLanzamiento(), descripcion).subscribe({
      next: () => {
        this.guardando.set(false);
        this.productoId.set('');
        this.fechaLanzamiento.set('');
        this.descripcion.set('');
        this.toast.ok('Preventa programada correctamente.');
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
