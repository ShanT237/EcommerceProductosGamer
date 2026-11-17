import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { MENSAJE_CUENTA_INACTIVA } from '../servicios/api.util';
import { AuthService } from '../servicios/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let valida: boolean;
  const verificarSesion = vi.fn(() => valida);
  const cerrarSesion = vi.fn();

  beforeEach(() => {
    valida = true;
    verificarSesion.mockClear();
    cerrarSesion.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: signal('tok'), verificarSesion, cerrarSesion } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  it('agrega el Bearer cuando la sesión es válida', () => {
    http.get(`${API_URL}/compras`).subscribe();

    const req = controller.expectOne(`${API_URL}/compras`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
    req.flush([]);
  });

  it('si el token se perdió no envía la petición y falla con 401', () => {
    valida = false;
    let status = 0;

    http.get(`${API_URL}/compras`).subscribe({ error: (e) => (status = e.status) });

    controller.expectNone(`${API_URL}/compras`);
    expect(status).toBe(401);
  });

  it('un 401 genérico cierra la sesión con el aviso por defecto', () => {
    http.get(`${API_URL}/compras`).subscribe({ error: () => undefined });

    controller
      .expectOne(`${API_URL}/compras`)
      .flush({ status: 401, mensaje: 'Token ausente, inválido o expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(cerrarSesion).toHaveBeenCalledWith(true, undefined);
  });

  it('un 401 con CUENTA_INACTIVA (baja con el token aún vigente) cierra la sesión explicando el motivo', () => {
    http.get(`${API_URL}/compras`).subscribe({ error: () => undefined });

    controller
      .expectOne(`${API_URL}/compras`)
      .flush(
        { status: 401, mensaje: 'La cuenta se encuentra inactiva o fue dada de baja', codigo: 'CUENTA_INACTIVA' },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(cerrarSesion).toHaveBeenCalledWith(true, MENSAJE_CUENTA_INACTIVA);
  });

  it('un error distinto de 401 no cierra la sesión', () => {
    http.get(`${API_URL}/compras`).subscribe({ error: () => undefined });

    controller.expectOne(`${API_URL}/compras`).flush({ mensaje: 'boom' }, { status: 500, statusText: 'Server Error' });

    expect(cerrarSesion).not.toHaveBeenCalled();
  });

  it('el login no pasa por la verificación de sesión', () => {
    http.post(`${API_URL}/auth/login`, {}).subscribe();

    controller.expectOne(`${API_URL}/auth/login`).flush({ token: 'x' });
    expect(verificarSesion).not.toHaveBeenCalled();
  });
});