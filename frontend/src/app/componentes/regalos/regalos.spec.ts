import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { MetaRecompensa, ProgresoRegalos, Regalo } from '../../modelos/modelos';
import { ToastService } from '../../servicios/toast.service';
import { Regalos } from './regalos';

function regalo(extra: Partial<Regalo> = {}): Regalo {
  return {
    id: 'r1', estado: 'DISPONIBLE', idProducto: 'p1', idCombo: null, idUsuario: null,
    notificado: false, fechaCreacion: '2026-11-15T10:00:00', fechaEntrega: null, ...extra,
  };
}

function progreso(extra: Partial<ProgresoRegalos> = {}): ProgresoRegalos {
  return { comprasCompletadas: 7, comprasPorRegalo: 5, regalosAsignados: 0, regalosDisponibles: 1, comprasParaSiguiente: 3, ...extra };
}

describe('Regalos (comprador)', () => {
  let fixture: ComponentFixture<Regalos>;
  let http: HttpTestingController;
  let toast: ToastService;

  const get = (ruta: string) => ({ method: 'GET', url: `${API_URL}/regalos/${ruta}` });
  const raiz = () => fixture.nativeElement as HTMLElement;
  const botones = (clase: string) => Array.from(raiz().querySelectorAll<HTMLButtonElement>(`button.${clase}`));

  function abrir(datos: { progreso?: ProgresoRegalos; disponibles?: Regalo[]; mios?: Regalo[]; metas?: MetaRecompensa[] } = {}) {
    TestBed.configureTestingModule({
      imports: [Regalos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(Regalos);
    fixture.detectChanges();
    http.expectOne(get('progreso')).flush(datos.progreso ?? progreso());
    http.expectOne(get('disponibles')).flush(datos.disponibles ?? [regalo()]);
    http.expectOne(get('mios')).flush(datos.mios ?? []);
    // Nombres de producto y combo (llegan aparte y no bloquean la pantalla)
    http.match((r) => r.url.startsWith(`${API_URL}/productos`)).forEach((r) => r.flush({ contenido: [] }));
    http.match((r) => r.url === `${API_URL}/combos`).forEach((r) => r.flush([]));
    http.expectOne({ method: 'GET', url: `${API_URL}/metas-recompensa` }).flush(datos.metas ?? []);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('muestraElProgresoYCuantasComprasFaltan', () => {
    abrir({ progreso: progreso({ comprasCompletadas: 7, comprasParaSiguiente: 3 }) });

    const texto = raiz().textContent ?? '';
    expect(texto).toContain('Llevas 7 compras completadas');
    expect(texto).toContain('Te faltan 3 compra(s) para tu próximo regalo');
  });

  it('muestraLasMetasActivasComoInformacion', () => {
    abrir({ metas: [{ id: 'm1', comprasRequeridas: 5, productoRegaloId: 'p1', activa: true }] });

    const texto = raiz().textContent ?? '';
    expect(texto).toContain('Metas de recompensa');
    expect(texto).toContain('5 compras');
  });

  it('sinMetasActivasNoMuestraLaSeccion', () => {
    abrir();

    expect(raiz().textContent).not.toContain('Metas de recompensa');
  });

  it('elegirDeshabilitadoSinRegalosDisponibles', () => {
    abrir({ progreso: progreso({ comprasCompletadas: 2, regalosDisponibles: 0, comprasParaSiguiente: 3 }) });

    expect(botones('elegir').length).toBe(1);
    expect(botones('elegir')[0].disabled).toBe(true);
    expect(raiz().textContent).toContain('Aún no tienes regalos por elegir');
  });

  it('elegirHacePutAsignarYRecarga', () => {
    abrir();
    expect(botones('elegir')[0].disabled).toBe(false);

    botones('elegir')[0].click();
    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/asignar` })
      .flush(regalo({ estado: 'ELEGIDO', idUsuario: 'u1' }));

    // Recarga progreso y listas: el regalo pasó de "disponibles" a "mis regalos"
    http.expectOne(get('progreso')).flush(progreso({ regalosAsignados: 1, regalosDisponibles: 0 }));
    http.expectOne(get('disponibles')).flush([]);
    http.expectOne(get('mios')).flush([regalo({ estado: 'ELEGIDO', idUsuario: 'u1' })]);
    fixture.detectChanges();

    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
    expect(botones('elegir').length).toBe(0);
    expect(raiz().textContent).toContain('Por enviar');
  });

  it('un422AlElegirMuestraElMensajeDelBackend', () => {
    abrir();

    botones('elegir')[0].click();
    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/asignar` })
      .flush({ mensaje: 'El usuario aún no alcanza la meta de 5 compras completadas' },
        { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    const error = toast.toasts().find((t) => t.tipo === 'error');
    expect(error?.texto).toContain('aún no alcanza la meta');
    // No se recarga nada y el botón vuelve a estar disponible para reintentar
    expect(botones('elegir')[0].disabled).toBe(false);
  });

  it('confirmarRecepcionSoloEnEstadoEnviado', () => {
    abrir({
      disponibles: [],
      mios: [
        regalo({ id: 'r2', estado: 'ELEGIDO', idUsuario: 'u1' }),
        regalo({ id: 'r3', estado: 'ENVIADO', idUsuario: 'u1' }),
        regalo({ id: 'r4', estado: 'ENTREGADO', idUsuario: 'u1', fechaEntrega: '2026-11-16T10:00:00' }),
      ],
    });

    expect(botones('confirmar').length).toBe(1);

    botones('confirmar')[0].click();
    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r3/entrega` })
      .flush(regalo({ id: 'r3', estado: 'ENTREGADO', idUsuario: 'u1' }));
    http.expectOne(get('progreso')).flush(progreso());
    http.expectOne(get('disponibles')).flush([]);
    http.expectOne(get('mios')).flush([]);

    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
  });

  it('si falla la carga muestra el error y permite reintentar', () => {
    TestBed.configureTestingModule({
      imports: [Regalos],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Regalos);
    fixture.detectChanges();
    http.expectOne(get('progreso')).flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });
    // forkJoin cancela las peticiones hermanas al fallar una; solo se responden las que siguen vivas
    http.match(() => true).filter((r) => !r.cancelled).forEach((r) => r.flush([]));
    fixture.detectChanges();

    expect(raiz().textContent).toContain('Error del servidor');
    expect(raiz().textContent).toContain('Reintentar');
  });
});
