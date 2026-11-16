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
    localStorage.clear()
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

  afterEach(() => {
    // El componente pide el catálogo al abrirse; estos tests no lo usan.
    http.match((r) => r.method === 'GET' && r.url.startsWith(`${API_URL}/productos`));
    http.verify();
    localStorage.clear();
  });

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

    it('pagarConCuponEnviaCodigoCuponUnaSolaVezEnElPost', () => {
      carrito.agregar(producto({ id: 'a' }), 2);
      carrito.agregar(producto({ id: 'b' }), 1);

      carrito.pagar('VERANO26').subscribe();

      // Un único POST con el cupón (consume un uso por pedido, no uno por ítem).
      const post = http.expectOne(postPedido());
      expect(post.request.body).toEqual({
        items: [
          { productoId: 'a', cantidad: 2 },
          { productoId: 'b', cantidad: 1 },
        ],
        codigoCupon: 'VERANO26',
      });
      post.flush(pedido([compra('cA', 'a', 2), compra('cB', 'b')]));
      // La confirmación no vuelve a llevar el cupón.
      const confirmar = http.expectOne(putPedido('ped1', 'confirmar'));
      expect(confirmar.request.body).toEqual({});
      confirmar.flush(pedido([compra('cA', 'a', 2), compra('cB', 'b')]));
    });

    it('pagarSinCuponNoEnviaElCampo', () => {
      carrito.agregar(producto({ id: 'a' }));

      carrito.pagar().subscribe();

      const post = http.expectOne(postPedido());
      expect('codigoCupon' in post.request.body).toBe(false);
      post.flush(pedido([compra('cA', 'a')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush(pedido([compra('cA', 'a')]));
    });

    it('si falla el pago con cupón el carrito queda intacto y se informa el mensaje del backend', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar('VENCIDO10').subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush({ mensaje: 'El cupón está vencido' }, { status: 422, statusText: 'Unprocessable Content' });

      expect(resultados[0].error).toBe('El cupón está vencido');
      expect(carrito.items().length).toBe(1);
      expect(carrito.pagando()).toBe(false);
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

    it('si la cancelación también falla informa el pedido sin cancelar y no lo oculta', () => {
      carrito.agregar(producto({ id: 'a' }));

      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));

      http.expectOne(postPedido()).flush(pedido([compra('cA', 'a')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'otro error' }, { status: 500, statusText: 'Server Error' });

      expect(resultados[0].error).toBe(MENSAJE_STOCK);
      expect(resultados[0].pedidoSinCancelar).toBe('ped1');
      expect(carrito.pedidoSinCancelar()).toBe('ped1');
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

  describe('pedido sin cancelar (falló confirmar y cancelar)', () => {
    const fallo500 = { status: 500, statusText: 'Server Error' };
    const getCompras = () => ({ method: 'GET', url: `${API_URL}/compras` });

    function pagarConCancelacionFallida(): ResultadoPago[] {
      carrito.agregar(producto({ id: 'a' }));
      const resultados: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => resultados.push(r));
      http.expectOne(postPedido()).flush(pedido([compra('cA', 'a')]));
      http.expectOne(putPedido('ped1', 'confirmar')).flush({ mensaje: MENSAJE_STOCK }, sinStock);
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'otro error' }, fallo500);
      return resultados;
    }

    it('bloquea un nuevo pago mientras el pedido siga sin cancelar (no duplica la reserva)', () => {
      pagarConCancelacionFallida();

      const otros: ResultadoPago[] = [];
      carrito.pagar().subscribe((r) => otros.push(r));

      http.expectNone(postPedido());
      expect(otros[0].pedidoSinCancelar).toBe('ped1');
      expect(otros[0].error).toContain('no se pudo cancelar');
    });

    it('reintentar y lograr cancelar libera el bloqueo y conserva el carrito', () => {
      pagarConCancelacionFallida();

      const desenlaces: string[] = [];
      carrito.reintentarCancelacion().subscribe((d) => desenlaces.push(d));
      http.expectOne(putPedido('ped1', 'cancelar')).flush(pedido([{ ...compra('cA', 'a'), estado: 'CANCELADA' }]));

      expect(desenlaces).toEqual(['CANCELADO']);
      expect(carrito.pedidoSinCancelar()).toBeNull();
      expect(carrito.items().length).toBe(1);
      expect(carrito.pagando()).toBe(false);
    });

    it('si cancelar vuelve a fallar pero el pedido ya estaba cancelado, reconcilia como cancelado', () => {
      pagarConCancelacionFallida();

      const desenlaces: string[] = [];
      carrito.reintentarCancelacion().subscribe((d) => desenlaces.push(d));
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'x' }, fallo500);
      http.expectOne(getCompras()).flush([{ ...compra('cA', 'a'), estado: 'CANCELADA' }]);

      expect(desenlaces).toEqual(['CANCELADO']);
      expect(carrito.pedidoSinCancelar()).toBeNull();
    });

    it('si el pedido en realidad se confirmó, reconcilia como pagado y vacía el carrito', () => {
      pagarConCancelacionFallida();

      const desenlaces: string[] = [];
      carrito.reintentarCancelacion().subscribe((d) => desenlaces.push(d));
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'x' }, fallo500);
      http.expectOne(getCompras()).flush([{ ...compra('cA', 'a'), estado: 'COMPLETADA' }]);

      expect(desenlaces).toEqual(['PAGADO']);
      expect(carrito.pedidoSinCancelar()).toBeNull();
      expect(carrito.items().length).toBe(0);
    });

    it('si sigue pendiente conserva el bloqueo para poder reintentar otra vez', () => {
      pagarConCancelacionFallida();

      const desenlaces: string[] = [];
      carrito.reintentarCancelacion().subscribe((d) => desenlaces.push(d));
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'x' }, fallo500);
      http.expectOne(getCompras()).flush([compra('cA', 'a')]);

      expect(desenlaces).toEqual(['PENDIENTE']);
      expect(carrito.pedidoSinCancelar()).toBe('ped1');
      expect(carrito.pagando()).toBe(false);
    });

    it('si tampoco se puede consultar el estado queda pendiente (no asume nada)', () => {
      pagarConCancelacionFallida();

      const desenlaces: string[] = [];
      carrito.reintentarCancelacion().subscribe((d) => desenlaces.push(d));
      http.expectOne(putPedido('ped1', 'cancelar')).flush({ mensaje: 'x' }, fallo500);
      http.expectOne(getCompras()).flush({ mensaje: 'x' }, fallo500);

      expect(desenlaces).toEqual(['PENDIENTE']);
      expect(carrito.pedidoSinCancelar()).toBe('ped1');
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

    it('persiste el carrito y lo recupera para el mismo usuario', () => {
      carrito.agregar(producto({ id: 'a' }), 2);

      TestBed.resetTestingModule(); // simula recargar la página
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: AuthService, useValue: { sesion } }, // misma sesión 'u1'
        ],
      });
      http = TestBed.inject(HttpTestingController);
      const otro = TestBed.inject(CarritoService);
      TestBed.tick();

      expect(otro.items().map((i) => [i.producto.id, i.cantidad])).toEqual([['a', 2]]);
    });

    it('el carrito de un usuario no se ve desde otro', () => {
      carrito.agregar(producto({ id: 'a' }));
      sesion.set({ id: 'u2', correo: 'ana@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
      TestBed.tick();
      expect(carrito.items().length).toBe(0);
      sesion.set({ id: 'u1', correo: 'jose@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
      TestBed.tick();
      expect(carrito.items().length).toBe(1);
    });

    it('un JSON corrupto en storage se ignora', () => {
      localStorage.setItem('gamerzone.carrito.u1', '{no es json');
      TestBed.tick(); // primera ejecución del efecto: lee y descarta lo corrupto
      expect(carrito.items()).toEqual([]);
    });

    it('sincronizar ajusta la cantidad al stock actual y quita agotados', () => {
      carrito.agregar(producto({ id: 'a', stock: 5 }), 5);
      carrito.agregar(producto({ id: 'b', stock: 5 }), 1);
      carrito.sincronizar([producto({ id: 'a', stock: 2, precio: 90 }), producto({ id: 'b', stock: 0 })]);
      expect(carrito.items().map((i) => [i.producto.id, i.cantidad])).toEqual([['a', 2]]);
    });

    it('un pago exitoso borra el carrito guardado', () => {
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
      expect(localStorage.getItem('gamerzone.carrito.u1')).toBeNull();
    });
  });
});
