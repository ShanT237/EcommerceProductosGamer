import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';
import { ToastService } from '../../servicios/toast.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  private toast = inject(ToastService);

  protected formulario = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });
  protected cargando = signal(false);
  protected error = signal('');

  protected demos = [
    { etiqueta: 'Comprador demo', correo: 'jose.demo@correo.com', clave: 'JoseGamer2026' },
    { etiqueta: 'Vendedor demo', correo: 'demo@gamerzone.com', clave: 'GamerZone2026' },
  ];

  protected usarDemo(d: { correo: string; clave: string }) {
    this.formulario.setValue({ correo: d.correo, contrasena: d.clave });
    this.formulario.markAsPristine();
    this.formulario.markAsUntouched();
    this.error.set('');
  }

  protected ingresar() {
    this.error.set('');
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { correo, contrasena } = this.formulario.getRawValue();
    this.cargando.set(true);
    this.auth.login(correo.trim(), contrasena).subscribe({
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
