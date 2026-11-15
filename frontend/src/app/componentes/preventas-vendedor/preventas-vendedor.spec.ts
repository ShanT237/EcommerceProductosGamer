import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { AuthService } from '../../servicios/auth.service';
import { ToastService } from '../../servicios/toast.service';
import { PreventasVendedor } from './preventas-vendedor';

describe('PreventasVendedor', () => {
  let fixture: ComponentFixture<PreventasVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const producto = {
    id: 'p1',
    vendedorId: 'v1',
    nombre: 'Mouse',
    descripcion: 'Mouse RGB',
    precio: 100000,
    stock: 5,
    gama: 'MEDIA',
    exclusividad: 'NORMAL',
    categoria: null,
  };
  type FormularioPreventa = {
    productoId: WritableSignal<string>;
    fechaLanzamiento: WritableSignal<string>;
    descripcion: WritableSignal<string>;
    programar: () => void;
  };

  function abrir() {
    fixture = TestBed.createComponent(PreventasVendedor);
    fixture.detectChanges();
    http.expectOne((req) => req.method === 'GET' && req.url.startsWith(`${API_URL}/productos`)).flush({
      contenido: [producto], pagina: 0, tamano: 100, totalElementos: 1, totalPaginas: 1,
    });
    http.expectOne({ method: 'GET', url: `${API_URL}/preventas` }).flush([]);
    fixture.detectChanges();
  }

  function completarFormulario(fecha: string) {
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.productoId.set('p1');
    componente.fechaLanzamiento.set(fecha);
    componente.descripcion.set('Edición especial');
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PreventasVendedor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion: signal({ id: 'v1', correo: 'v@test.com', rol: 'VENDEDOR', exp: 9999999999 }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => http.verify());

  it('fechaPasadaONoElegidaNoEnviaPeticion', () => {
    abrir();
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    completarFormulario('');
    componente.programar();
    fixture.detectChanges();

    expect(raiz().textContent).toContain('La fecha de lanzamiento debe ser, como mínimo, mañana.');
    http.expectNone({ method: 'POST', url: `${API_URL}/preventas` });

    completarFormulario('2000-01-01');
    componente.programar();
    fixture.detectChanges();
    expect(raiz().textContent).toContain('La fecha de lanzamiento debe ser, como mínimo, mañana.');
    http.expectNone({ method: 'POST', url: `${API_URL}/preventas` });
  });

  it('programaLaPreventaYLaMuestraEnLaLista', () => {
    abrir();
    completarFormulario('2099-12-01');
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.programar();
    fixture.detectChanges();

    const req = http.expectOne({ method: 'POST', url: `${API_URL}/preventas` });
    expect(req.request.body).toEqual({
      productoId: 'p1',
      fechaLanzamiento: '2099-12-01',
      descripcion: 'Edición especial',
    });
    req.flush({ id: 'pr1', productoId: 'p1', fechaLanzamiento: '2099-12-01', descripcion: 'Edición especial' });
    fixture.detectChanges();

    expect(raiz().textContent).toContain('Mouse');
    expect(raiz().textContent).toContain('Edición especial');
    expect(toast.toasts().some((mensaje) => mensaje.texto === 'Preventa programada correctamente.')).toBe(true);
  });

  it('unaSegundaPreventaMuestraElError409', () => {
    abrir();
    completarFormulario('2099-12-01');
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.programar();
    http.expectOne({ method: 'POST', url: `${API_URL}/preventas` }).flush(
      { mensaje: 'El producto ya tiene una preventa programada' },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(raiz().querySelector('[role="alert"]')?.textContent)
      .toContain('El producto ya tiene una preventa programada');
  });
});
