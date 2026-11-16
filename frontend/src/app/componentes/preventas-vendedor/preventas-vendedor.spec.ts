import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ToastService } from '../../servicios/toast.service';
import { PreventasVendedor } from './preventas-vendedor';

const esperar = () => new Promise<void>((r) => setTimeout(r));

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
    editandoId: WritableSignal<string | null>;
  };
  const existente = { id: 'pr1', productoId: 'p1', fechaLanzamiento: '2099-12-01', descripcion: 'Edición especial' };

  function abrir(preventas: unknown[] = []) {
    fixture = TestBed.createComponent(PreventasVendedor);
    fixture.detectChanges();
    http.expectOne((req) => req.method === 'GET' && req.url.startsWith(`${API_URL}/productos`)).flush({
      contenido: [producto], pagina: 0, tamano: 100, totalElementos: 1, totalPaginas: 1,
    });
    http.expectOne({ method: 'GET', url: `${API_URL}/preventas` }).flush(preventas);
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

  afterEach(() => {
    vi.restoreAllMocks();
    http.verify();
  });

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

  it('editarCargaLaPreventaEnElFormularioYBloqueaElProducto', () => {
    abrir([existente]);

    (raiz().querySelector('.editar-preventa') as HTMLButtonElement).click();
    fixture.detectChanges();

    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    expect(componente.editandoId()).toBe('pr1');
    expect(componente.fechaLanzamiento()).toBe('2099-12-01');
    expect(componente.descripcion()).toBe('Edición especial');
    expect(raiz().querySelector('h2')?.textContent).toContain('Editar preventa');
    const producto = raiz().querySelector('#producto-preventa') as HTMLInputElement;
    expect(producto.disabled).toBe(true);
    expect(producto.value).toBe('Mouse');
  });

  it('guardarLosCambiosHacePutYActualizaLaLista', () => {
    abrir([existente]);
    (raiz().querySelector('.editar-preventa') as HTMLButtonElement).click();
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.fechaLanzamiento.set('2099-12-24');
    componente.descripcion.set('  Reprogramada  ');
    componente.programar();
    fixture.detectChanges();

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/preventas/pr1` });
    expect(req.request.body).toEqual({ fechaLanzamiento: '2099-12-24', descripcion: 'Reprogramada' });
    req.flush({ ...existente, fechaLanzamiento: '2099-12-24', descripcion: 'Reprogramada' });
    fixture.detectChanges();

    expect(componente.editandoId()).toBeNull();
    expect(raiz().querySelector('h2')?.textContent).toContain('Programar preventa');
    expect(raiz().querySelector('.preventa')?.textContent).toContain('Reprogramada');
    expect(toast.toasts().some((mensaje) => mensaje.texto === 'Preventa actualizada correctamente.')).toBe(true);
  });

  it('editarConFechaPasadaNoEnviaPeticion', () => {
    abrir([existente]);
    (raiz().querySelector('.editar-preventa') as HTMLButtonElement).click();
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.fechaLanzamiento.set('2000-01-01');
    componente.programar();
    fixture.detectChanges();

    expect(raiz().textContent).toContain('La fecha de lanzamiento debe ser, como mínimo, mañana.');
    http.expectNone({ method: 'PUT', url: `${API_URL}/preventas/pr1` });
  });

  it('unErrorDelBackendAlModificarSeMuestraYConservaLaEdicion', () => {
    abrir([existente]);
    (raiz().querySelector('.editar-preventa') as HTMLButtonElement).click();
    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    componente.programar();
    http.expectOne({ method: 'PUT', url: `${API_URL}/preventas/pr1` }).flush(
      { mensaje: 'La preventa ya fue lanzada y no se puede modificar' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(raiz().querySelector('[role="alert"]')?.textContent)
      .toContain('La preventa ya fue lanzada y no se puede modificar');
    expect(componente.editandoId()).toBe('pr1');
  });

  it('descartarLosCambiosVuelveAlModoProgramar', () => {
    abrir([existente]);
    (raiz().querySelector('.editar-preventa') as HTMLButtonElement).click();
    fixture.detectChanges();

    (raiz().querySelector('.cancelar-edicion') as HTMLButtonElement).click();
    fixture.detectChanges();

    const componente = fixture.componentInstance as unknown as FormularioPreventa;
    expect(componente.editandoId()).toBeNull();
    expect(raiz().querySelector('h2')?.textContent).toContain('Programar preventa');
    expect(raiz().querySelector('.cancelar-edicion')).toBeNull();
  });

  it('cancelarConConfirmacionHaceDeleteYQuitaLaPreventaDeLaLista', async () => {
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
    abrir([existente]);

    (raiz().querySelector('.cancelar-preventa') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/preventas/pr1` })
      .flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(raiz().querySelector('.preventa')).toBeNull();
    expect(raiz().textContent).toContain('No tienes preventas futuras programadas.');
    expect(toast.toasts().some((mensaje) => mensaje.texto.includes('cancelada'))).toBe(true);
  });

  it('siNoSeConfirmaNoSeEnviaNingunaPeticion', async () => {
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(false);
    abrir([existente]);

    (raiz().querySelector('.cancelar-preventa') as HTMLButtonElement).click();
    await esperar();
    fixture.detectChanges();

    http.expectNone({ method: 'DELETE', url: `${API_URL}/preventas/pr1` });
    expect(raiz().querySelector('.preventa')).not.toBeNull();
  });

  it('unErrorAlCancelarMuestraElMensajeYConservaLaPreventa', async () => {
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
    abrir([existente]);

    (raiz().querySelector('.cancelar-preventa') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/preventas/pr1` }).flush(
      { mensaje: 'La preventa ya fue lanzada y no se puede cancelar' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('ya fue lanzada');
    expect(raiz().querySelector('.preventa')).not.toBeNull();
  });
});
