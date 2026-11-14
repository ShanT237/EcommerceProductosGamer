import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Notificacion, Sesion } from '../modelos/modelos';
import { AuthService } from './auth.service';
import { NotificacionService } from './notificacion.service';

function notificacion(id: string, leida = false): Notificacion {
  return {
    id,
    productoId: 'p1',
    tipo: 'DESCUENTO',
    mensaje: `Aviso ${id}`,
    fecha: '2026-11-14T10:00:00',
    leida,
  };
}

describe('NotificacionService', () => {
  let servicio: NotificacionService;
  let http: HttpTestingController;
  let sesion: ReturnType<typeof signal<Sesion | null>>;

  const getLista = () => ({ method: 'GET', url: `${API_URL}/notificaciones` });
  const putLeida = (id: string) => ({ method: 'PUT', url: `${API_URL}/notificaciones/${id}/leida` });

  function iniciar(rol: Sesion['rol'], id = 'u1') {
    sesion = signal<Sesion | null>({ id, correo: 'jose@correo.com', rol, exp: 9999999999 });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    servicio = TestBed.inject(NotificacionService);
    http = TestBed.inject(HttpTestingController);
    TestBed.tick(); // deja correr el efecto que carga la lista al iniciar
  }

  afterEach(() => http.verify());

  it('cuentaLasNoLeidas', () => {
    iniciar('COMPRADOR');
    http.expectOne(getLista()).flush([notificacion('n1'), notificacion('n2', true), notificacion('n3')]);

    expect(servicio.notificaciones().length).toBe(3);
    expect(servicio.noLeidas()).toBe(2);
  });

  it('marcarLeidaActualizaElEstadoLocal', () => {
    iniciar('COMPRADOR');
    http.expectOne(getLista()).flush([notificacion('n1'), notificacion('n2')]);

    servicio.marcarLeida('n1').subscribe();
    http.expectOne(putLeida('n1')).flush(notificacion('n1', true));

    expect(servicio.notificaciones().find((n) => n.id === 'n1')?.leida).toBe(true);
    expect(servicio.notificaciones().find((n) => n.id === 'n2')?.leida).toBe(false);
    expect(servicio.noLeidas()).toBe(1);
  });

  it('vendedorNoPideNotificaciones', () => {
    iniciar('VENDEDOR');

    http.expectNone(getLista());
    expect(servicio.notificaciones()).toEqual([]);
    expect(servicio.noLeidas()).toBe(0);
  });

  it('seVaciaYRecargaAlCambiarDeUsuario', () => {
    iniciar('COMPRADOR', 'u1');
    http.expectOne(getLista()).flush([notificacion('n1')]);
    expect(servicio.noLeidas()).toBe(1);

    sesion.set({ id: 'u2', correo: 'ana@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
    TestBed.tick();

    expect(servicio.notificaciones()).toEqual([]);
    http.expectOne(getLista()).flush([]);
    expect(servicio.noLeidas()).toBe(0);
  });

  it('seVaciaAlCerrarSesion', () => {
    iniciar('COMPRADOR');
    http.expectOne(getLista()).flush([notificacion('n1')]);

    sesion.set(null);
    TestBed.tick();

    expect(servicio.notificaciones()).toEqual([]);
    http.expectNone(getLista());
  });
});
