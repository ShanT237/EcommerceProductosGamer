import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Combo, Producto, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ProductoService } from '../../servicios/producto.service';
import { ToastService } from '../../servicios/toast.service';
import { CombosVendedor } from './combos-vendedor';

function producto(id: string, nombre: string, extra: Partial<Producto> = {}): Producto {
  return {
    id, vendedorId: 'v1', nombre, descripcion: 'Desc', precio: 100000, stock: 10,
    gama: 'MEDIA', exclusividad: 'NORMAL', categoria: null, ...extra,
  };
}

function combo(productos: Array<{ id: string; nombre: string }>): Combo {
  return {
    id: 'c1',
    vendedorId: 'v1',
    nombre: 'Combo Gamer',
    descripcion: 'Setup completo',
    descuento: 15,
    productos: productos.map((p) => ({ ...p, precio: 100000, stock: 10 })),
    precioSinDescuento: productos.length * 100000,
    precioConDescuento: productos.length * 85000,
  };
}

/** Signals protegidos del componente que las pruebas fijan directamente. */
type Interno = {
  nombre: WritableSignal<string>;
  descripcion: WritableSignal<string>;
  descuento: WritableSignal<number | null>;
};

describe('CombosVendedor', () => {
  let fixture: ComponentFixture<CombosVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const getMios = () => ({ method: 'GET', url: `${API_URL}/combos/mios` });
  const raiz = () => fixture.nativeElement as HTMLElement;
  const interno = () => fixture.componentInstance as unknown as Interno;
  const marcar = (i: number) => (raiz().querySelectorAll('.opcion input')[i] as HTMLInputElement).click();
  const enviar = () => (raiz().querySelector('button.crear') as HTMLButtonElement).click();

  function abrir(mios: Combo[] = []) {
    const sesion = signal<Sesion | null>({ id: 'v1', correo: 'tienda@correo.com', rol: 'VENDEDOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [CombosVendedor],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { sesion } }],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    TestBed.inject(ProductoService).catalogo.set([
      producto('p1', 'Teclado'),
      producto('p2', 'Mouse'),
      producto('p3', 'Headset'),
      producto('ajeno', 'De otro', { vendedorId: 'v2' }),
    ]);
    fixture = TestBed.createComponent(CombosVendedor);
    fixture.detectChanges();
    http.expectOne(getMios()).flush(mios);
    fixture.detectChanges();
  }

  function llenar() {
    interno().nombre.set('Combo Gamer');
    interno().descripcion.set('Setup completo');
    interno().descuento.set(15);
  }

  afterEach(() => http.verify());

  it('soloOfreceLosProductosPropiosParaElCombo', () => {
    abrir();

    const nombres = Array.from(raiz().querySelectorAll('.opcion')).map((o) => o.textContent);
    expect(nombres.length).toBe(3);
    expect(nombres.join(' ')).not.toContain('De otro');
  });

  it('conMenosDeDosProductosNoEnviaPeticion', () => {
    abrir();
    llenar();
    marcar(0);

    enviar();
    fixture.detectChanges();

    http.expectNone({ method: 'POST', url: `${API_URL}/combos` });
    expect(raiz().querySelector('.error')?.textContent).toContain('al menos dos productos');
  });

  it('unDescuentoFueraDeRangoNoEnviaPeticion', () => {
    abrir();
    llenar();
    interno().descuento.set(100);
    marcar(0);
    marcar(1);

    enviar();
    fixture.detectChanges();

    http.expectNone({ method: 'POST', url: `${API_URL}/combos` });
    expect(raiz().querySelector('.error')?.textContent).toContain('menor que 100');
  });

  it('creaElComboYLoMuestra', () => {
    abrir();
    llenar();
    marcar(0);
    marcar(1);

    enviar();
    const post = http.expectOne({ method: 'POST', url: `${API_URL}/combos` });
    expect(post.request.body).toEqual({
      nombre: 'Combo Gamer', descripcion: 'Setup completo', descuento: 15, productoIds: ['p1', 'p2'],
    });
    post.flush({ id: 'c1', vendedorId: 'v1', nombre: 'Combo Gamer', descripcion: 'Setup completo', descuento: 15, productos: ['p1', 'p2'] });
    http.expectOne(getMios()).flush([combo([{ id: 'p1', nombre: 'Teclado' }, { id: 'p2', nombre: 'Mouse' }])]);
    fixture.detectChanges();

    expect(raiz().querySelector('.mios')?.textContent).toContain('Combo Gamer');
    expect(raiz().querySelectorAll('.mios .productos li').length).toBe(2);
    expect(interno().nombre()).toBe('');
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
  });

  it('unErrorDelBackendAlCrearSeMuestraEnElFormulario', () => {
    abrir();
    llenar();
    marcar(0);
    marcar(1);

    enviar();
    http.expectOne({ method: 'POST', url: `${API_URL}/combos` }).flush(
      { mensaje: 'El producto no pertenece al vendedor' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(raiz().querySelector('.error')?.textContent).toContain('no pertenece al vendedor');
  });

  it('añadirUnProductoHacePutYRecargaLosCombos', () => {
    abrir([combo([{ id: 'p1', nombre: 'Teclado' }, { id: 'p2', nombre: 'Mouse' }])]);

    const select = raiz().querySelector('.anadir select') as HTMLSelectElement;
    expect(select.options.length).toBe(2); // placeholder + Headset (los otros ya están en el combo)
    select.value = 'p3';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    (raiz().querySelector('button.agregar') as HTMLButtonElement).click();

    const put = http.expectOne({ method: 'PUT', url: `${API_URL}/combos/c1/productos/p3` });
    put.flush({});
    http.expectOne(getMios()).flush([
      combo([{ id: 'p1', nombre: 'Teclado' }, { id: 'p2', nombre: 'Mouse' }, { id: 'p3', nombre: 'Headset' }]),
    ]);
    fixture.detectChanges();

    expect(raiz().querySelectorAll('.mios .productos li').length).toBe(3);
  });

  it('quitarBajoElMinimoMuestraElMensajeDelBackend', () => {
    abrir([combo([{ id: 'p1', nombre: 'Teclado' }, { id: 'p2', nombre: 'Mouse' }])]);

    (raiz().querySelector('button.quitar') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/combos/c1/productos/p1` }).flush(
      { mensaje: 'Un combo debe tener al menos dos productos' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('al menos dos productos');
    expect(raiz().querySelectorAll('.mios .productos li').length).toBe(2);
  });

  it('quitarUnProductoRecargaLaLista', () => {
    abrir([combo([{ id: 'p1', nombre: 'Teclado' }, { id: 'p2', nombre: 'Mouse' }, { id: 'p3', nombre: 'Headset' }])]);

    (raiz().querySelector('button.quitar') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/combos/c1/productos/p1` }).flush({});
    http.expectOne(getMios()).flush([combo([{ id: 'p2', nombre: 'Mouse' }, { id: 'p3', nombre: 'Headset' }])]);
    fixture.detectChanges();

    expect(raiz().querySelectorAll('.mios .productos li').length).toBe(2);
  });

  it('sinCombosMuestraMensajeVacio', () => {
    abrir([]);

    expect(raiz().querySelector('.mios .vacio')?.textContent).toContain('Aún no has creado combos');
  });
});
