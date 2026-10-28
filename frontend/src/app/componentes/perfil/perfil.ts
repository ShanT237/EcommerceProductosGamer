import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../servicios/auth.service';
import { UsuarioService } from '../../servicios/usuario.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {
  protected auth = inject(AuthService);
  private usuarios = inject(UsuarioService);
  private toast = inject(ToastService);

  // ---- datos personales ----
  protected nombre = signal('');
  protected correo = signal(this.auth.sesion()?.correo ?? '');
  protected guardandoPerfil = signal(false);
  protected errorPerfil = signal('');

  // ---- contraseña ----
  protected actual = signal('');
  protected nueva = signal('');
  protected repetida = signal('');
  protected guardandoClave = signal(false);
  protected errorClave = signal('');

  protected guardarPerfil() {
    this.errorPerfil.set('');
    if (!this.nombre().trim()) return this.errorPerfil.set('Escribe tu nombre.');
    if (!this.correo().trim()) return this.errorPerfil.set('Escribe tu correo.');

    const cambioCorreo = this.correo().trim() !== this.auth.sesion()?.correo;
    this.guardandoPerfil.set(true);
    this.usuarios.editarPerfil(this.nombre().trim(), this.correo().trim()).subscribe({
      next: () => {
        this.guardandoPerfil.set(false);
        if (cambioCorreo) {
          // El token guarda el correo viejo: hay que volver a entrar para renovarlo.
          this.toast.info('Perfil actualizado. Ingresa de nuevo con tu nuevo correo.');
          this.auth.cerrarSesion();
        } else {
          this.toast.ok('Perfil actualizado.');
        }
      },
      error: (e) => { this.guardandoPerfil.set(false); this.errorPerfil.set(mensajeDeError(e)); },
    });
  }

  protected cambiarClave() {
    this.errorClave.set('');
    if (!this.actual()) return this.errorClave.set('Escribe tu contraseña actual.');
    if (this.nueva().length < 8) return this.errorClave.set('La nueva contraseña debe tener al menos 8 caracteres.');
    if (this.nueva() !== this.repetida()) return this.errorClave.set('Las contraseñas nuevas no coinciden.');

    this.guardandoClave.set(true);
    this.usuarios.cambiarContrasena(this.actual(), this.nueva()).subscribe({
      next: () => {
        this.guardandoClave.set(false);
        this.actual.set(''); this.nueva.set(''); this.repetida.set('');
        this.toast.ok('Contraseña actualizada.');
      },
      error: (e) => { this.guardandoClave.set(false); this.errorClave.set(mensajeDeError(e)); },
    });
  }
}
