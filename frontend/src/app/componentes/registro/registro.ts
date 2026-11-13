import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';
import { ToastService } from '../../servicios/toast.service';
import { TipoVendedor } from '../../modelos/modelos';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: './registro.css',
})
export class Registro {
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  protected modo = signal<'COMPRADOR' | 'VENDEDOR'>('COMPRADOR');
  protected nombre = signal('');
  protected correo = signal('');
  protected contrasena = signal('');
  protected tipo = signal<TipoVendedor>('PERSONA_NATURAL');
  protected documento = signal('');
  protected cargando = signal(false);
  protected error = signal('');

  protected etiquetaDocumento = () => (this.tipo() === 'EMPRESA' ? 'NIT' : 'Cédula');

  protected crear() {
    this.error.set('');
    if (!this.nombre().trim() || !this.correo().trim()) return this.error.set('Completa tu nombre y tu correo.');
    if (this.contrasena().length < 8) return this.error.set('La contraseña debe tener al menos 8 caracteres.');
    if (this.modo() === 'VENDEDOR' && !this.documento().trim()) return this.error.set(`Escribe tu ${this.etiquetaDocumento()}.`);

    this.cargando.set(true);
    const peticion =
      this.modo() === 'COMPRADOR'
        ? this.auth.registrarComprador(this.nombre().trim(), this.correo().trim(), this.contrasena())
        : this.auth.registrarVendedor({
            nombre: this.nombre().trim(),
            correo: this.correo().trim(),
            tipo: this.tipo(),
            documento: this.documento().trim(),
            contrasena: this.contrasena(),
          });

    peticion.subscribe({
      next: () => {
        // Cuenta creada: iniciamos sesión de una vez.
        this.auth.login(this.correo().trim(), this.contrasena()).subscribe({
          next: () => {
            this.cargando.set(false);
            this.toast.ok('¡Cuenta creada! Ya estás dentro.');
            this.router.navigateByUrl(this.modo() === 'VENDEDOR' ? '/vendedor' : '/catalogo');
          },
          error: () => {
            this.cargando.set(false);
            this.toast.info('Cuenta creada. Ahora ingresa con tus datos.');
            this.router.navigateByUrl('/login');
          },
        });
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
