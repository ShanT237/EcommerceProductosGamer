import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './servicios/auth.service';
import { CarritoService } from './servicios/carrito.service';
import { NotificacionService } from './servicios/notificacion.service';
import { ToastService } from './servicios/toast.service';
import { ConfirmacionService } from './servicios/confirmacion.service';
import { WishlistService } from './servicios/wishlist.service';
import { Confirmacion } from './componentes/confirmacion/confirmacion';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Confirmacion],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected auth = inject(AuthService);
  protected toast = inject(ToastService);
  protected carrito = inject(CarritoService);
  protected notificaciones = inject(NotificacionService); // al inyectarlo carga los avisos del comprador
  protected wishlist = inject(WishlistService);
  private confirmacion = inject(ConfirmacionService);
  protected menuAbierto = signal(false);

  protected cerrarMenu() { this.menuAbierto.set(false); }
  protected async salir() {
    this.cerrarMenu();
    const confirmo = await this.confirmacion.pedir({
      titulo: 'Cerrar sesión',
      mensaje: '¿Quieres cerrar tu sesión?',
      confirmar: 'Salir',
      peligro: false,
    });
    if (confirmo) this.auth.cerrarSesion();
  }
}
