import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Notificacion, TipoNotificacion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';
import { fechaHora } from '../../servicios/formato';
import { NotificacionService } from '../../servicios/notificacion.service';
import { ToastService } from '../../servicios/toast.service';

/** Pantalla `/notificaciones`: avisos del comprador o del vendedor; al hacer clic en uno se marca como leído. */
@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.css',
})
export class Notificaciones implements OnInit {
  protected servicio = inject(NotificacionService);
  private auth = inject(AuthService);
  protected esVendedor = computed(() => this.auth.sesion()?.rol === 'VENDEDOR');
  private toast = inject(ToastService);
  protected fechaHora = fechaHora;

  /** Texto corto que acompaña a cada tipo de aviso. */
  protected etiquetas: Record<TipoNotificacion, string> = {
    DESCUENTO: 'Descuento',
    STOCK_DISPONIBLE: 'Stock repuesto',
    PREVENTA_DISPONIBLE: 'Preventa',
    META_ALCANZADA: 'Meta',
    REGALO_ENVIADO: 'Regalo',
    STOCK_BAJO: 'Stock bajo',
    STOCK_AGOTADO: 'Agotado',
    NUEVA_VENTA: 'Venta',
    REEMBOLSO_SOLICITADO: 'Reembolso',
    NUEVA_RESENA: 'Reseña',
    NUEVA_VALORACION: 'Valoración',
  };

  /** Avisos que piden atención del vendedor (se pintan en ámbar). */
  protected esAlerta(tipo: TipoNotificacion) {
    return tipo === 'STOCK_BAJO' || tipo === 'STOCK_AGOTADO' || tipo === 'REEMBOLSO_SOLICITADO';
  }

  protected cargando = signal(true);
  protected ocupada = signal<string | null>(null);

  ngOnInit() {
    // Se refresca al entrar por si llegaron avisos nuevos desde que se inició sesión.
    this.servicio.cargar().subscribe({
      next: () => this.cargando.set(false),
      error: (e) => { this.cargando.set(false); this.toast.error(mensajeDeError(e)); },
    });
  }

  protected abrir(n: Notificacion) {
    if (n.leida || this.ocupada() === n.id) return;
    this.ocupada.set(n.id);
    this.servicio.marcarLeida(n.id).subscribe({
      next: () => this.ocupada.set(null),
      error: (e) => { this.ocupada.set(null); this.toast.error(mensajeDeError(e)); },
    });
  }
}
