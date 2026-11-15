import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Cupon } from '../../modelos/modelos';
import { ToastService } from '../../servicios/toast.service';
import { CuponesVendedor } from './cupones-vendedor';

function cupon(extra: Partial<Cupon> = {}): Cupon {
  return {
    id: 'c1', codigo: 'GAMER10', porcentaje: 10, vence: '2099-12-31', usosMaximos: 5, usos: 1, vendedorId: 'v1', ...extra,
  };
}

/** Signals protegidos del componente que las pruebas fijan directamente. */
type Interno = {
  codigo: WritableSignal<string>;
  porcentaje: WritableSignal<number | null>;
  vence: WritableSignal<string>;
  usosMaximos: WritableSignal<number | null>;
};

describe('CuponesVendedor', () => {
  let fixture: ComponentFixture<CuponesVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const getMios = () => ({ method: 'GET', url: `${API_URL}/cupones` });
  const postCrear = () => ({ method: 'POST', url: `${API_URL}/cupones` });
  const raiz = () => fixture.nativeElement as HTMLElement;
  const interno = () => fixture.componentInstance as unknown as Interno;
  const enviar = () => (raiz().querySelector('button.crear') as HTMLButtonElement).click();
  const textoError = () => raiz().querySelector('.error')?.textContent ?? '';

  function abrir(mios: Cupon[] = []) {
    TestBed.configureTestingModule({
      imports: [CuponesVendedor],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(CuponesVendedor);
    fixture.detectChanges();
    http.expectOne(getMios()).flush(mios);
    fixture.detectChanges();
  }

  function llenar(extra: Partial<{ codigo: string; porcentaje: number | null; vence: string; usosMaximos: number | null }> = {}) {
    const datos = { codigo: 'GAMER10', porcentaje: 10, vence: '2099-12-31', usosMaximos: 5, ...extra };
    interno().codigo.set(datos.codigo);
    interno().porcentaje.set(datos.porcentaje);
    interno().vence.set(datos.vence);
    interno().usosMaximos.set(datos.usosMaximos);
  }

  afterEach(() => http.verify());

  it('formularioInvalidoNoEnviaPeticion', () => {
    abrir();

    enviar();
    fixture.detectChanges();

    http.expectNone(postCrear());
    expect(textoError()).toContain('código');
  });

  it('unPorcentajeDeCienNoEnviaPeticion', () => {
    abrir();
    llenar({ porcentaje: 100 });

    enviar();
    fixture.detectChanges();

    http.expectNone(postCrear());
    expect(textoError()).toContain('porcentaje');
  });

  it('unVencimientoPasadoNoEnviaPeticion', () => {
    abrir();
    llenar({ vence: '2000-01-01' });

    enviar();
    fixture.detectChanges();

    http.expectNone(postCrear());
    expect(textoError()).toContain('antes de hoy');
  });

  it('losUsosMaximosDebenSerUnEnteroPositivo', () => {
    abrir();
    llenar({ usosMaximos: 0 });

    enviar();
    fixture.detectChanges();

    http.expectNone(postCrear());
    expect(textoError()).toContain('usos máximos');
  });

  it('creaElCuponYLoAgregaALaLista', () => {
    abrir([cupon({ id: 'c0', codigo: 'VIEJO5' })]);
    llenar({ codigo: '  GAMER10  ' });

    enviar();
    const post = http.expectOne(postCrear());
    expect(post.request.body).toEqual({ codigo: 'GAMER10', porcentaje: 10, vence: '2099-12-31', usosMaximos: 5 });
    post.flush(cupon({ id: 'c9', codigo: 'GAMER10', usos: 0 }));
    fixture.detectChanges();

    const tarjetas = Array.from(raiz().querySelectorAll('.cupon h4')).map((h) => h.textContent?.trim());
    expect(tarjetas).toEqual(['GAMER10', 'VIEJO5']);
    expect(toast.toasts().some((t) => t.tipo === 'ok' && t.texto.includes('GAMER10'))).toBe(true);
    expect(interno().codigo()).toBe(''); // el formulario queda limpio
  });

  it('unCodigoDuplicadoMuestraElError409', () => {
    abrir();
    llenar();

    enviar();
    http.expectOne(postCrear()).flush({ mensaje: 'Ya existe un cupón con ese código' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(textoError()).toContain('Ya existe un cupón con ese código');
    expect(raiz().querySelectorAll('.cupon').length).toBe(0);
    expect(interno().codigo()).toBe('GAMER10'); // se conserva lo escrito para corregirlo
    expect((raiz().querySelector('button.crear') as HTMLButtonElement).disabled).toBe(false);
  });

  it('listaLosCuponesConSuEstadoYSusUsos', () => {
    abrir([
      cupon({ id: 'a', codigo: 'VIGENTE', usos: 2, usosMaximos: 5 }),
      cupon({ id: 'b', codigo: 'VENCIDO', vence: '2000-01-01' }),
      cupon({ id: 'c', codigo: 'AGOTADO', usos: 5, usosMaximos: 5 }),
    ]);

    const estados = Array.from(raiz().querySelectorAll('.estado')).map((e) => e.textContent?.trim());
    expect(estados).toEqual(['vigente', 'vencido', 'agotado']);
    expect(raiz().querySelector('.cupon')?.textContent).toContain('2 de 5 usos');
  });

  it('sinCuponesMuestraElMensajeVacio', () => {
    abrir();

    expect(raiz().querySelector('.vacio')?.textContent).toContain('Aún no has creado cupones');
  });

  it('siFallaLaCargaMuestraElErrorYPermiteReintentar', () => {
    TestBed.configureTestingModule({
      imports: [CuponesVendedor],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CuponesVendedor);
    fixture.detectChanges();
    http.expectOne(getMios()).flush({ mensaje: 'Fallo temporal' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(raiz().querySelector('.vacio')?.textContent).toContain('Fallo temporal');

    (raiz().querySelector('.vacio button') as HTMLButtonElement).click();
    http.expectOne(getMios()).flush([cupon()]);
    fixture.detectChanges();

    expect(raiz().querySelectorAll('.cupon').length).toBe(1);
  });
});
