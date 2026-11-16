import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { MetaRecompensa, Producto } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ToastService } from '../../servicios/toast.service';
import { MetasVendedor } from './metas-vendedor';

const VENDEDOR = 'v1';
const URL_METAS = `${API_URL}/metas-recompensa`;

function producto(extra: Partial<Producto> = {}): Producto {
  return {
    id: 'p1', vendedorId: VENDEDOR, nombre: 'Mouse gamer', descripcion: 'Desc', precio: 100, stock: 5,
    gama: 'MEDIA', exclusividad: 'NORMAL', categoria: null, ...extra,
  };
}

function meta(extra: Partial<MetaRecompensa> = {}): MetaRecompensa {
  return { id: 'm1', comprasRequeridas: 5, productoRegaloId: 'p1', activa: true, ...extra };
}

/** Signals protegidos del componente que las pruebas fijan directamente. */
type Interno = {
  umbral: WritableSignal<number | null>;
  productoId: WritableSignal<string>;
  umbralEdit: WritableSignal<number | null>;
};

describe('MetasVendedor', () => {
  let fixture: ComponentFixture<MetasVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const interno = () => fixture.componentInstance as unknown as Interno;
  const botones = (clase: string) => Array.from(raiz().querySelectorAll<HTMLButtonElement>(`button.${clase}`));
  const textoError = () => raiz().querySelector('.error')?.textContent ?? '';
  const titulos = () => Array.from(raiz().querySelectorAll('.meta h4')).map((h) => h.textContent?.trim());

  function abrir(metas: MetaRecompensa[] = []) {
    TestBed.configureTestingModule({
      imports: [MetasVendedor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion: () => ({ id: VENDEDOR }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(MetasVendedor);
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: URL_METAS }).flush(metas);
    http.match((r) => r.url.startsWith(`${API_URL}/productos`))
      .forEach((r) => r.flush({ contenido: [producto(), producto({ id: 'p2', vendedorId: 'otro', nombre: 'Ajeno' })] }));
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('umbralCeroNoEnviaPeticion', () => {
    abrir();
    interno().umbral.set(0);
    interno().productoId.set('p1');

    botones('crear')[0].click();
    fixture.detectChanges();

    http.expectNone({ method: 'POST', url: URL_METAS });
    expect(textoError()).toContain('mayor que 0');
  });

  it('sin elegir el producto de regalo no envía la petición', () => {
    abrir();
    interno().umbral.set(5);

    botones('crear')[0].click();
    fixture.detectChanges();

    http.expectNone({ method: 'POST', url: URL_METAS });
    expect(textoError()).toContain('Elige el producto');
  });

  it('solo ofrece los productos del propio vendedor', () => {
    abrir();

    const opciones = Array.from(raiz().querySelectorAll('#mt-producto option')).map((o) => o.textContent?.trim());
    expect(opciones).toContain('Mouse gamer');
    expect(opciones).not.toContain('Ajeno');
  });

  it('creaLaMetaYLaListaActiva', () => {
    abrir([meta({ id: 'm10', comprasRequeridas: 10 })]);
    interno().umbral.set(3);
    interno().productoId.set('p1');

    botones('crear')[0].click();
    const req = http.expectOne({ method: 'POST', url: URL_METAS });
    expect(req.request.body).toEqual({ comprasRequeridas: 3, productoRegaloId: 'p1' });
    req.flush(meta({ id: 'm3', comprasRequeridas: 3 }));
    fixture.detectChanges();

    // Aparece en la lista, ordenada por umbral, y se limpia el formulario
    expect(titulos()).toEqual(['3 compras', '10 compras']);
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
    expect(interno().umbral()).toBeNull();
    expect(interno().productoId()).toBe('');
  });

  it('un error del backend al crear se muestra en el formulario', () => {
    abrir();
    interno().umbral.set(5);
    interno().productoId.set('p1');

    botones('crear')[0].click();
    http.expectOne({ method: 'POST', url: URL_METAS })
      .flush({ mensaje: 'El producto regalo no existe' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(textoError()).toContain('El producto regalo no existe');
  });

  it('desactivarHacePutYLaQuitaDeLaLista', () => {
    abrir([meta({ id: 'm1', comprasRequeridas: 5 }), meta({ id: 'm2', comprasRequeridas: 8 })]);
    expect(botones('desactivar').length).toBe(2);

    botones('desactivar')[0].click();
    http.expectOne({ method: 'PUT', url: `${URL_METAS}/m1/desactivar` }).flush(meta({ id: 'm1', activa: false }));
    fixture.detectChanges();

    expect(titulos()).toEqual(['8 compras']);
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
  });

  it('un404ODosDesactivacionesMuestranElMensajeDelBackend', () => {
    abrir([meta({ id: 'm1', comprasRequeridas: 5 }), meta({ id: 'm2', comprasRequeridas: 8 })]);

    // 404: la meta ya no existe
    botones('desactivar')[0].click();
    http.expectOne({ method: 'PUT', url: `${URL_METAS}/m1/desactivar` })
      .flush({ mensaje: 'La meta no existe' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('La meta no existe');

    // 422: ya estaba desactivada (la segunda desactivación)
    botones('desactivar')[1].click();
    http.expectOne({ method: 'PUT', url: `${URL_METAS}/m2/desactivar` })
      .flush({ mensaje: 'La meta ya está desactivada' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();
    expect(toast.toasts().some((t) => t.tipo === 'error' && t.texto.includes('ya está desactivada'))).toBe(true);

    // Las metas siguen en la lista y los botones vuelven a estar disponibles
    expect(titulos().length).toBe(2);
    expect(botones('desactivar')[0].disabled).toBe(false);
  });

  it('cambiar el umbral hace PUT y reordena la lista', () => {
    abrir([meta({ id: 'm1', comprasRequeridas: 5 }), meta({ id: 'm2', comprasRequeridas: 8 })]);

    botones('editar')[0].click();
    fixture.detectChanges();
    interno().umbralEdit.set(12);
    botones('guardar')[0].click();

    const req = http.expectOne({ method: 'PUT', url: `${URL_METAS}/m1/umbral` });
    expect(req.request.body).toEqual({ nuevoUmbral: 12 });
    req.flush(meta({ id: 'm1', comprasRequeridas: 12 }));
    fixture.detectChanges();

    expect(titulos()).toEqual(['8 compras', '12 compras']);
  });

  it('un umbral de edición no positivo no envía la petición', () => {
    abrir([meta()]);

    botones('editar')[0].click();
    fixture.detectChanges();
    interno().umbralEdit.set(0);
    botones('guardar')[0].click();

    http.expectNone({ method: 'PUT', url: `${URL_METAS}/m1/umbral` });
    expect(toast.toasts().some((t) => t.tipo === 'error')).toBe(true);
  });

  it('si falla la carga muestra el error y permite reintentar', () => {
    TestBed.configureTestingModule({
      imports: [MetasVendedor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion: () => ({ id: VENDEDOR }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MetasVendedor);
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: URL_METAS })
      .flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });
    http.match((r) => r.url.startsWith(`${API_URL}/productos`)).forEach((r) => r.flush({ contenido: [] }));
    fixture.detectChanges();

    expect(raiz().textContent).toContain('Error del servidor');
    expect(raiz().textContent).toContain('Reintentar');
  });
});
