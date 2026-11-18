import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';
import { ToastService } from '../../servicios/toast.service';

/** Mismas reglas que el backend: mínimo 8 caracteres, una mayúscula y un número. */
function conMayuscula(c: AbstractControl): ValidationErrors | null {
  return /\p{Lu}/u.test(c.value ?? '') ? null : { mayuscula: true };
}
function conNumero(c: AbstractControl): ValidationErrors | null {
  return /\d/.test(c.value ?? '') ? null : { numero: true };
}

/** Paso 2 de la recuperación: el enlace del correo abre esta pantalla (`/restablecer?token=…`). */
@Component({
  selector: 'app-restablecer-contrasena',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './restablecer-contrasena.html',
  styleUrl: './restablecer-contrasena.css',
})
export class RestablecerContrasena {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  private toast = inject(ToastService);

  protected token = this.ruta.snapshot.queryParamMap.get('token') ?? '';

  protected formulario = this.fb.nonNullable.group({
    nueva: ['', [Validators.required, Validators.minLength(8), conMayuscula, conNumero]],
    confirmar: ['', Validators.required],
  });
  protected cargando = signal(false);
  protected error = signal('');

  protected noCoinciden(): boolean {
    const { nueva, confirmar } = this.formulario.getRawValue();
    return this.formulario.controls.confirmar.touched && confirmar !== nueva;
  }

  protected guardar() {
    this.error.set('');
    const { nueva, confirmar } = this.formulario.getRawValue();
    if (this.formulario.invalid || nueva !== confirmar) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.auth.restablecerContrasena(this.token, nueva).subscribe({
      next: () => {
        this.cargando.set(false);
        this.toast.ok('¡Listo! Tu contraseña se actualizó. Ya puedes ingresar.');
        this.router.navigateByUrl('/login');
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}
