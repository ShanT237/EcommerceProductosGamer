import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { ComboService } from './combo.service';

describe('ComboService', () => {
  let servicio: ComboService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ComboService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listarHaceGetACombos', () => {
    servicio.listar().subscribe();
    const req = http.expectOne(`${API_URL}/combos`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('miosHaceGetACombosMios', () => {
    servicio.mios().subscribe();
    const req = http.expectOne(`${API_URL}/combos/mios`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('crearHacePostConElCuerpoSinVendedorId', () => {
    const cuerpo = { nombre: 'Combo', descripcion: 'Desc', descuento: 15, productoIds: ['a', 'b'] };
    servicio.crear(cuerpo).subscribe();
    const req = http.expectOne(`${API_URL}/combos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({});
  });

  it('agregarProductoHacePutConLosIdsEnLaUrl', () => {
    servicio.agregarProducto('c1', 'p9').subscribe();
    const req = http.expectOne(`${API_URL}/combos/c1/productos/p9`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('quitarProductoHaceDeleteConLosIdsEnLaUrl', () => {
    servicio.quitarProducto('c1', 'p9').subscribe();
    const req = http.expectOne(`${API_URL}/combos/c1/productos/p9`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('comprarHacePostConLaCantidad', () => {
    servicio.comprar('c1', 3).subscribe();
    const req = http.expectOne(`${API_URL}/combos/c1/comprar`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ cantidad: 3 });
    req.flush({ comboId: 'c1', compras: [], total: 0 });
  });

  it('modificarHacePutConLosDatosSinProductos', () => {
    const cuerpo = { nombre: 'Combo Pro', descripcion: 'Desc', descuento: 25 };
    servicio.modificar('c1', cuerpo).subscribe();
    const req = http.expectOne(`${API_URL}/combos/c1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({});
  });

  it('eliminarHaceDeleteConElIdEnLaUrl', () => {
    servicio.eliminar('c1').subscribe();
    const req = http.expectOne(`${API_URL}/combos/c1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
