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

  it('modificarHacePutYActualizaElIndice', () => {
    servicio.futuras.set(new Map([['p1', preventa]]));
    const modificada: Preventa = { ...preventa, fechaLanzamiento: '2027-01-15', descripcion: 'Nueva' };

    let recibida: Preventa | undefined;
    servicio.modificar('pr1', '2027-01-15', 'Nueva').subscribe((p) => (recibida = p));
    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/preventas/pr1` });
    expect(req.request.body).toEqual({ fechaLanzamiento: '2027-01-15', descripcion: 'Nueva' });
    req.flush(modificada);

    expect(recibida).toEqual(modificada);
    expect(servicio.futuras().get('p1')).toEqual(modificada);
    expect(servicio.futuras().size).toBe(1);
  });

  it('cancelarHaceDeleteYQuitaLaPreventaDelIndice', () => {
    const otra: Preventa = { ...preventa, id: 'pr2', productoId: 'p2' };
    servicio.futuras.set(new Map([['p1', preventa], ['p2', otra]]));

    servicio.cancelar('pr1').subscribe();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/preventas/pr1` }).flush(null, { status: 204, statusText: 'No Content' });

    expect(servicio.futuras().has('p1')).toBe(false);
    expect(servicio.futuras().get('p2')).toEqual(otra);
  });

  it('siCancelarFallaLaPreventaSeConserva', () => {
    servicio.futuras.set(new Map([['p1', preventa]]));

    servicio.cancelar('pr1').subscribe({ error: () => undefined });
    http.expectOne({ method: 'DELETE', url: `${API_URL}/preventas/pr1` }).flush(
      { mensaje: 'La preventa ya fue lanzada y no se puede cancelar' },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    expect(servicio.futuras().get('p1')).toEqual(preventa);
  });
});
