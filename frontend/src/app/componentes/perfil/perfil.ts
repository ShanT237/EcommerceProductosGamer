import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { PerfilCuenta } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { UsuarioService } from '../../servicios/usuario.service';
import { VendedorService } from '../../servicios/vendedor.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';

/** Lo que la pantalla necesita de la cuenta: lo implementan `UsuarioService` (comprador) y `VendedorService` (vendedor). */
interface CuentaApi {
  obtenerPerfil(): Observable<PerfilCuenta>;
  editarPerfil(nombre: string, correo: string): Observable<unknown>;
  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Observable<unknown>;
  darDeBaja(): Observable<unknown>;
}

@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil implements OnInit {
  protected auth = inject(AuthService);
  private usuarios = inject(UsuarioService);
  private vendedores = inject(VendedorService);
  private toast = inject(ToastService);
  private confirmacion = inject(ConfirmacionService);

  protected esVendedor = computed(() => this.auth.rol() === 'VENDEDOR');
  /** La misma pantalla sirve a los dos roles: cada uno habla con su propio endpoint (`/usuarios/me` o `/vendedores/me`). */
  private get api(): CuentaApi {
    return this.esVendedor() ? this.vendedores : this.usuarios;
  }

  // ---- datos fijos del vendedor (tipo y documento no se editan) ----
  protected cuenta = signal<PerfilCuenta | null>(null);
  protected etiquetaTipo = computed(() => (this.cuenta()?.tipo === 'EMPRESA' ? 'Empresa' : 'Persona natural'));
  protected etiquetaDocumento = computed(() => (this.cuenta()?.tipo === 'EMPRESA' ? 'NIT' : 'Cédula'));

  // ---- datos personales ----
  protected nombre = signal('');
  protected correo = signal('');
  protected correoOriginal = signal(''); // correo guardado en el servidor
  protected cargandoPerfil = signal(true);
  protected guardandoPerfil = signal(false);
  protected errorPerfil = signal('');

  // ---- contraseña ----
  protected actual = signal('');
  protected nueva = signal('');
  protected repetida = signal('');
  protected guardandoClave = signal(false);
  protected errorClave = signal('');

  // ---- baja de la cuenta ----
  protected dandoBaja = signal(false);
  protected errorBaja = signal('');

  ngOnInit() {
    this.api.obtenerPerfil().subscribe({
      next: (p) => {
        this.cuenta.set(p);
        this.nombre.set(p.nombre);
        this.correo.set(p.correo);
        this.correoOriginal.set(p.correo);
        this.cargandoPerfil.set(false);
      },
      error: (e) => {
        this.cargandoPerfil.set(false);
        this.errorPerfil.set(mensajeDeError(e));
      },
    });
  }

  protected async guardarPerfil() {
    this.errorPerfil.set('');
    if (!this.nombre().trim()) return this.errorPerfil.set('Escribe tu nombre.');
    if (!this.correo().trim()) return this.errorPerfil.set('Escribe tu correo.');

    const cambioCorreo = this.correo().trim().toLowerCase() !== this.correoOriginal().toLowerCase();
    if (cambioCorreo) {
      const acepto = await this.confirmacion.pedir({
        titulo: 'Cambiar correo',
        mensaje: `Tu correo pasará a ser ${this.correo().trim()} y tendrás que ingresar de nuevo con él. ¿Continuar?`,
        confirmar: 'Cambiar correo',
        peligro: false,
      });
      if (!acepto) return;
    }
    this.guardandoPerfil.set(true);
    this.api.editarPerfil(this.nombre().trim(), this.correo().trim()).subscribe({
      next: () => {
        this.guardandoPerfil.set(false);
        if (cambioCorreo) {
          // El token guarda el correo viejo: hay que volver a entrar para renovarlo.
          this.toast.info('Perfil actualizado. Ingresa de nuevo con tu nuevo correo.');
          this.auth.cerrarSesion();
        } else {
          this.correoOriginal.set(this.correo().trim());
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
    this.api.cambiarContrasena(this.actual(), this.nueva()).subscribe({
      next: () => {
        this.guardandoClave.set(false);
        this.actual.set(''); this.nueva.set(''); this.repetida.set('');
        this.toast.ok('Contraseña actualizada.');
      },
      error: (e) => { this.guardandoClave.set(false); this.errorClave.set(mensajeDeError(e)); },
    });
  }

  protected async darDeBaja() {
    this.errorBaja.set('');
    const acepto = await this.confirmacion.pedir({
      titulo: 'Dar de baja mi cuenta',
      mensaje: this.esVendedor()
        ? 'Dejarás de poder ingresar y de publicar productos, y no podrás volver a usar esta cuenta. Tu historial de ventas se conserva. ¿Continuar?'
        : 'Dejarás de poder ingresar y de comprar, y no podrás volver a usar esta cuenta. Tu historial de compras se conserva. ¿Continuar?',
      confirmar: 'Dar de baja',
      peligro: true,
    });
    if (!acepto) return;

    this.dandoBaja.set(true);
    this.api.darDeBaja().subscribe({
      next: () => {
        this.dandoBaja.set(false);
        this.toast.info('Tu cuenta fue dada de baja.');
        this.auth.cerrarSesion();
      },
      error: (e) => { this.dandoBaja.set(false); this.errorBaja.set(mensajeDeError(e)); },
    });
  }
}
