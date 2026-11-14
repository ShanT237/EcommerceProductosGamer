import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Notificacion, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { NotificacionService } from '../../servicios/notificacion.service';
import { ToastService } from '../../servicios/toast.service';
import { Notificaciones } from './notificaciones';

function notificacion(id: string, leida = false): Notificacion {
  return {
    id,
    productoId: 'p1',
    tipo: 'DESCUENTO',
    mensaje: `El producto ${id} ha bajado de precio`,
    fecha: '2026-11-14T10:00:00',
    leida,
  };
}

describe('Notificaciones (pantalla)', () => {
  let fixture: ComponentFixture<Notificaciones>;
  let http: HttpTestingController;
  let servicio: NotificacionService;
  let toast: ToastService;

  const getLista = () => ({ method: 'GET', url: `${API_URL}/notificaciones` });
  const putLeida = (id: string) => ({ method: 'PUT', url: `${API_URL}/notificaciones/${id}/leida` });
  const filas = () => Array.from(fixture.nativeElement.querySelectorAll('.notificacion')) as HTMLButtonElement[];

  /** El servicio carga al iniciar sesión y la pantalla vuelve a cargar al abrirse: se responden ambas. */
  function abrirConLista(lista: Notificacion[]) {
    fixture = TestBed.createComponent(Notificaciones);
    fixture.detectChanges();
    http.match(getLista()).forEach((r) => r.flush(lista));
    fixture.detectChanges();
  }

  beforeEach(() => {
    const sesion = signal<Sesion | null>({ id: 'u1', correo: 'jose@correo.com', rol: 'COMPRADOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [Notificaciones],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    servicio = TestBed.inject(NotificacionService);
    toast = TestBed.inject(ToastService);
    TestBed.tick(); // el efecto del servicio hace la primera carga
  });

  afterEach(() => http.verify());

  it('muestraLasNotificacionesYResaltaLasNoLeidas', () => {
    abrirConLista([notificacion('n1'), notificacion('n2', true), notificacion('n3')]);

    expect(filas().length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.notificacion.no-leida').length).toBe(2);
    expect(filas()[0].textContent).toContain('El producto n1 ha bajado de precio');
  });

  it('alHacerClicLlamaAlPutYBajaElContador', () => {
    abrirConLista([notificacion('n1'), notificacion('n2')]);
    expect(servicio.noLeidas()).toBe(2);

    filas()[0].click();
    http.expectOne(putLeida('n1')).flush(notificacion('n1', true));
    fixture.detectChanges();

    expect(servicio.noLeidas()).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('.notificacion.no-leida').length).toBe(1);
  });

  it('unaYaLeidaNoLlamaAlPut', () => {
    abrirConLista([notificacion('n1', true)]);

    filas()[0].click();

    http.expectNone(putLeida('n1'));
  });

  it('sinNotificacionesMuestraMensajeVacio', () => {
    abrirConLista([]);

    expect(filas().length).toBe(0);
    expect(fixture.nativeElement.querySelector('.vacio')?.textContent).toContain('No tienes notificaciones');
  });

  it('unErrorAlMarcarMuestraElMensajeDelBackend', () => {
    abrirConLista([notificacion('n1')]);

    filas()[0].click();
    http.expectOne(putLeida('n1')).flush(
      { mensaje: 'La notificación no pertenece al usuario' },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('no pertenece al usuario');
    expect(servicio.noLeidas()).toBe(1);
  });
});
