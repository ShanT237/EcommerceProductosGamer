import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Compra, Producto, Sesion } from '../modelos/modelos';
import { AuthService } from './auth.service';
import { CarritoService, ResultadoPago } from './carrito.service';

const MENSAJE_STOCK = 'No hay suficiente stock disponible';

function producto(overrides: Partial<Producto> = {}): Producto {
  return {
    id: 'p1',
    vendedorId: 'v1',
    nombre: 'Teclado',
    descripcion: 'Teclado mecánico',
    precio: 100,
    stock: 5,
    gama: 'MEDIA',
    exclusividad: 'NORMAL',
    categoria: null,
    ...overrides,
  };
}

function compra(id: string, productoId: string, cantidad = 1): Compra {
  return {
    id,
    usuarioId: 'u1',
    productoId,
    cantidad,
    precioUnitario: 100,
    subtotal: 100 * cantidad,
    fecha: '2026-11-13T10:00:00',
    estado: 'PENDIENTE',
  };
}

describe('CarritoService', () => {
  let carrito: CarritoService;
  let http: HttpTestingController;
  let sesion: ReturnType<typeof signal<Sesion | null>>;

  const postCompra = () => ({ method: 'POST', url: `${API_URL}/compras` });
  const putAccion = (id: string, accion: string) => ({ method: 'PUT', url: `${API_URL}/compras/${id}/${accion}` });
  const sinStock = { status: 422, statusText: 'Unprocessable Content' };

  beforeEach(() => {
    sesion = signal<Sesion | null>({ id: 'u1', correo: 'jose@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    carrito = TestBed.inject(CarritoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('estado del carrito', () => {
    it('agregar fusiona el mismo producto y respeta el stock', () => {
      const p = producto({ stock: 3 });
      carrito.agregar(p, 2);
      carrito.agregar(p, 2);

      expect(carrito.items().length).toBe(1);
      expect(carrito.items()[0].cantidad).toBe(3);
    });

    it('agregar limita un producto exclusivo a una unidad', () => {
      carrito.agregar(producto({ exclusividad: 'EXCLUSIVO', stock: 5 }), 4);

      expect(carrito.items()[0].cantidad).toBe(1);
    });

    it('agregar ignora un producto agotado', () => {
      carrito.agregar(producto({ stock: 0 }));

      expect(carrito.items().length).toBe(0);
    });

    it('limiteDe devuelve 1 para un exclusivo y 0 si está agotado', () => {
      expect(carrito.limiteDe(producto({ exclusividad: 'EXCLUSIVO', stock: 5 }))).toBe(1);
      expect(carrito.limiteDe(producto({ exclusividad: 'EXCLUSIVO', stock: 0 }))).toBe(0);
      expect(carrito.limiteDe(producto({ stock: 7 }))).toBe(7);
    });

    it('subtotal y unidades suman todos los ítems', () => {
      carrito.agregar(producto({ id: 'a', precio: 100 }), 2);
      carrito.agregar(producto({ id: 'b', precio: 50 }), 1);

      expect(carrito.subtotal()).toBe(250);
      expect(carrito.unidades()).toBe(3);
    });

    it('cambiarCantidad respeta el límite y quitar elimina el ítem', () => {
      const a = producto({ id: 'a', stock: 3 });
      const b = producto({ id: 'b', stock: 3 });
      carrito.agregar(a);
      carrito.agregar(b);

      carrito.cambiarCantidad('a', 10);
      expect(carrito.items().find((i) => i.producto.id === 'a')?.cantidad).toBe(3);

      carrito.cambiarCantidad('a', 2);
      expect(carrito.items().find((i) => i.producto.id === 'a')?.cantidad).toBe(2);

      carrito.cambiarCantidad('b', 0);
      expect(carrito.items().map((i) => i.producto.id)).toEqual(['a']);

      carrito.quitar('a');
      expect(carrito.items().length).toBe(0);
    });

    it('vaciar deja el carrito sin ítems', () => {
      carrito.agregar(producto({ id: 'a' }));
      carrito.agregar(producto({ id: 'b' }));

      carrito.vaciar();

      expect(carrito.items().length).toBe(0);
      expect(carrito.unidades()).toBe(0);
    });
  });

  describe('pagar', () => {
    it('crea y confirma cada ítem en orden', () => {
      const a = producto({ id: 'a' });
      const b = producto({ id: 'b' });
      carrito.agregar(a, 2);
      carrito.agregar(b, 1);

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      const postA = http.expectOne(postCompra());
      expect(postA.request.body).toEqual({ productoId: 'a', cantidad: 2 });
      postA.flush(compra('cA', 'a', 2));

      // Secuencial: B no se pide hasta que A queda confirmada.
      http.expectNone(postCompra());
      http.expectOne(putAccion('cA', 'confirmar')).flush(compra('cA', 'a', 2));

      const postB = http.expectOne(postCompra());
      expect(postB.request.body).toEqual({ productoId: 'b', cantidad: 1 });
      postB.flush(compra('cB', 'b'));
      http.expectOne(putAccion('cB', 'confirmar')).flush(compra('cB', 'b'));

      expect(resultados.length).toBe(1);
      expect(resultados[0].completadas.length).toBe(2);
      expect(resultados[0].fallo).toBeNull();
      expect(carrito.items().length).toBe(0);
      expect(carrito.pagando()).toBe(false);
    });

    it('se detiene en el primer fallo y conserva los ítems restantes', () => {
      const a = producto({ id: 'a' });
      const b = producto({ id: 'b' });
      const c = producto({ id: 'c' });
      carrito.agregar(a);
      carrito.agregar(b);
      carrito.agregar(c);

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postCompra()).flush(compra('cA', 'a'));
      http.expectOne(putAccion('cA', 'confirmar')).flush(compra('cA', 'a'));
      http.expectOne(postCompra()).flush({ mensaje: MENSAJE_STOCK }, sinStock);

      // No se hizo ninguna petición por C (http.verify() también lo comprueba).
      http.expectNone(postCompra());

      const r = resultados[0];
      expect(r.completadas.length).toBe(1);
      expect(r.fallo?.producto.id).toBe('b');
      expect(r.fallo?.mensaje).toBe(MENSAJE_STOCK);
      expect(carrito.items().map((i) => i.producto.id)).toEqual(['b', 'c']);
      expect(carrito.pagando()).toBe(false);
    });

    it('cancela la reserva si falla la confirmación', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postCompra()).flush(compra('cA', 'a'));
      http.expectOne(putAccion('cA', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putAccion('cA', 'cancelar')).flush(compra('cA', 'a'));

      const r = resultados[0];
      expect(r.completadas.length).toBe(0);
      expect(r.fallo?.mensaje).toBe(MENSAJE_STOCK);
      expect(carrito.items().map((i) => i.producto.id)).toEqual(['a']);
    });

    it('reporta el error de confirmar aunque la cancelación también falle', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postCompra()).flush(compra('cA', 'a'));
      http.expectOne(putAccion('cA', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putAccion('cA', 'cancelar')).flush({ mensaje: 'otro error' }, { status: 500, statusText: 'Server Error' });

      expect(resultados[0].fallo?.mensaje).toBe(MENSAJE_STOCK);
      expect(carrito.pagando()).toBe(false);
    });

    it('ignora un segundo clic mientras ya está pagando', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));
      carrito.pagar().subscribe((r) => resultados.push(r));

      // expectOne falla si hubiera dos peticiones POST.
      const postA = http.expectOne(postCompra());
      expect(carrito.pagando()).toBe(true);

      postA.flush(compra('cA', 'a'));
      expect(carrito.pagando()).toBe(true);
      http.expectOne(putAccion('cA', 'confirmar')).flush(compra('cA', 'a'));

      expect(carrito.pagando()).toBe(false);
      expect(resultados.length).toBe(1);
    });

    it('no hace nada si el carrito está vacío', () => {
      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectNone(postCompra());
      expect(resultados.length).toBe(0);
      expect(carrito.pagando()).toBe(false);
    });
  });

  describe('sesión', () => {
    it('el carrito se vacía cuando cambia la sesión', () => {
      TestBed.tick(); // primera ejecución del efecto
      carrito.agregar(producto());
      TestBed.tick();
      expect(carrito.items().length).toBe(1); // la misma sesión no lo vacía

      sesion.set(null);
      TestBed.tick();

      expect(carrito.items().length).toBe(0);
    });
  });
});
