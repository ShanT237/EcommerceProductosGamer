import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Combo, Regalo } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ComboService } from '../../servicios/combo.service';
import { fechaHora } from '../../servicios/formato';
import { ProductoService } from '../../servicios/producto.service';
import { RegaloService } from '../../servicios/regalo.service';
import { ToastService } from '../../servicios/toast.service';

type Tipo = 'producto' | 'combo';

/** Pestaña "Regalos" del panel del vendedor: ofrecer un regalo y enviar los que los compradores eligieron. */
@Component({
  selector: 'app-regalos-vendedor',
  imports: [FormsModule],
  templateUrl: './regalos-vendedor.html',
  styleUrl: './regalos-vendedor.css',
})
export class RegalosVendedor implements OnInit {
  private servicio = inject(RegaloService);
  private productos = inject(ProductoService);
  private combosApi = inject(ComboService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);
  protected fechaHora = fechaHora;

  protected porEnviar = signal<Regalo[]>([]);
  protected misCombos = signal<Combo[]>([]);
  /** Solo los productos del vendedor autenticado (el backend rechaza regalos de productos ajenos). */
  protected misProductos = computed(() =>
    this.productos.catalogo().filter((p) => p.vendedorId === this.auth.sesion()?.id),
  );
  protected cargando = signal(true);
  protected error = signal('');
  protected ocupada = signal<string | null>(null);

  // ---- formulario de creación ----
  protected tipo = signal<Tipo>('producto');
  protected productoId = signal('');
  protected comboId = signal('');
  protected errorForm = signal('');
  protected creando = signal(false);

  ngOnInit() {
    this.cargar();
    this.productos.cargarCatalogo().subscribe({ error: () => undefined });
    this.combosApi.mios().subscribe({ next: (c) => this.misCombos.set(c), error: () => undefined });
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.porEnviar().subscribe({
      next: (l) => { this.porEnviar.set(l); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  protected nombreDe(r: Regalo): string {
    if (r.idProducto) {
      return this.productos.catalogo().find((p) => p.id === r.idProducto)?.nombre ?? 'Producto de regalo';
    }
    return this.misCombos().find((c) => c.id === r.idCombo)?.nombre ?? 'Combo de regalo';
  }

  protected crear() {
    this.errorForm.set('');
    const esProducto = this.tipo() === 'producto';
    const id = esProducto ? this.productoId() : this.comboId();
    if (!id) {
      return this.errorForm.set(esProducto ? 'Elige un producto para el regalo.' : 'Elige un combo para el regalo.');
    }
    if (this.creando()) return;

    this.creando.set(true);
    this.servicio.crear(esProducto ? { productoId: id } : { comboId: id }).subscribe({
      next: () => {
        this.creando.set(false);
        this.toast.ok('Regalo creado: ya está disponible para los compradores.');
        this.productoId.set('');
        this.comboId.set('');
      },
      error: (e) => { this.creando.set(false); this.errorForm.set(mensajeDeError(e)); },
    });
  }

  protected enviar(r: Regalo) {
    if (this.ocupada()) return;
    this.ocupada.set(r.id);
    this.servicio.enviar(r.id).subscribe({
      next: () => {
        this.ocupada.set(null);
        this.toast.ok(`"${this.nombreDe(r)}" marcado como enviado.`);
        this.porEnviar.update((lista) => lista.filter((x) => x.id !== r.id));
      },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  /** Cancela el regalo (el comprador, si ya lo había elegido, es avisado y recupera su cupo). */
  protected async cancelar(r: Regalo) {
    if (this.ocupada()) return;
    const acepto = await this.confirmacion.pedir({
      titulo: 'Cancelar regalo',
      mensaje: `¿Cancelar el regalo "${this.nombreDe(r)}"? El comprador será avisado y recuperará su cupo.`,
      confirmar: 'Cancelar regalo',
      cancelar: 'Volver',
    });
    if (!acepto) return;
    this.ocupada.set(r.id);
    this.servicio.cancelar(r.id).subscribe({
      next: () => {
        this.ocupada.set(null);
        this.toast.ok(`"${this.nombreDe(r)}" cancelado.`);
        this.porEnviar.update((lista) => lista.filter((x) => x.id !== r.id));
      },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
