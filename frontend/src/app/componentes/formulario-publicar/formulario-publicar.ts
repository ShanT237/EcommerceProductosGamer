import { Component, computed, inject, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Exclusividad, Gama, Producto } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';
import { mensajeDeError } from '../../servicios/api.util';
import { TarjetaProducto } from '../tarjeta-producto/tarjeta-producto';

/** Un texto compuesto solo de espacios cuenta como vacío (Validators.required no lo detecta). */
function sinSoloEspacios(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim() === '' ? { required: true } : null;
}

/**
 * Formulario reactivo para publicar un producto (`POST /api/productos`, solo VENDEDOR).
 * Valida en el cliente antes de enviar, bloquea el botón mientras la petición está en curso
 * y muestra el mensaje que devuelve el backend si algo falla.
 */
@Component({
  selector: 'app-formulario-publicar',
  imports: [ReactiveFormsModule, TarjetaProducto],
  templateUrl: './formulario-publicar.html',
  styleUrl: './formulario-publicar.css',
})
export class FormularioPublicar {
  private fb = inject(NonNullableFormBuilder);
  private productos = inject(ProductoService);
  private toast = inject(ToastService);
  private auth = inject(AuthService);

  /** Se emite con el producto creado cuando el backend responde 201. */
  publicado = output<Producto>();

  private readonly valoresIniciales = {
    nombre: '',
    descripcion: '',
    precio: null as number | null,
    stock: 1 as number | null,
    gama: 'MEDIA' as Gama,
    exclusividad: 'NORMAL' as Exclusividad,
  };

  protected form = this.fb.group({
    nombre: [this.valoresIniciales.nombre, [Validators.required, sinSoloEspacios, Validators.maxLength(100)]],
    descripcion: [this.valoresIniciales.descripcion, [Validators.required, sinSoloEspacios, Validators.maxLength(500)]],
    // Un decimal en el stock haría fallar el JSON del backend con un 400 poco claro: se rechaza aquí.
    precio: this.fb.control<number | null>(this.valoresIniciales.precio, [Validators.required, Validators.min(1)]),
    stock: this.fb.control<number | null>(this.valoresIniciales.stock, [Validators.required, Validators.min(0), Validators.pattern(/^\d+$/)]),
    gama: this.fb.control<Gama>(this.valoresIniciales.gama),
    exclusividad: this.fb.control<Exclusividad>(this.valoresIniciales.exclusividad),
  });

  protected publicando = signal(false);
  protected errorServidor = signal('');

  private valores = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected vistaPrevia = computed<Producto>(() => {
    const v = this.valores();
    return {
      id: 'previa',
      vendedorId: this.auth.sesion()?.id ?? '',
      nombre: v.nombre?.trim() || 'Tu producto',
      descripcion: v.descripcion?.trim() || 'Así se verá tu producto en el catálogo.',
      precio: v.precio ?? 0,
      stock: v.stock ?? 0,
      gama: v.gama ?? 'MEDIA',
      exclusividad: v.exclusividad ?? 'NORMAL',
      categoria: null,
    };
  });

  protected enviar() {
    if (this.publicando()) return;
    this.errorServidor.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.publicando.set(true);
    this.productos
      .publicar({
        nombre: v.nombre.trim(),
        descripcion: v.descripcion.trim(),
        precio: v.precio as number,
        stock: v.stock as number,
        gama: v.gama,
        exclusividad: v.exclusividad,
      })
      .pipe(finalize(() => this.publicando.set(false)))
      .subscribe({
        next: (producto) => {
          this.form.reset(this.valoresIniciales);
          this.toast.ok('¡Producto publicado!');
          this.publicado.emit(producto);
        },
        error: (e) => this.errorServidor.set(mensajeDeError(e)),
      });
  }
}
