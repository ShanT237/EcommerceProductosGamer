import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './servicios/auth.service';
import { ToastService } from './servicios/toast.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected auth = inject(AuthService);
  protected toast = inject(ToastService);
  protected menuAbierto = signal(false);

  protected cerrarMenu() { this.menuAbierto.set(false); }
  protected salir() { this.cerrarMenu(); this.auth.cerrarSesion(); }
}
