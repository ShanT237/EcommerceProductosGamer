import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { API_URL } from '../config';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

const CLAVE = 'gamerzone.token';

function jwt(expSegundos: number): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: 'u1', email: 'jose@correo.com', rol: 'COMPRADOR', exp: expSegundos })}.firma`;
}
const enUnaHora = () => Math.floor(Date.now() / 1000) + 3600;

describe('AuthService.verificarSesion', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  function iniciarSesion(token: string) {
    auth.login('jose@correo.com', 'x').subscribe();
    http.expectOne(`${API_URL}/auth/login`).flush({ token });
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => localStorage.clear());

  it('sin sesión iniciada no hace nada', () => {
    expect(auth.verificarSesion()).toBe(true);
    expect(navegar).not.toHaveBeenCalled();
  });

  it('con un token válido y guardado mantiene la sesión', () => {
    iniciarSesion(jwt(enUnaHora()));

    expect(auth.verificarSesion()).toBe(true);
    expect(auth.autenticado()).toBe(true);
  });

  it('si el token se borra del localStorage cierra la sesión y manda al login', () => {
    iniciarSesion(jwt(enUnaHora()));

    localStorage.removeItem(CLAVE); // lo que hace la persona desde DevTools

    expect(auth.verificarSesion()).toBe(false);
    expect(auth.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login']);
    expect(TestBed.inject(ToastService).toasts().some((t) => t.tipo === 'info')).toBe(true);
  });

  it('cerrarSesion(true, mensaje) avisa con el texto indicado', () => {
    iniciarSesion(jwt(enUnaHora()));

    auth.cerrarSesion(true, 'Tu cuenta fue dada de baja.');

    expect(auth.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login']);
    expect(TestBed.inject(ToastService).toasts().some((t) => t.texto === 'Tu cuenta fue dada de baja.')).toBe(true);
  });

  it('cerrarSesion(true) sin mensaje usa el aviso por defecto', () => {
    iniciarSesion(jwt(enUnaHora()));

    auth.cerrarSesion(true);

    expect(
      TestBed.inject(ToastService).toasts().some((t) => t.texto === 'Tu sesión terminó o expiró. Vuelve a ingresar.'),
    ).toBe(true);
  });

  it('si el token guardado se reemplaza por uno inválido cierra la sesión', () => {
    iniciarSesion(jwt(enUnaHora()));

    localStorage.setItem(CLAVE, 'token-roto');

    expect(auth.verificarSesion()).toBe(false);
    expect(auth.autenticado()).toBe(false);
  });

  it('si el token venció cierra la sesión aunque siga en el localStorage', () => {
    const vencePronto = jwt(Math.floor(Date.now() / 1000) + 1);
    iniciarSesion(vencePronto);
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 5000);

    expect(auth.verificarSesion()).toBe(false);
    expect(auth.autenticado()).toBe(false);
    vi.restoreAllMocks();
  });
});