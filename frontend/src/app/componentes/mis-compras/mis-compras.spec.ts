import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Compra } from '../../modelos/modelos';
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
      contenido: [], pagina: 0, tamano: 100, totalElementos: 0, totalPaginas: 0,
    });
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush(lista);
    http.expectOne({ method: 'GET', url: `${API_URL}/puntos/historial` }).flush([]);
    fixture.detectChanges();
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

    boton('Confirmar pago')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/confirmar` }).flush({ pedidoId: 'ped1', compras: [], total: 200 });
    // El mensaje habla del pedido completo (2 productos, 200), no de la línea suelta.
    expect(pedir.mock.calls[0][0].mensaje).toContain('pedido completo (2 producto(s))');
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush([]);
  });

  it('cancelar una línea de un pedido usa PUT /pedidos/{id}/cancelar', async () => {
    abrir([compra('c1', 'ped1'), compra('c2', 'ped1')]);

    boton('Cancelar')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/cancelar` }).flush({ pedidoId: 'ped1', compras: [], total: 200 });
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush([]);
  });

  it('una compra suelta (sin pedido) sigue usando los endpoints de compra', async () => {
    abrir([compra('c9', null)]);

    boton('Confirmar pago')!.click();
    await esperar();

    http.expectOne({ method: 'PUT', url: `${API_URL}/compras/c9/confirmar` }).flush(compra('c9', null, { estado: 'COMPLETADA' }));
    http.expectOne({ method: 'GET', url: `${API_URL}/compras` }).flush([]);
  });

  it('las líneas de un pedido no ofrecen aplicar cupón por separado', () => {
    abrir([compra('c1', 'ped1'), compra('c9', null)]);

    const botonesCupon = [...(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)].filter(
      (b) => b.textContent?.trim() === 'Cupón',
    );
    expect(botonesCupon.length).toBe(1); // solo la compra suelta
  });
});
