import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Compra, EstadoCompra } from '../../modelos/modelos';
import { CompraService } from '../../servicios/compra.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';
import { fechaHora, precio } from '../../servicios/formato';

type Dialogo = { tipo: 'resena' | 'vendedor' | 'cupon'; compra: Compra };

@Component({
  selector: 'app-mis-compras',
  imports: [FormsModule, RouterLink],
  templateUrl: './mis-compras.html',
  styleUrl: './mis-compras.css',
  host: { '(document:keydown.escape)': 'cerrarDialogo()' },
})
export class MisCompras implements OnInit {
  private compras = inject(CompraService);
  private productos = inject(ProductoService);
  private toast = inject(ToastService);
  protected precio = precio;
  protected fechaHora = fechaHora;

  protected lista = signal<Compra[]>([]);
  protected puntos = signal(0);
  protected cargando = signal(true);
  protected ocupada = signal<string | null>(null);
  protected filtro = signal<EstadoCompra | ''>('');
  protected estados: Array<EstadoCompra | ''> = ['', 'PENDIENTE', 'COMPLETADA', 'REEMBOLSADA', 'CANCELADA', 'EXPIRADA'];

  protected dialogo = signal<Dialogo | null>(null);
  protected estrellas = signal(5);
  protected comentario = signal('');
  protected cupon = signal('');
  protected redimiendo = signal(false);
  protected puntosACanjear = signal<number | null>(null);

  protected visibles = computed(() =>
    [...this.lista()]
      .filter((c) => !this.filtro() || c.estado === this.filtro())
      .sort((a, b) => b.fecha.localeCompare(a.fecha)),
  );

  protected gastado = computed(() =>
    this.lista().filter((c) => c.estado === 'COMPLETADA').reduce((s, c) => s + c.subtotal, 0),
  );

  ngOnInit() {
    this.productos.cargarCatalogo().subscribe({ error: () => undefined });
    this.recargar();
    this.compras.historialPuntos().subscribe({
      next: (m) => this.puntos.set(m.reduce((s, x) => s + (x.tipo === 'GANADO' ? x.cantidad : -x.cantidad), 0)),
      error: () => undefined,
    });
  }

  protected nombreDe(productoId: string): string {
    return this.productos.catalogo().find((p) => p.id === productoId)?.nombre ?? 'Producto';
  }

  protected recargar() {
    this.cargando.set(true);
    this.compras.listar().subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.toast.error(mensajeDeError(e)); },
    });
  }

  /** Ejecuta una acción sobre una compra y recarga la lista; los errores de negocio se muestran tal cual los dice el backend. */
  private accion(c: Compra, llamada: import('rxjs').Observable<unknown>, exito: string) {
    this.ocupada.set(c.id);
    llamada.subscribe({
      next: () => { this.ocupada.set(null); this.toast.ok(exito); this.cerrarDialogo(); this.recargar(); },
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected confirmar(c: Compra) { this.accion(c, this.compras.confirmar(c.id), 'Compra confirmada. ¡Disfrútala!'); }
  protected cancelar(c: Compra) { this.accion(c, this.compras.cancelar(c.id), 'Compra cancelada.'); }
  protected reembolsar(c: Compra) {
    if (confirm('¿Seguro que quieres solicitar el reembolso de esta compra?')) {
      this.accion(c, this.compras.reembolsar(c.id), 'Reembolso solicitado correctamente.');
    }
  }

  protected abrirDialogo(tipo: Dialogo['tipo'], compra: Compra) {
    this.estrellas.set(5);
    this.comentario.set('');
    this.cupon.set('');
    this.dialogo.set({ tipo, compra });
  }

  protected cerrarDialogo() { this.dialogo.set(null); this.redimiendo.set(false); }

  protected abrirPuntos() { this.puntosACanjear.set(null); this.redimiendo.set(true); }

  protected redimir() {
    const n = this.puntosACanjear();
    if (n == null || n <= 0) return this.toast.error('Escribe cuántos puntos quieres redimir.');
    if (n > this.puntos()) return this.toast.error('No tienes tantos puntos.');
    this.compras.redimirPuntos(n).subscribe({
      next: (r) => {
        this.puntos.set(r.saldo);
        this.redimiendo.set(false);
        this.toast.ok(`Redimiste ${n} puntos. Te quedan ${r.saldo}.`);
      },
      error: (e) => this.toast.error(mensajeDeError(e)),
    });
  }

  protected enviarDialogo() {
    const d = this.dialogo();
    if (!d) return;
    if (d.tipo === 'resena') {
      if (!this.comentario().trim()) return this.toast.error('Escribe un comentario para tu reseña.');
      this.accion(d.compra, this.compras.resenar(d.compra.id, this.estrellas(), this.comentario().trim()), 'Reseña publicada. ¡Gracias!');
    } else if (d.tipo === 'vendedor') {
      this.accion(d.compra, this.compras.valorarVendedor(d.compra.id, this.estrellas(), this.comentario().trim()), 'Valoración enviada al vendedor.');
    } else {
      if (!this.cupon().trim()) return this.toast.error('Escribe el código del cupón.');
      this.accion(d.compra, this.compras.aplicarCupon(d.compra.id, this.cupon().trim()), 'Cupón aplicado.');
    }
  }
}
