import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MetaRecompensa } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { AuthService } from '../../servicios/auth.service';
import { MetaService } from '../../servicios/meta.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';

/** Pestaña "Metas" del panel del vendedor: crear metas de recompensa, cambiar su umbral y desactivarlas. */
@Component({
  selector: 'app-metas-vendedor',
  imports: [FormsModule],
  templateUrl: './metas-vendedor.html',
  styleUrl: './metas-vendedor.css',
})
export class MetasVendedor implements OnInit {
  private servicio = inject(MetaService);
  private productos = inject(ProductoService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  protected metas = signal<MetaRecompensa[]>([]);
  /** Solo los productos del vendedor autenticado: son los que puede ofrecer como regalo. */
  protected misProductos = computed(() =>
    this.productos.catalogo().filter((p) => p.vendedorId === this.auth.sesion()?.id),
  );
  protected cargando = signal(true);
  protected error = signal('');
  /** Id de la meta con una petición en curso (evita dobles clics). */
  protected ocupada = signal<string | null>(null);

  // ---- formulario de creación ----
  protected umbral = signal<number | null>(null);
  protected productoId = signal('');
  protected errorForm = signal('');
  protected creando = signal(false);

  // ---- edición del umbral de una meta ----
  protected editandoId = signal<string | null>(null);
  protected umbralEdit = signal<number | null>(null);

  ngOnInit() {
    this.cargar();
    this.productos.cargarCatalogo().subscribe({ error: () => undefined });
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.listar().subscribe({
      next: (l) => { this.metas.set(this.ordenar(l)); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected nombreDe(m: MetaRecompensa): string {
    return this.productos.catalogo().find((p) => p.id === m.productoRegaloId)?.nombre ?? 'Producto de regalo';
  }

  private ordenar(lista: MetaRecompensa[]): MetaRecompensa[] {
    return [...lista].sort((a, b) => a.comprasRequeridas - b.comprasRequeridas);
  }

  private umbralValido(valor: number | null): valor is number {
    return typeof valor === 'number' && Number.isInteger(valor) && valor > 0;
  }

  protected crear() {
    this.errorForm.set('');
    const umbral = this.umbral();
    if (!this.umbralValido(umbral)) {
      return this.errorForm.set('El umbral debe ser un número entero mayor que 0.');
    }
    if (!this.productoId()) {
      return this.errorForm.set('Elige el producto que se regala.');
    }
    if (this.creando()) return;

    this.creando.set(true);
    this.servicio.crear({ comprasRequeridas: umbral, productoRegaloId: this.productoId() }).subscribe({
      next: (meta) => {
        this.creando.set(false);
        this.toast.ok(`Meta creada: a las ${meta.comprasRequeridas} compras se ofrece un regalo.`);
        this.metas.update((l) => this.ordenar([...l, meta]));
        this.umbral.set(null);
        this.productoId.set('');
      },
      error: (e) => { this.creando.set(false); this.errorForm.set(mensajeDeError(e)); },
    });
  }

  protected editar(m: MetaRecompensa) {
    this.editandoId.set(m.id);
    this.umbralEdit.set(m.comprasRequeridas);
  }

  protected cancelarEdicion() {
    this.editandoId.set(null);
    this.umbralEdit.set(null);
  }

  protected guardarUmbral(m: MetaRecompensa) {
    const nuevo = this.umbralEdit();
    if (!this.umbralValido(nuevo)) {
      return this.toast.error('El umbral debe ser un número entero mayor que 0.');
    }
    if (this.ocupada()) return;

    this.ocupada.set(m.id);
    this.servicio.actualizarUmbral(m.id, nuevo).subscribe({
      next: (actualizada) => {
        this.ocupada.set(null);
        this.cancelarEdicion();
        this.toast.ok('Umbral actualizado.');
        this.metas.update((l) => this.ordenar(l.map((x) => (x.id === actualizada.id ? actualizada : x))));
      },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected desactivar(m: MetaRecompensa) {
    if (this.ocupada()) return;
    this.ocupada.set(m.id);
    this.servicio.desactivar(m.id).subscribe({
      next: () => {
        this.ocupada.set(null);
        this.toast.ok(`Meta de ${m.comprasRequeridas} compras desactivada.`);
        this.metas.update((l) => l.filter((x) => x.id !== m.id));
      },
      // 404 (ya no existe) o 422 (ya estaba desactivada): se muestra el mensaje del backend.
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
