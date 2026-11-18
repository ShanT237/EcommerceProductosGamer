import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { mensajeDeError } from '../../servicios/api.util';

/** Paso 1 de la recuperación de contraseña: la persona escribe su correo y recibe un enlace. */
@Component({
  selector: 'app-olvide-contrasena',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './olvide-contrasena.html',
  styleUrl: './olvide-contrasena.css',
})
export class OlvideContrasena implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private canal?: BroadcastChannel;

  protected formulario = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
  });
  protected cargando = signal(false);
  protected enviado = signal(false);
  protected correoEnviado = signal('');
  protected error = signal('');

  ngOnInit(): void {
    if (typeof window !== 'undefined' && 'BroadcastChannel' in window) {
      this.canal = new BroadcastChannel('gamerzone_recuperacion');
      this.canal.onmessage = (evento) => {
        if (evento.data?.tipo === 'ENLACE_ABIERTO') {
          this.reiniciar();
        }
      };
    }
  }

  ngOnDestroy(): void {
    this.canal?.close();
  }

  protected enviar() {
    this.error.set('');
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const correo = this.formulario.getRawValue().correo.trim();
    this.cargando.set(true);
    this.auth.solicitarRecuperacion(correo).subscribe({
      next: () => {
        this.cargando.set(false);
        this.correoEnviado.set(correo);
        this.enviado.set(true);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }

  /** Vuelve al formulario para pedir otro enlace (por ejemplo, si el correo estaba mal escrito). */
  protected pedirOtro() {
    this.enviado.set(false);
  }

  private reiniciar() {
    this.enviado.set(false);
    this.correoEnviado.set('');
    this.error.set('');
    this.formulario.reset({ correo: '' });
  }
}
