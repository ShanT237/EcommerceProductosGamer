import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Compra, Pedido, Producto, Sesion } from '../modelos/modelos';
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
    pedidoId: 'ped1',
  };
}

function pedido(compras: Compra[]): Pedido {
  return { pedidoId: 'ped1', compras, total: compras.reduce((suma, c) => suma + c.subtotal, 0) };
}

describe('CarritoService', () => {
  let carrito: CarritoService;
  let http: HttpTestingController;
  let sesion: ReturnType<typeof signal<Sesion | null>>;

  const postPedido = () => ({ method: 'POST', url: `${API_URL}/pedidos` });
  const putPedido = (id: string, accion: string) => ({ method: 'PUT', url: `${API_URL}/pedidos/${id}/${accion}` });
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
    it('hace un solo POST con todos los ítems', () => {
      carrito.agregar(producto({ id: 'a' }), 2);
      carrito.agregar(producto({ id: 'b' }), 1);

      carrito.pagar().subscribe();

      // expectOne falla si hubiera más de un POST (uno por ítem).
      const post = http.expectOne(postPedido());
      expect(post.request.body).toEqual({
        items: [
          { productoId: 'a', cantidad: 2 },
          { productoId: 'b', cantidad: 1 },
        ],
      });
      post.flush(pedido([compra('cA', 'a', 2), compra('cB', 'b')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush(pedido([compra('cA', 'a', 2), compra('cB', 'b')]));
    });

    it('confirma el pedido después de crearlo y vacía el carrito', () => {
      carrito.agregar(producto({ id: 'a' }), 2);
      carrito.agregar(producto({ id: 'b' }), 1);

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush(pedido([compra('cA', 'a', 2), compra('cB', 'b')]));
      // Confirmar no se pide hasta que el pedido existe, y es un único PUT para todo el pedido.
      expect(carrito.pagando()).toBe(true);
      const confirmado = pedido([{ ...compra('cA', 'a', 2), estado: 'COMPLETADA' }, { ...compra('cB', 'b'), estado: 'COMPLETADA' }]);
      http.expectOne(putPedido('ped1', 'confirmar')).flush(confirmado);

      expect(resultados.length).toBe(1);
      expect(resultados[0].error).toBeNull();
      expect(resultados[0].compras.length).toBe(2);
      expect(carrito.items().length).toBe(0);
      expect(carrito.pagando()).toBe(false);
    });

    it('si falla la creación el carrito queda intacto y se informa el mensaje del backend', () => {
      carrito.agregar(producto({ id: 'a' }));
      carrito.agregar(producto({ id: 'b' }), 2);

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush({ mensaje: MENSAJE_STOCK }, sinStock);

      // Nada que confirmar ni cancelar: el pedido nunca se creó.
      http.expectNone(putPedido('ped1', 'confirmar'));
      http.expectNone(putPedido('ped1', 'cancelar'));
      expect(resultados[0].compras.length).toBe(0);
      expect(resultados[0].error).toBe(MENSAJE_STOCK);
      expect(carrito.items().map((i) => i.producto.id)).toEqual(['a', 'b']);
      expect(carrito.items()[1].cantidad).toBe(2);
      expect(carrito.pagando()).toBe(false);
    });

    it('si falla la confirmación cancela el pedido y conserva el carrito', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush(pedido([compra('cA', 'a')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putPedido('ped1', 'cancelar')).flush(pedido([{ ...compra('cA', 'a'), estado: 'CANCELADA' }]));

      expect(resultados[0].compras.length).toBe(0);
      expect(resultados[0].error).toBe(MENSAJE_STOCK);
      expect(carrito.items().map((i) => i.producto.id)).toEqual(['a']);
      expect(carrito.pagando()).toBe(false);
    });

    it('reporta el error de confirmar aunque la cancelación también falle', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush(pedido([compra('cA', 'a')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'otro error' }, { status: 500, statusText: 'Server Error' });

      expect(resultados[0].error).toBe(MENSAJE_STOCK);
      expect(carrito.items().length).toBe(1);
      expect(carrito.pagando()).toBe(false);
    });

    it('un segundo clic mientras ya está pagando no genera otro POST', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));
      carrito.pagar().subscribe((r) => resultados.push(r));

      // expectOne falla si hubiera dos peticiones POST.
      const post = http.expectOne(postPedido());
      expect(carrito.pagando()).toBe(true);

      post.flush(pedido([compra('cA', 'a')]));
      expect(carrito.pagando()).toBe(true);
      http.expectOne(putPedido('ped1', 'confirmar')).flush(pedido([compra('cA', 'a')]));

      expect(carrito.pagando()).toBe(false);
      expect(resultados.length).toBe(1);
    });

    it('no hace nada si el carrito está vacío', () => {
      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectNone(postPedido());
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
