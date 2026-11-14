import { Component, OnInit, inject, signal } from '@angular/core';
import { Notificacion } from '../../modelos/modelos';
import { mensajeDeError } from '../../servicios/api.util';
import { fechaHora } from '../../servicios/formato';
import { NotificacionService } from '../../servicios/notificacion.service';
import { ToastService } from '../../servicios/toast.service';

/** Pantalla `/notificaciones`: avisos del comprador; al hacer clic en uno se marca como leído. */
@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.css',
})
export class Notificaciones implements OnInit {
  protected servicio = inject(NotificacionService);
  private toast = inject(ToastService);
  protected fechaHora = fechaHora;

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
