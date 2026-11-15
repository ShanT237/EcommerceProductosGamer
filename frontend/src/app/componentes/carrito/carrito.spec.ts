import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Compra, Cupon, Pedido, Producto, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { CarritoService } from '../../servicios/carrito.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { precio } from '../../servicios/formato';
import { ToastService } from '../../servicios/toast.service';
import { Carrito } from './carrito';

/** Deja correr las promesas pendientes (la confirmación se resuelve de forma asíncrona). */
const esperar = () => new Promise<void>((r) => setTimeout(r));

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
    pedidoId: 'ped1',
  };
}

function pedido(...compras: Compra[]): Pedido {
  return { pedidoId: 'ped1', compras, total: compras.reduce((suma, c) => suma + c.subtotal, 0) };
}

describe('Carrito (pantalla)', () => {
  let fixture: ComponentFixture<Carrito>;
  let http: HttpTestingController;
  let carrito: CarritoService;
  let toast: ToastService;

  const postPedido = () => ({ method: 'POST', url: `${API_URL}/pedidos` });
  const putConfirmar = () => ({ method: 'PUT', url: `${API_URL}/pedidos/ped1/confirmar` });
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
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
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

  it('el botón Pagar se bloquea mientras se procesa', async () => {
    carrito.agregar(producto());
    fixture.detectChanges();
    expect(boton().disabled).toBe(false);

    boton().click();
    await esperar();
    fixture.detectChanges();

    expect(boton().disabled).toBe(true);
    expect(boton().textContent?.trim()).toBe('Procesando…');
    http.expectOne(postPedido());
  });

  it('un segundo clic no genera una segunda petición', async () => {
    carrito.agregar(producto());
    fixture.detectChanges();

    boton().click();
    await esperar();
    boton().click();
    await esperar();

    // expectOne falla si hubiera dos peticiones POST.
    http.expectOne(postPedido());
  });

  it('el + se deshabilita al llegar al límite de stock', () => {
    carrito.agregar(producto({ stock: 2 }), 2);
    fixture.detectChanges();

    const mas = fixture.nativeElement.querySelector('button.mas') as HTMLButtonElement;
    expect(mas.disabled).toBe(true);
  });

  it('un 422 se muestra con el mensaje del backend y el carrito queda intacto', async () => {
    carrito.agregar(producto({ id: 'a', nombre: 'Mouse' }));
    carrito.agregar(producto({ id: 'b', nombre: 'Teclado de ensayo' }));
    fixture.detectChanges();

    boton().click();
    await esperar();
    http.expectOne(postPedido()).flush({ mensaje: MENSAJE_STOCK }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    const error = toast.toasts().find((t) => t.tipo === 'error');
    expect(error?.texto).toContain(MENSAJE_STOCK);
    expect(error?.texto).not.toContain('Los pagos anteriores');
    expect(carrito.items().map((i) => i.producto.id)).toEqual(['a', 'b']);
    expect(boton().disabled).toBe(false);
  });

  it('un pago completo hace un solo POST con todos los ítems y navega a /mis-compras', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    carrito.agregar(producto({ id: 'a' }));
    carrito.agregar(producto({ id: 'b' }), 2);
    fixture.detectChanges();

    boton().click();
    await esperar();
    const post = http.expectOne(postPedido());
    expect(post.request.body).toEqual({
      items: [
        { productoId: 'a', cantidad: 1 },
        { productoId: 'b', cantidad: 2 },
      ],
    });
    post.flush(pedido(compra('cA', 'a'), compra('cB', 'b')));
    http.expectOne(putConfirmar()).flush(pedido(compra('cA', 'a'), compra('cB', 'b')));

    expect(navegar).toHaveBeenCalledWith('/mis-compras');
    expect(toast.toasts().some((t) => t.tipo === 'ok' && t.texto.includes('2 producto(s) pagado(s)'))).toBe(true);
    expect(carrito.items().length).toBe(0);
  });

  it('si la persona cancela la confirmación no se hace ninguna petición', async () => {
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(false);
    carrito.agregar(producto());
    fixture.detectChanges();

    boton().click();
    await esperar();

    http.expectNone(postPedido());
    expect(carrito.items().length).toBe(1);
  });

  // ---- cupón ----
  const getCupon = (codigo = 'VERANO26') => ({ method: 'GET', url: `${API_URL}/cupones/${codigo}` });
  const campoCupon = () => fixture.nativeElement.querySelector('#cupon-carrito') as HTMLInputElement;
  const textoDe = (selector: string) => (fixture.nativeElement.querySelector(selector) as HTMLElement | null)?.textContent ?? '';

  function cupon(extra: Partial<Cupon> = {}): Cupon {
    return { id: 'c1', codigo: 'VERANO26', porcentaje: 10, vence: '2026-12-31', usosMaximos: 100, usos: 0, vendedorId: null, ...extra };
  }

  /** Escribe el código y pulsa "Aplicar". */
  function escribirYAplicar(codigo = 'VERANO26') {
    campoCupon().value = codigo;
    campoCupon().dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('button.aplicar-cupon') as HTMLButtonElement).click();
  }

  it('aplicarMuestraElDescuentoEstimado', () => {
    carrito.agregar(producto({ id: 'a', precio: 100000 }), 2); // subtotal 200.000
    fixture.detectChanges();

    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon({ porcentaje: 10 }));
    fixture.detectChanges();

    expect(textoDe('.descuento strong')).toContain(precio(20000));
    expect(textoDe('.fila.total strong')).toContain(precio(180000));
    expect(textoDe('.cupon-aplicado')).toContain('VERANO26');
  });

  it('unCuponInvalidoMuestraElMensajeDelBackend', () => {
    carrito.agregar(producto({ id: 'a' }));
    fixture.detectChanges();

    escribirYAplicar('VIEJO10');
    http.expectOne(getCupon('VIEJO10')).flush({ mensaje: 'El cupón está vencido' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    expect(textoDe('.error-cupon')).toContain('El cupón está vencido');
    expect(fixture.nativeElement.querySelector('.descuento')).toBeNull();
    expect(carrito.items().length).toBe(1);
  });

  it('unCuponInexistenteMuestraElMensajeDelBackend', () => {
    carrito.agregar(producto({ id: 'a' }));
    fixture.detectChanges();

    escribirYAplicar('NOEXISTE');
    http.expectOne(getCupon('NOEXISTE')).flush({ mensaje: 'El cupón no existe' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(textoDe('.error-cupon')).toContain('El cupón no existe');
  });

  it('sinCodigoNoConsultaNada', () => {
    carrito.agregar(producto({ id: 'a' }));
    fixture.detectChanges();

    escribirYAplicar('   ');
    fixture.detectChanges();

    http.expectNone({ method: 'GET', url: `${API_URL}/cupones/` });
    expect(textoDe('.error-cupon')).toContain('Escribe el código');
  });

  it('unCuponDeOtroVendedorSinProductosAplicablesSeRechazaEnLaVistaPrevia', () => {
    carrito.agregar(producto({ id: 'a', vendedorId: 'v1' }));
    fixture.detectChanges();

    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon({ vendedorId: 'v2' }));
    fixture.detectChanges();

    expect(textoDe('.error-cupon')).toContain('no aplica a ningún producto');
    expect(fixture.nativeElement.querySelector('.descuento')).toBeNull();
  });

  it('conUnCuponConDuenoSoloDescuentaLosProductosDeEseVendedor', () => {
    carrito.agregar(producto({ id: 'a', vendedorId: 'v1', precio: 100000 }));
    carrito.agregar(producto({ id: 'b', vendedorId: 'v2', precio: 100000 }));
    fixture.detectChanges();

    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon({ vendedorId: 'v2', porcentaje: 10 }));
    fixture.detectChanges();

    expect(textoDe('.descuento strong')).toContain(precio(10000)); // solo el producto de v2
    expect(textoDe('.fila.total strong')).toContain(precio(190000));
  });

  it('pagarConCuponEnviaCodigoCuponEnUnSoloPostYLimpiaElCupon', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    carrito.agregar(producto({ id: 'a' }));
    carrito.agregar(producto({ id: 'b' }));
    fixture.detectChanges();
    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon());
    fixture.detectChanges();

    boton().click();
    await esperar();

    const post = http.expectOne(postPedido());
    expect(post.request.body).toEqual({
      items: [
        { productoId: 'a', cantidad: 1 },
        { productoId: 'b', cantidad: 1 },
      ],
      codigoCupon: 'VERANO26',
    });
    post.flush(pedido(compra('cA', 'a'), compra('cB', 'b')));
    http.expectOne(putConfirmar()).flush(pedido(compra('cA', 'a'), compra('cB', 'b')));
    fixture.detectChanges();

    expect(carrito.items().length).toBe(0);
    expect(fixture.nativeElement.querySelector('.cupon-aplicado')).toBeNull();
  });

  it('siElPagoConCuponFallaMuestraElMensajeYConservaCuponYCarrito', async () => {
    carrito.agregar(producto({ id: 'a' }));
    fixture.detectChanges();
    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon());
    fixture.detectChanges();

    boton().click();
    await esperar();
    http.expectOne(postPedido()).flush({ mensaje: 'Ya usaste este cupón' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    const error = toast.toasts().find((t) => t.tipo === 'error');
    expect(error?.texto).toContain('Ya usaste este cupón');
    expect(carrito.items().length).toBe(1);
    expect(textoDe('.cupon-aplicado')).toContain('VERANO26');
  });

  it('quitarElCuponQuitaElDescuentoYElPagoNoLoEnvia', async () => {
    carrito.agregar(producto({ id: 'a' }));
    fixture.detectChanges();
    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon());
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('button.quitar-cupon') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.descuento')).toBeNull();

    boton().click();
    await esperar();
    const post = http.expectOne(postPedido());
    expect('codigoCupon' in post.request.body).toBe(false);
  });

  it('siSeQuitanLosProductosDelVendedorDuenoElCuponNoSeEnvia', async () => {
    carrito.agregar(producto({ id: 'a', vendedorId: 'v1' }));
    carrito.agregar(producto({ id: 'b', vendedorId: 'v2' }));
    fixture.detectChanges();
    escribirYAplicar();
    http.expectOne(getCupon()).flush(cupon({ vendedorId: 'v2' }));
    fixture.detectChanges();

    carrito.quitar('b');
    fixture.detectChanges();
    expect(textoDe('.error-cupon')).toContain('ya no aplica');

    boton().click();
    await esperar();
    const post = http.expectOne(postPedido());
    expect('codigoCupon' in post.request.body).toBe(false);
  });
});