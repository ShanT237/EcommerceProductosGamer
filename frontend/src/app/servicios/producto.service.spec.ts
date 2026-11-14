import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { ProductoDescuento } from '../modelos/modelos';
import { ProductoService } from './producto.service';

describe('ProductoService (descuento inmediato)', () => {
  let servicio: ProductoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ProductoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('aplicarDescuento hace POST y devuelve el precio original y el porcentaje', () => {
    let resultado: ProductoDescuento | undefined;
    servicio.aplicarDescuento('p1', 20).subscribe((r) => (resultado = r));

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/descuentos` });
    expect(req.request.body).toEqual({ productoId: 'p1', porcentaje: 20 });
    req.flush({ id: 'p1', nombre: 'Mouse', nuevoPrecio: 80, precioOriginal: 100, porcentajeDescuento: 20 });

    expect(resultado?.precioOriginal).toBe(100);
    expect(resultado?.porcentajeDescuento).toBe(20);
  });

  it('quitarDescuento hace DELETE al producto y devuelve el precio restaurado', () => {
    let resultado: ProductoDescuento | undefined;
    servicio.quitarDescuento('p1').subscribe((r) => (resultado = r));

    const req = http.expectOne({ method: 'DELETE', url: `${API_URL}/descuentos/p1` });
    req.flush({ id: 'p1', nombre: 'Mouse', nuevoPrecio: 100, precioOriginal: null, porcentajeDescuento: null });

    expect(resultado?.nuevoPrecio).toBe(100);
    expect(resultado?.precioOriginal).toBeNull();
  });
});
