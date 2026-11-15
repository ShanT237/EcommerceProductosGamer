import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Preventa } from '../modelos/modelos';
import { PreventaService } from './preventa.service';

const preventa: Preventa = {
  id: 'pr1',
  productoId: 'p1',
  fechaLanzamiento: '2026-12-01',
  descripcion: 'Edición especial',
};

describe('PreventaService', () => {
  let servicio: PreventaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(PreventaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('cargaYIndexaPorProducto', () => {
    let recibidas: Preventa[] | undefined;
    servicio.cargar().subscribe((lista) => (recibidas = lista));
    http.expectOne({ method: 'GET', url: `${API_URL}/preventas` }).flush([preventa]);

    expect(recibidas).toEqual([preventa]);
    expect(servicio.futuras().get('p1')).toEqual(preventa);
    expect(servicio.cargada()).toBe(true);
    expect(servicio.cargando()).toBe(false);
  });

  it('programarHacePostYActualizaElIndice', () => {
    let recibida: Preventa | undefined;
    servicio.programar('p1', '2026-12-01', 'Edición especial').subscribe((p) => (recibida = p));
    const req = http.expectOne({ method: 'POST', url: `${API_URL}/preventas` });
    expect(req.request.body).toEqual({
      productoId: 'p1',
      fechaLanzamiento: '2026-12-01',
      descripcion: 'Edición especial',
    });
    req.flush(preventa);

    expect(recibida).toEqual(preventa);
    expect(servicio.futuras().get('p1')).toEqual(preventa);
  });
});
