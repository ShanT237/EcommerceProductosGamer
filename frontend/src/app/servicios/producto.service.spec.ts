import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { ProductoDescuento, PromocionTemporada } from '../modelos/modelos';
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

describe('ProductoService (promociones de temporada)', () => {
  let servicio: ProductoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ProductoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('configurarPromocion hace POST con producto, porcentaje y fechas', () => {
    servicio.configurarPromocion('p1', 20, '2026-12-01', '2026-12-31').subscribe();

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/promociones` });
    expect(req.request.body).toEqual({ productoId: 'p1', porcentaje: 20, desde: '2026-12-01', hasta: '2026-12-31' });
    req.flush({});
  });

  it('listarPromociones hace GET y devuelve las del vendedor con el nombre del producto', () => {
    let resultado: PromocionTemporada[] | undefined;
    servicio.listarPromociones().subscribe((r) => (resultado = r));

    http.expectOne({ method: 'GET', url: `${API_URL}/promociones` }).flush([
      { id: 'pr1', productoId: 'p1', productoNombre: 'Mouse', porcentaje: 15, desde: '2026-12-01', hasta: '2026-12-31' },
    ]);

    expect(resultado?.length).toBe(1);
    expect(resultado?.[0].productoNombre).toBe('Mouse');
    expect(resultado?.[0].porcentaje).toBe(15);
  });

  it('modificarPromocion hace PUT a la promoción con porcentaje y fechas, sin el producto', () => {
    servicio.modificarPromocion('pr1', 30, '2026-12-05', '2026-12-20').subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/promociones/pr1` });
    expect(req.request.body).toEqual({ porcentaje: 30, desde: '2026-12-05', hasta: '2026-12-20' });
    req.flush({});
  });

  it('quitarPromocion hace DELETE a la promoción', () => {
    let completo = false;
    servicio.quitarPromocion('pr1').subscribe({ complete: () => (completo = true) });

    http.expectOne({ method: 'DELETE', url: `${API_URL}/promociones/pr1` })
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(completo).toBe(true);
  });
});
