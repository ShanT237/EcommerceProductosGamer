import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Regalo } from '../modelos/modelos';
import { RegaloService } from './regalo.service';

const REGALO: Regalo = {
  id: 'r1', estado: 'DISPONIBLE', idProducto: 'p1', idCombo: null, idUsuario: null,
  notificado: false, fechaCreacion: '2026-11-15T10:00:00', fechaEntrega: null,
};

describe('RegaloService', () => {
  let servicio: RegaloService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(RegaloService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('progreso hace GET a /regalos/progreso', () => {
    servicio.progreso().subscribe();

    http.expectOne({ method: 'GET', url: `${API_URL}/regalos/progreso` })
      .flush({ comprasCompletadas: 7, comprasPorRegalo: 5, regalosAsignados: 0, regalosDisponibles: 1, comprasParaSiguiente: 3 });
  });

  it('disponibles, mios y porEnviar hacen GET a su endpoint', () => {
    servicio.disponibles().subscribe();
    servicio.mios().subscribe();
    servicio.porEnviar().subscribe();

    http.expectOne({ method: 'GET', url: `${API_URL}/regalos/disponibles` }).flush([REGALO]);
    http.expectOne({ method: 'GET', url: `${API_URL}/regalos/mios` }).flush([]);
    http.expectOne({ method: 'GET', url: `${API_URL}/regalos/por-enviar` }).flush([]);
  });

  it('crear hace POST a /regalos con el producto o el combo en el body', () => {
    servicio.crear({ productoId: 'p1' }).subscribe();

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/regalos` });
    expect(req.request.body).toEqual({ productoId: 'p1' });
    req.flush(REGALO);
  });

  it('asignar hace PUT a /regalos/{id}/asignar', () => {
    servicio.asignar('r1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/asignar` }).flush({ ...REGALO, estado: 'ELEGIDO' });
  });

  it('enviar hace PUT a /regalos/{id}/enviar', () => {
    servicio.enviar('r1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/enviar` }).flush({ ...REGALO, estado: 'ENVIADO' });
  });

  it('confirmarEntrega hace PUT a /regalos/{id}/entrega', () => {
    servicio.confirmarEntrega('r1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/entrega` }).flush({ ...REGALO, estado: 'ENTREGADO' });
  });
});
