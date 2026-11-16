import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { MetaRecompensa } from '../modelos/modelos';
import { MetaService } from './meta.service';

const META: MetaRecompensa = { id: 'm1', comprasRequeridas: 5, productoRegaloId: 'p1', activa: true };

describe('MetaService', () => {
  let servicio: MetaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(MetaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar hace GET a /metas-recompensa', () => {
    let recibidas: MetaRecompensa[] = [];
    servicio.listar().subscribe((l) => (recibidas = l));

    http.expectOne({ method: 'GET', url: `${API_URL}/metas-recompensa` }).flush([META]);
    expect(recibidas).toEqual([META]);
  });

  it('crear hace POST con el umbral y el producto regalo', () => {
    servicio.crear({ comprasRequeridas: 5, productoRegaloId: 'p1' }).subscribe();

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/metas-recompensa` });
    expect(req.request.body).toEqual({ comprasRequeridas: 5, productoRegaloId: 'p1' });
    req.flush(META);
  });

  it('actualizarUmbral hace PUT con nuevoUmbral', () => {
    servicio.actualizarUmbral('m1', 8).subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/metas-recompensa/m1/umbral` });
    expect(req.request.body).toEqual({ nuevoUmbral: 8 });
    req.flush({ ...META, comprasRequeridas: 8 });
  });

  it('desactivar hace PUT a /metas-recompensa/{id}/desactivar', () => {
    servicio.desactivar('m1').subscribe();

    http.expectOne({ method: 'PUT', url: `${API_URL}/metas-recompensa/m1/desactivar` })
      .flush({ ...META, activa: false });
  });
});
