import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';
import { ToastService } from '../../servicios/toast.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  private toast = inject(ToastService);

  protected correo = signal('');
  protected contrasena = signal('');
  protected cargando = signal(false);
  protected error = signal('');
  protected expirada = this.ruta.snapshot.queryParamMap.get('expirada') === '1';

  protected demos = [
    { etiqueta: 'Comprador demo', correo: 'jose.demo@correo.com', clave: 'JoseGamer2026' },
    { etiqueta: 'Vendedor demo', correo: 'demo@gamerzone.com', clave: 'GamerZone2026' },
  ];

  protected usarDemo(d: { correo: string; clave: string }) {
    this.correo.set(d.correo);
    this.contrasena.set(d.clave);
    this.error.set('');
  }

  protected ingresar() {
    if (!this.correo().trim() || !this.contrasena()) {
      this.error.set('Escribe tu correo y tu contraseña.');
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.auth.login(this.correo().trim(), this.contrasena()).subscribe({
      next: () => {
        this.cargando.set(false);
        this.toast.ok(`¡Bienvenido, ${this.auth.nombre()}!`);
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        const destino = volver ?? (this.auth.rol() === 'VENDEDOR' ? '/vendedor' : '/catalogo');
        this.router.navigateByUrl(destino);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(e.status === 401 || e.status === 400 ? 'Correo o contraseña incorrectos.' : mensajeDeError(e));
      },
    });
  }
}
