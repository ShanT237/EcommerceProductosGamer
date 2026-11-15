import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { AuthService } from '../servicios/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let valida: boolean;
  const verificarSesion = vi.fn(() => valida);

  beforeEach(() => {
    valida = true;
    verificarSesion.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: signal('tok'), verificarSesion, cerrarSesion: vi.fn() } },
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

  it('el login no pasa por la verificación de sesión', () => {
    http.post(`${API_URL}/auth/login`, {}).subscribe();

    controller.expectOne(`${API_URL}/auth/login`).flush({ token: 'x' });
    expect(verificarSesion).not.toHaveBeenCalled();
  });
});
