import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Cupon } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { CuponService } from '../../servicios/cupon.service';
import { fechaCompleta } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';

type EstadoCupon = 'vigente' | 'vencido' | 'agotado';

/** Pestaña "Cupones" del panel del vendedor: crear cupones propios y ver cuántos usos llevan. */
@Component({
  selector: 'app-cupones-vendedor',
  imports: [FormsModule],
  templateUrl: './cupones-vendedor.html',
  styleUrl: './cupones-vendedor.css',
})
export class CuponesVendedor implements OnInit {
  private servicio = inject(CuponService);
  private toast = inject(ToastService);
  protected fechaCompleta = fechaCompleta;

  /** Hoy (AAAA-MM-DD, día local): fecha mínima de vencimiento y referencia para saber si un cupón venció. */
  protected hoy = new Date().toLocaleDateString('en-CA');

  protected cupones = signal<Cupon[]>([]);
  protected cargando = signal(true);
  protected error = signal('');

  // ---- formulario de creación ----
  protected codigo = signal('');
  protected porcentaje = signal<number | null>(null);
  protected vence = signal('');
  protected usosMaximos = signal<number | null>(null);
  protected errorForm = signal('');
  protected creando = signal(false);

  ngOnInit() {
    this.cargar();
  }

  protected cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.misCupones().subscribe({
      next: (l) => { this.cupones.set(l); this.cargando.set(false); },
      error: (e) => { this.error.set(mensajeDeError(e)); this.cargando.set(false); },
    });
  }

  /** El último día de uso es inclusivo, igual que en el backend. */
  protected estado(c: Cupon): EstadoCupon {
    if (c.vence < this.hoy) return 'vencido';
    if (c.usos >= c.usosMaximos) return 'agotado';
    return 'vigente';
  }

  protected porcentajeUsos(c: Cupon): number {
    return c.usosMaximos > 0 ? Math.min(100, (c.usos / c.usosMaximos) * 100) : 0;
  }

  protected crear() {
    this.errorForm.set('');
    const codigo = this.codigo().trim();
    const porcentaje = this.porcentaje();
    const vence = this.vence();
    const usosMaximos = this.usosMaximos();

    if (!codigo) return this.errorForm.set('El código es obligatorio.');
    if (porcentaje == null || !(porcentaje > 0) || porcentaje > 99.99) {
      return this.errorForm.set('El porcentaje debe ser mayor a 0 y menor que 100.');
    }
    if (!vence) return this.errorForm.set('La fecha de vencimiento es obligatoria.');
    if (vence < this.hoy) return this.errorForm.set('El cupón no puede vencer antes de hoy.');
    if (usosMaximos == null || !Number.isInteger(usosMaximos) || usosMaximos < 1) {
      return this.errorForm.set('Los usos máximos deben ser un número entero positivo.');
    }
    if (this.creando()) return;

    this.creando.set(true);
    this.servicio.crear({ codigo, porcentaje, vence, usosMaximos }).subscribe({
      next: (c) => {
        this.creando.set(false);
        this.toast.ok(`Cupón ${c.codigo} creado.`);
        this.codigo.set('');
        this.porcentaje.set(null);
        this.vence.set('');
        this.usosMaximos.set(null);
        this.cupones.update((lista) => [c, ...lista.filter((x) => x.id !== c.id)]);
      },
      error: (e) => { this.creando.set(false); this.errorForm.set(mensajeDeError(e)); },
    });
  }
}
