import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Pagina, Producto, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ToastService } from '../../servicios/toast.service';
import { PanelVendedor } from './panel-vendedor';

function producto(extra: Partial<Producto> = {}): Producto {
  return {
    id: 'p1',
    vendedorId: 'v1',
    nombre: 'Mouse',
    descripcion: 'Mouse RGB',
    precio: 100000,
    stock: 10,
    gama: 'MEDIA',
    exclusividad: 'NORMAL',
    categoria: null,
    ...extra,
  };
}

const conDescuento = () => producto({ precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20 });
const pagina = (contenido: Producto[]): Pagina<Producto> =>
  ({ contenido, pagina: 0, tamano: 100, totalElementos: contenido.length, totalPaginas: 1 });

describe('PanelVendedor (descuento inmediato)', () => {
  let fixture: ComponentFixture<PanelVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const catalogo = (r: { url: string }) => r.url === `${API_URL}/productos`;

  /** Responde el catálogo y deja que fallen en silencio las llamadas de reportes (el panel ignora esos errores). */
  function responderCarga(lista: Producto[]) {
    http.match(catalogo).forEach((r) => r.flush(pagina(lista)));
    http.match((r) => !catalogo(r) && r.method === 'GET')
      .forEach((r) => r.flush({ mensaje: 'sin datos' }, { status: 500, statusText: 'Server Error' }));
    fixture.detectChanges();
  }

  function abrirMisProductos(lista: Producto[]) {
    fixture = TestBed.createComponent(PanelVendedor);
    fixture.detectChanges();
    responderCarga(lista);
    (raiz().querySelectorAll('.pestanas button')[1] as HTMLButtonElement).click(); // "Mis productos"
    fixture.detectChanges();
  }

  beforeEach(() => {
    const sesion = signal<Sesion | null>({ id: 'v1', correo: 'v@correo.com', rol: 'VENDEDOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [PanelVendedor],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { sesion } }],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => http.verify());

  it('muestraElPrecioOriginalElPorcentajeYElBotonQuitarEnMisProductos', () => {
    abrirMisProductos([conDescuento()]);

    expect(raiz().querySelector('.fila .precio-original')?.textContent).toContain('100.000');
    expect(raiz().querySelector('.fila .etiqueta.descuento')?.textContent).toContain('-20%');
    expect(raiz().querySelector('.fila .quitar-descuento')).not.toBeNull();
  });

  it('unProductoSinDescuentoNoMuestraElBotonQuitar', () => {
    abrirMisProductos([producto()]);

    expect(raiz().querySelector('.fila .quitar-descuento')).toBeNull();
    expect(raiz().querySelector('.fila .etiqueta.descuento')).toBeNull();
  });

  it('quitarDescuentoHaceDeleteYRecargaElCatalogoSinElDescuento', () => {
    abrirMisProductos([conDescuento()]);

    (raiz().querySelector('.quitar-descuento') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/descuentos/p1` })
      .flush({ id: 'p1', nombre: 'Mouse', nuevoPrecio: 100000, precioOriginal: null, porcentajeDescuento: null });
    responderCarga([producto()]); // tras quitar, se vuelve a pedir el catálogo

    expect(raiz().querySelector('.fila .quitar-descuento')).toBeNull();
    expect(raiz().querySelector('.fila .etiqueta.descuento')).toBeNull();
    expect(raiz().querySelector('.fila .precio')?.textContent).toContain('100.000');
    expect(toast.toasts().find((t) => t.tipo === 'ok')?.texto).toContain('Descuento retirado');
  });

  it('unErrorAlQuitarMuestraElMensajeDelBackendYConservaElDescuento', () => {
    abrirMisProductos([conDescuento()]);

    (raiz().querySelector('.quitar-descuento') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/descuentos/p1` }).flush(
      { mensaje: 'El producto no tiene un descuento activo' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('no tiene un descuento activo');
    expect(raiz().querySelector('.fila .quitar-descuento')).not.toBeNull();
  });

  it('conDescuentoActivoElPrecioNoSeEditaEnElModal', () => {
    abrirMisProductos([conDescuento()]);

    (raiz().querySelector('.fila .botones .btn') as HTMLButtonElement).click(); // "Editar"
    fixture.detectChanges();

    expect((raiz().querySelector('#ep') as HTMLInputElement).disabled).toBe(true);
    expect(raiz().textContent).toContain('quítalo para poder cambiar el precio');
  });

  it('sinDescuentoElPrecioSiSeEditaEnElModal', () => {
    abrirMisProductos([producto()]);

    (raiz().querySelector('.fila .botones .btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect((raiz().querySelector('#ep') as HTMLInputElement).disabled).toBe(false);
  });

  it('elSelectDeDescuentoInmediatoDeshabilitaLosProductosQueYaTienenDescuento', () => {
    abrirMisProductos([conDescuento(), producto({ id: 'p2', nombre: 'Teclado' })]);

    (raiz().querySelectorAll('.pestanas button')[2] as HTMLButtonElement).click(); // "Promociones"
    fixture.detectChanges();

    const opciones = Array.from(raiz().querySelectorAll('#dp option')) as HTMLOptionElement[];
    expect(opciones.find((o) => o.textContent?.includes('Mouse'))?.disabled).toBe(true);
    expect(opciones.find((o) => o.textContent?.includes('Teclado'))?.disabled).toBe(false);
    expect(raiz().querySelector('.activos')?.textContent).toContain('Mouse');
  });
});
