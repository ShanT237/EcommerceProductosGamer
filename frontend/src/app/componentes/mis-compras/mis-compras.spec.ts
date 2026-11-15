import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Compra, MovimientoPuntos } from '../../modelos/modelos';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { MisCompras } from './mis-compras';

/** Deja correr las promesas pendientes (la confirmación se resuelve de forma asíncrona). */
const esperar = () => new Promise<void>((r) => setTimeout(r));

function compra(id: string, pedidoId: string | null, overrides: Partial<Compra> = {}): Compra {
  return {
    id,
    usuarioId: 'u1',
    productoId: 'p-' + id,
    cantidad: 1,
    precioUnitario: 100,
    subtotal: 100,
    fecha: '2026-11-14T10:00:00',
    estado: 'PENDIENTE',
    pedidoId,
    ...overrides,
  };
}

describe('MisCompras (pedidos)', () => {
  let fixture: ComponentFixture<MisCompras>;
  let http: HttpTestingController;
  let pedir: ReturnType<typeof vi.spyOn>;

  /** Crea la pantalla y responde las tres cargas iniciales (catálogo, compras y puntos). */
  function abrir(lista: Compra[]) {
    fixture = TestBed.createComponent(MisCompras);
    fixture.detectChanges();
    http.expectOne((r) => r.method === 'GET' && r.url.startsWith(`${API_URL}/productos`)).flush({
      contenido: lista.map((c, i) => ({
        id: c.productoId, vendedorId: 'v1', nombre: `Producto ${i + 1}`,
        descripcion: '', precio: 100, stock: 5, gama: 'MEDIA', exclusividad: 'NORMAL',
        categoria: null,
      })),
      pagina: 0, tamano: 100, totalElementos: lista.length, totalPaginas: 1,
    });
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush(lista);
    http.expectOne({ method: 'GET', url: `${API_URL}/puntos/historial` }).flush([]);
    fixture.detectChanges();
  }

  /** Tras cada acción la pantalla recarga las compras y el historial de puntos. */
  function responderRecarga() {
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush([]);
    http.expectOne({ method: 'GET', url: `${API_URL}/puntos/historial` }).flush([]);
  }

  const boton = (texto: string) =>
    [...(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)].find((b) =>
      b.textContent?.trim() === texto,
    );

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisCompras],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    pedir = vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('confirmar una línea de un pedido usa PUT /pedidos/{id}/confirmar', async () => {
    abrir([compra('c1', 'ped1'), compra('c2', 'ped1')]);
    expect(fixture.nativeElement.querySelectorAll('article.compra')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('2 producto(s)');
    expect(fixture.nativeElement.textContent).toContain('Producto 1 + Producto 2');

    boton('Confirmar pago')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/confirmar` }).flush({ pedidoId: 'ped1', compras: [], total: 200 });
    // El mensaje habla del pedido completo (2 productos, 200), no de la línea suelta.
    expect(pedir.mock.calls[0][0].mensaje).toContain('pedido completo (2 producto(s))');
    responderRecarga();
  });

  it('cancelar una línea de un pedido usa PUT /pedidos/{id}/cancelar', async () => {
    abrir([compra('c1', 'ped1'), compra('c2', 'ped1')]);

    boton('Cancelar')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/cancelar` }).flush({ pedidoId: 'ped1', compras: [], total: 200 });
    responderRecarga();
  });

  it('reembolsa todas las líneas del pedido con una sola acción', async () => {
    abrir([compra('c1', 'ped1', { estado: 'COMPLETADA' }), compra('c2', 'ped1', { estado: 'COMPLETADA' })]);

    boton('Reembolso')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/reembolso` }).flush({
      pedidoId: 'ped1', compras: [], total: 200,
    });
    responderRecarga();
  });

  it('una compra suelta (sin pedido) sigue usando los endpoints de compra', async () => {
    abrir([compra('c9', null)]);

    boton('Confirmar pago')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/compras/c9/confirmar` }).flush(compra('c9', null, { estado: 'COMPLETADA' }));
    responderRecarga();
  });

  it('muestraElNombreOriginalDelComboComoUnaSolaOperacion', () => {
    abrir([
      compra('c1', 'ped1', { comboId: 'combo1', comboNombre: 'Combo Gamer' }),
      compra('c2', 'ped1', { comboId: 'combo1', comboNombre: 'Combo Gamer' }),
    ]);

    expect(fixture.nativeElement.querySelectorAll('article.compra')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Combo Gamer');
  });

  it('ninguna compra pendiente ofrece aplicar un cupón: el cupón se usa al crear el pedido', () => {
    abrir([compra('c1', 'ped1'), compra('c9', null)]);

    expect(boton('Cupón')).toBeUndefined();
  });
});

const compraCompletada: Compra = {
  id: 'c1',
  pedidoId: null,
  usuarioId: 'u1',
  productoId: 'p1',
  cantidad: 1,
  precioUnitario: 100,
  subtotal: 100,
  fecha: '2026-11-14T10:00:00',
  estado: 'COMPLETADA',
};
const ganado = (n: number): MovimientoPuntos => ({ id: 'm' + n, cantidad: n, tipo: 'GANADO', fecha: '2026-11-14T10:00:00' });

describe('MisCompras (puntos)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisCompras],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('tras publicar una reseña vuelve a pedir el historial y el saldo se actualiza sin recargar', () => {
    const fixture = TestBed.createComponent(MisCompras);
    fixture.detectChanges();
    http.match((r) => r.url.startsWith(`${API_URL}/productos`)).forEach((r) => r.flush({ contenido: [] }));
    http.expectOne(`${API_URL}/compras`).flush([compraCompletada]);
    http.expectOne(`${API_URL}/puntos/historial`).flush([]);
    const c = fixture.componentInstance as any;
    expect(c.puntos()).toBe(0);

    c.abrirDialogo('resena', compraCompletada);
    c.comentario.set('Muy bueno');
    c.enviarDialogo();
    http.expectOne({ method: 'POST', url: `${API_URL}/resenas` }).flush({});

    http.expectOne(`${API_URL}/compras`).flush([{ ...compraCompletada }]);
    http.expectOne(`${API_URL}/puntos/historial`).flush([ganado(10)]);
    expect(c.puntos()).toBe(10);
    http.verify();
  });
});
