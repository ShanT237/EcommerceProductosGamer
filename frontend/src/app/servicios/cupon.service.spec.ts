import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Cupon } from '../modelos/modelos';
import { CuponService } from './cupon.service';

const CUPON: Cupon = {
  id: 'c1', codigo: 'VERANO26', porcentaje: 10, vence: '2026-12-31', usosMaximos: 100, usos: 3, vendedorId: null,
};

describe('CuponService', () => {
  let servicio: CuponService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CuponService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consultar hace GET /cupones/{codigo}', () => {
    let recibido: Cupon | undefined;
    servicio.consultar('VERANO26').subscribe((c) => (recibido = c));

    http.expectOne({ method: 'GET', url: `${API_URL}/cupones/VERANO26` }).flush(CUPON);
    expect(recibido).toEqual(CUPON);
  });

  it('consultar recorta los espacios y codifica el código en la URL', () => {
    servicio.consultar('  ahorro 10/a ').subscribe();

    http.expectOne({ method: 'GET', url: `${API_URL}/cupones/ahorro%2010%2Fa` }).flush(CUPON);
  });

  it('misCupones hace GET /cupones', () => {
    let recibidos: Cupon[] | undefined;
    servicio.misCupones().subscribe((l) => (recibidos = l));

    http.expectOne({ method: 'GET', url: `${API_URL}/cupones` }).flush([CUPON]);
    expect(recibidos).toEqual([CUPON]);
  });

  it('crear hace POST /cupones con el cuerpo tal cual', () => {
    const nuevo = { codigo: 'GAMER10', porcentaje: 10, vence: '2026-12-31', usosMaximos: 5 };
    let creado: Cupon | undefined;
    servicio.crear(nuevo).subscribe((c) => (creado = c));

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/cupones` });
    expect(req.request.body).toEqual(nuevo);
    req.flush({ ...CUPON, ...nuevo, vendedorId: 'v1' });
    expect(creado?.codigo).toBe('GAMER10');
  });
});
