import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { PerfilUsuario, UsuarioService } from './usuario.service';

const PERFIL: PerfilUsuario = { id: 'u1', nombre: 'Jose', correo: 'jose@correo.com', puntos: 30, activo: true };

describe('UsuarioService', () => {
  let servicio: UsuarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtenerPerfil hace GET /usuarios/me', () => {
    let recibido: PerfilUsuario | undefined;
    servicio.obtenerPerfil().subscribe((p) => (recibido = p));

    http.expectOne({ method: 'GET', url: `${API_URL}/usuarios/me` }).flush(PERFIL);
    expect(recibido).toEqual(PERFIL);
  });

  it('editarPerfil hace PUT /usuarios/me/perfil con nombre y correo', () => {
    servicio.editarPerfil('Jose Pro', 'pro@correo.com').subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/usuarios/me/perfil` });
    expect(req.request.body).toEqual({ nombre: 'Jose Pro', correo: 'pro@correo.com' });
    req.flush(PERFIL);
  });

  it('cambiarContrasena hace PUT /usuarios/me/contrasena', () => {
    servicio.cambiarContrasena('Actual2026', 'Nueva2027x').subscribe();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/usuarios/me/contrasena` });
    expect(req.request.body).toEqual({ contrasenaActual: 'Actual2026', contrasenaNueva: 'Nueva2027x' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('darDeBaja hace PUT /usuarios/me/baja y devuelve la cuenta inactiva', () => {
    let recibido: PerfilUsuario | undefined;
    servicio.darDeBaja().subscribe((p) => (recibido = p));

    http.expectOne({ method: 'PUT', url: `${API_URL}/usuarios/me/baja` }).flush({ ...PERFIL, activo: false });
    expect(recibido?.activo).toBe(false);
  });
});
