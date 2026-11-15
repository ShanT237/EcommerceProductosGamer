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

  it('crearPedido con cupón envía codigoCupon recortado en el mismo POST', () => {
    const items = [{ productoId: 'a', cantidad: 1 }];
    servicio.crearPedido(items, '  VERANO26 ').subscribe();

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/pedidos` });
    expect(req.request.body).toEqual({ items, codigoCupon: 'VERANO26' });
    req.flush(PEDIDO);
  });

  it('crearPedido sin cupón o con el código en blanco no envía el campo', () => {
    const items = [{ productoId: 'a', cantidad: 1 }];
    servicio.crearPedido(items, '   ').subscribe();
    servicio.crearPedido(items, undefined).subscribe();

    const reqs = http.match({ method: 'POST', url: `${API_URL}/pedidos` });
    expect(reqs.length).toBe(2);
    reqs.forEach((r) => {
      expect(r.request.body).toEqual({ items });
      expect('codigoCupon' in r.request.body).toBe(false);
      r.flush(PEDIDO);
    });
  });

  it('confirmarPedido hace PUT /pedidos/{id}/confirmar', () => {
    servicio.confirmarPedido('ped1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/confirmar` }).flush(PEDIDO);
  });

  it('cancelarPedido hace PUT /pedidos/{id}/cancelar', () => {
    servicio.cancelarPedido('ped1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/cancelar` }).flush(PEDIDO);
  });

  it('reembolsarPedido hace PUT /pedidos/{id}/reembolso', () => {
    servicio.reembolsarPedido('ped1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/pedidos/ped1/reembolso` }).flush(PEDIDO);
  });
});