import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Pedido } from '../modelos/modelos';
import { CompraService } from './compra.service';

const PEDIDO: Pedido = { pedidoId: 'ped1', compras: [], total: 0 };

describe('CompraService (pedidos)', () => {
  let servicio: CompraService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CompraService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('crearPedido hace POST /pedidos con la lista de ítems', () => {
    const items = [
      { productoId: 'a', cantidad: 2 },
      { productoId: 'b', cantidad: 1 },
    ];
    let recibido: Pedido | undefined;
    servicio.crearPedido(items).subscribe((p) => (recibido = p));

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/pedidos` });
    expect(req.request.body).toEqual({ items });
    req.flush(PEDIDO);
    expect(recibido).toEqual(PEDIDO);
  });

  it('confirmarPedido hace PUT /pedidos/{id}/confirmar', () => {
    servicio.confirmarPedido('ped1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/confirmar` }).flush(PEDIDO);
  });

  it('cancelarPedido hace PUT /pedidos/{id}/cancelar', () => {
    servicio.cancelarPedido('ped1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/cancelar` }).flush(PEDIDO);
  });
});
