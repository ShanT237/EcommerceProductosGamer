import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Compra, Producto, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { CarritoService } from '../../servicios/carrito.service';
import { ToastService } from '../../servicios/toast.service';
import { Carrito } from './carrito';

const MENSAJE_STOCK = 'No hay suficiente stock disponible';

function producto(overrides: Partial<Producto> = {}): Producto {
  return {
    id: 'p1',
    vendedorId: 'v1',
    nombre: 'Teclado de ensayo',
    descripcion: 'Teclado mecánico',
    precio: 150000,
    stock: 5,
    gama: 'MEDIA',
    exclusividad: 'NORMAL',
    categoria: null,
    ...overrides,
  };
}

function compra(id: string, productoId: string): Compra {
  return {
    id,
    usuarioId: 'u1',
    productoId,
    cantidad: 1,
    precioUnitario: 150000,
    subtotal: 150000,
    fecha: '2026-11-13T10:00:00',
    estado: 'PENDIENTE',
  };
}

describe('Carrito (pantalla)', () => {
  let fixture: ComponentFixture<Carrito>;
  let http: HttpTestingController;
  let carrito: CarritoService;
  let toast: ToastService;

  const postCompra = () => ({ method: 'POST', url: `${API_URL}/compras` });
  const boton = () => fixture.nativeElement.querySelector('button.pagar') as HTMLButtonElement;

  beforeEach(() => {
    const sesion = signal<Sesion | null>({ id: 'u1', correo: 'jose@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [Carrito],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    carrito = TestBed.inject(CarritoService);
    toast = TestBed.inject(ToastService);
    // El efecto del servicio vacía el carrito en su primera ejecución: se deja correr antes de añadir productos.
    TestBed.tick();
    fixture = TestBed.createComponent(Carrito);
  });

  afterEach(() => http.verify());

  it('el botón Pagar está deshabilitado con el carrito vacío', () => {
    fixture.detectChanges();

    expect(boton().disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('.vacio')).not.toBeNull();
  });

  it('el botón Pagar se bloquea mientras se procesa', () => {
    carrito.agregar(producto());
    fixture.detectChanges();
    expect(boton().disabled).toBe(false);

    boton().click();
    fixture.detectChanges();

    expect(boton().disabled).toBe(true);
    expect(boton().textContent?.trim()).toBe('Procesando…');
    http.expectOne(postCompra());
  });

  it('un segundo clic no genera una segunda petición', () => {
    carrito.agregar(producto());
    fixture.detectChanges();

    boton().click();
    boton().click();

    // expectOne falla si hubiera dos peticiones POST.
    http.expectOne(postCompra());
  });

  it('el + se deshabilita al llegar al límite de stock', () => {
    carrito.agregar(producto({ stock: 2 }), 2);
    fixture.detectChanges();

    const mas = fixture.nativeElement.querySelector('button.mas') as HTMLButtonElement;
    expect(mas.disabled).toBe(true);
  });

  it('un 422 se muestra con el mensaje del backend', () => {
    carrito.agregar(producto());
    fixture.detectChanges();

    boton().click();
    http.expectOne(postCompra()).flush({ mensaje: MENSAJE_STOCK }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    const error = toast.toasts().find((t) => t.tipo === 'error');
    expect(error?.texto).toContain(MENSAJE_STOCK);
    expect(error?.texto).toContain('Teclado de ensayo');
    expect(error?.texto).not.toContain('Los pagos anteriores');
    expect(carrito.items().map((i) => i.producto.id)).toEqual(['p1']);
    expect(boton().disabled).toBe(false);
  });

  it('un 422 después de pagos exitosos avisa que los anteriores quedaron completados', () => {
    carrito.agregar(producto({ id: 'a', nombre: 'Mouse' }));
    carrito.agregar(producto({ id: 'b', nombre: 'Teclado de ensayo' }));
    fixture.detectChanges();

    boton().click();
    http.expectOne(postCompra()).flush(compra('cA', 'a'));
    http.expectOne({ method: 'PUT', url: `${API_URL}/compras/cA/confirmar` }).flush(compra('cA', 'a'));
    http.expectOne(postCompra()).flush({ mensaje: MENSAJE_STOCK }, { status: 422, statusText: 'Unprocessable Content' });

    const error = toast.toasts().find((t) => t.tipo === 'error');
    expect(error?.texto).toContain('Los pagos anteriores quedaron completados');
    expect(carrito.items().map((i) => i.producto.id)).toEqual(['b']);
  });

  it('un pago completo navega a /mis-compras', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    carrito.agregar(producto());
    fixture.detectChanges();

    boton().click();
    http.expectOne(postCompra()).flush(compra('cA', 'p1'));
    http.expectOne({ method: 'PUT', url: `${API_URL}/compras/cA/confirmar` }).flush(compra('cA', 'p1'));

    expect(navegar).toHaveBeenCalledWith('/mis-compras');
    expect(toast.toasts().some((t) => t.tipo === 'ok' && t.texto.includes('1 producto(s) pagado(s)'))).toBe(true);
    expect(carrito.items().length).toBe(0);
  });
});
