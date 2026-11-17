import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { PerfilVendedor, VendedorService } from './vendedor.service';

const PERFIL: PerfilVendedor = {
  id: 'v1', nombre: 'GamerZone', correo: 'ventas@gamerzone.com', tipo: 'EMPRESA', documento: '900123', activo: true,
};

describe('VendedorService', () => {
  let servicio: VendedorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(VendedorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtenerPerfil hace GET /vendedores/me', () => {
    let recibido: PerfilVendedor | undefined;
    servicio.obtenerPerfil().subscribe((p) => (recibido = p));

    http.expectOne({ method: 'GET', url: `${API_URL}/vendedores/me` }).flush(PERFIL);
    expect(recibido).toEqual(PERFIL);
  });

  it('editarPerfil hace PUT /vendedores/me/perfil solo con nombre y correo', () => {
    servicio.editarPerfil('GamerZone Pro', 'pro@gamerzone.com').subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/perfil` });
    expect(req.request.body).toEqual({ nombre: 'GamerZone Pro', correo: 'pro@gamerzone.com' });
    req.flush(PERFIL);
  });

  it('cambiarContrasena hace PUT /vendedores/me/contrasena', () => {
    servicio.cambiarContrasena('Actual2026', 'Nueva2027x').subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/contrasena` });
    expect(req.request.body).toEqual({ contrasenaActual: 'Actual2026', contrasenaNueva: 'Nueva2027x' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('darDeBaja hace PUT /vendedores/me/baja y devuelve la cuenta inactiva', () => {
    let recibido: PerfilVendedor | undefined;
    servicio.darDeBaja().subscribe((p) => (recibido = p));

    http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/baja` }).flush({ ...PERFIL, activo: false });
    expect(recibido?.activo).toBe(false);
  });
});
