import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { API_URL } from '../../config';
import { Pagina, Producto, PromocionTemporada, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
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

function promo(extra: Partial<PromocionTemporada> = {}): PromocionTemporada {
  return {
    id: 'pr1',
    productoId: 'p1',
    productoNombre: 'Mouse',
    porcentaje: 15,
    desde: '2026-12-01',
    hasta: '2026-12-31',
    ...extra,
  };
}

/** Fecha ISO local (yyyy-MM-dd) a `offset` días de hoy; sirve para promociones vigentes sin depender del día en que corra la prueba. */
const diaISO = (offset: number) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return d.toLocaleDateString('en-CA');
};

/** Signals protegidos del componente que las pruebas necesitan fijar directamente. */
type Interno = {
  promoProducto: WritableSignal<string>;
  promoPorcentaje: WritableSignal<number | null>;
  promoDesde: WritableSignal<string>;
  promoHasta: WritableSignal<string>;
  descProducto: WritableSignal<string>;
};

const conDescuento = () => producto({ precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20 });
const pagina = (contenido: Producto[]): Pagina<Producto> =>
  ({ contenido, pagina: 0, tamano: 100, totalElementos: contenido.length, totalPaginas: 1 });

/** Deja correr las promesas pendientes (la confirmación se resuelve de forma asíncrona). */
const esperar = () => new Promise<void>((r) => setTimeout(r));

describe('PanelVendedor (descuento inmediato)', () => {
  let fixture: ComponentFixture<PanelVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const catalogo = (r: { url: string }) => r.url === `${API_URL}/productos`;
  const promociones = (r: { url: string }) => r.url === `${API_URL}/promociones`;

  /** Responde catálogo y promociones; los reportes fallan en silencio (el panel ignora esos errores). */
  function responderCarga(lista: Producto[], promos: PromocionTemporada[] = []) {
    http.match(catalogo).forEach((r) => r.flush(pagina(lista)));
    http.match((r) => promociones(r) && r.method === 'GET').forEach((r) => r.flush(promos));
    http.match((r) => !catalogo(r) && !promociones(r) && r.method === 'GET')
      .forEach((r) => r.flush({ mensaje: 'sin datos' }, { status: 500, statusText: 'Server Error' }));
    fixture.detectChanges();
  }

  function abrirMisProductos(lista: Producto[], promos: PromocionTemporada[] = []) {
    fixture = TestBed.createComponent(PanelVendedor);
    fixture.detectChanges();
    responderCarga(lista, promos);
    (raiz().querySelectorAll('.pestanas button')[1] as HTMLButtonElement).click(); // "Mis productos"
    fixture.detectChanges();
  }

  function irAPromociones() {
    (raiz().querySelectorAll('.pestanas button')[2] as HTMLButtonElement).click(); // "Promociones"
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
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    http.verify();
  });

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

  it('quitarDescuentoHaceDeleteYRecargaElCatalogoSinElDescuento', async () => {
    abrirMisProductos([conDescuento()]);

    (raiz().querySelector('.quitar-descuento') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/descuentos/p1` })
      .flush({ id: 'p1', nombre: 'Mouse', nuevoPrecio: 100000, precioOriginal: null, porcentajeDescuento: null });
    responderCarga([producto()]); // tras quitar, se vuelve a pedir el catálogo

    expect(raiz().querySelector('.fila .quitar-descuento')).toBeNull();
    expect(raiz().querySelector('.fila .etiqueta.descuento')).toBeNull();
    expect(raiz().querySelector('.fila .precio')?.textContent).toContain('100.000');
    expect(toast.toasts().find((t) => t.tipo === 'ok')?.texto).toContain('Descuento retirado');
  });

  it('unErrorAlQuitarMuestraElMensajeDelBackendYConservaElDescuento', async () => {
    abrirMisProductos([conDescuento()]);

    (raiz().querySelector('.quitar-descuento') as HTMLButtonElement).click();
    await esperar();
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

    irAPromociones();

    const opciones = Array.from(raiz().querySelectorAll('#dp option')) as HTMLOptionElement[];
    expect(opciones.find((o) => o.textContent?.includes('Mouse'))?.disabled).toBe(true);
    expect(opciones.find((o) => o.textContent?.includes('Teclado'))?.disabled).toBe(false);
    expect(raiz().querySelector('.activos')?.textContent).toContain('Mouse');
  });
});

describe('PanelVendedor (promociones de temporada)', () => {
  let fixture: ComponentFixture<PanelVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const interno = () => fixture.componentInstance as unknown as Interno;
  const catalogo = (r: { url: string }) => r.url === `${API_URL}/productos`;
  const promociones = (r: { url: string }) => r.url === `${API_URL}/promociones`;

  function responderCarga(lista: Producto[], promos: PromocionTemporada[] = []) {
    http.match(catalogo).forEach((r) => r.flush(pagina(lista)));
    http.match((r) => promociones(r) && r.method === 'GET').forEach((r) => r.flush(promos));
    http.match((r) => !catalogo(r) && !promociones(r) && r.method === 'GET')
      .forEach((r) => r.flush({ mensaje: 'sin datos' }, { status: 500, statusText: 'Server Error' }));
    fixture.detectChanges();
  }

  function abrirMisProductos(lista: Producto[], promos: PromocionTemporada[] = []) {
    fixture = TestBed.createComponent(PanelVendedor);
    fixture.detectChanges();
    responderCarga(lista, promos);
    (raiz().querySelectorAll('.pestanas button')[1] as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  function irAPromociones() {
    (raiz().querySelectorAll('.pestanas button')[2] as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  function enviarFormularioPromocion() {
    (raiz().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
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
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    http.verify();
  });

  // ---- hallazgo 2: el panel debe restar la promoción ----

  it('laFilaMuestraElPrecioConLaPromocionRestada', () => {
    abrirMisProductos([producto({ precioPromocional: 85000, porcentajePromocion: 15 })]);

    expect(raiz().querySelector('.fila .precio')?.textContent).toContain('85.000');
    expect(raiz().querySelector('.fila .precio-original')?.textContent).toContain('100.000');
    expect(raiz().querySelector('.fila .etiqueta.promo')?.textContent).toContain('15%');
  });

  it('laFilaMuestraQuitarTemporadaYElimina​LaPromocionAplicada', async () => {
    abrirMisProductos(
      [producto({ precioPromocional: 85000, porcentajePromocion: 15, promocionHasta: '2026-12-31' })],
      [promo()],
    );

    (raiz().querySelector('.fila .quitar-promocion') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/promociones/pr1` })
      .flush(null, { status: 204, statusText: 'No Content' });
    responderCarga([producto()], []);

    expect(raiz().querySelector('.fila .quitar-promocion')).toBeNull();
  });

  it('sinPromocionAplicadaLaFilaNoMuestraQuitarTemporada', () => {
    abrirMisProductos([producto()], [promo()]);

    expect(raiz().querySelector('.fila .quitar-promocion')).toBeNull();
  });
  it('laFilaMuestraElPrecioFinalCuandoHayOfertaYPromocion', () => {
    abrirMisProductos([producto({
      precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20,
      precioPromocional: 72000, porcentajePromocion: 10,
    })]);

    expect(raiz().querySelector('.fila .precio')?.textContent).toContain('72.000');
    expect(raiz().querySelector('.fila .precio-original')?.textContent).toContain('100.000');
  });

  it('laOfertaYLaPromocionSeDiferencianConNombreYTotalAcumulado', () => {
    abrirMisProductos([producto({
      precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20,
      precioPromocional: 72000, porcentajePromocion: 10,
    })]);

    expect(raiz().querySelector('.fila .etiqueta.descuento')?.textContent).toContain('Oferta');
    expect(raiz().querySelector('.fila .etiqueta.promo')?.textContent).toContain('Temporada');
    expect(raiz().querySelector('.fila .desglose')?.textContent).toContain('28%');
  });

  // ---- hallazgo 1: listar, modificar y quitar ----

  it('listaLasPromocionesConfiguradasConSuEstado', () => {
    abrirMisProductos(
      [producto()],
      [promo({ id: 'pr1', desde: diaISO(-1), hasta: diaISO(5) }), promo({ id: 'pr2', desde: diaISO(-30), hasta: diaISO(-20) })],
    );

    irAPromociones();

    const filas = Array.from(raiz().querySelectorAll('.activos .activo'));
    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('Mouse');
    expect(filas[0].querySelector('.estado.vigente')).not.toBeNull();
    expect(filas[1].querySelector('.estado.vencida')).not.toBeNull();
  });

  it('sinPromocionesNoMuestraLaLista', () => {
    abrirMisProductos([producto()]);

    irAPromociones();

    expect(raiz().querySelector('.activos .estado')).toBeNull();
  });

  it('configurarUnaPromocionNuevaHacePostYRecargaLaLista', () => {
    abrirMisProductos([producto()]);
    irAPromociones();
    interno().promoProducto.set('p1');
    interno().promoPorcentaje.set(20);
    interno().promoDesde.set('2026-12-01');
    interno().promoHasta.set('2026-12-31');

    enviarFormularioPromocion();
    http.expectOne({ method: 'POST', url: `${API_URL}/promociones` }).flush(
      { id: 'pr1', productoId: 'p1', porcentaje: 20, desde: '2026-12-01', hasta: '2026-12-31' },
    );
    responderCarga([producto()], [promo({ porcentaje: 20 })]);

    expect(toast.toasts().find((t) => t.tipo === 'ok')?.texto).toContain('configurada');
    expect(raiz().querySelector('.activos')?.textContent).toContain('-20%');
  });

  it('editarPromocionCambiaElFormularioAModoEdicion', () => {
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();

    (raiz().querySelector('.activos .acciones .btn') as HTMLButtonElement).click(); // "Editar"
    fixture.detectChanges();

    expect(raiz().querySelector('form h3')?.textContent).toContain('Editar promoción de temporada');
    expect(raiz().querySelector('form button[type="submit"]')?.textContent).toContain('Guardar cambios');
    expect(Array.from(raiz().querySelectorAll('form .botones .btn')).some((b) => b.textContent?.includes('Cancelar'))).toBe(true);
  });

  it('guardarLaEdicionHacePutConLosValoresDeLaPromocionYRecargaLaLista', () => {
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();
    (raiz().querySelector('.activos .acciones .btn') as HTMLButtonElement).click();
    interno().promoPorcentaje.set(30);
    fixture.detectChanges();

    enviarFormularioPromocion();
    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/promociones/pr1` });
    expect(req.request.body).toEqual({ porcentaje: 30, desde: '2026-12-01', hasta: '2026-12-31' });
    req.flush({ id: 'pr1', productoId: 'p1', porcentaje: 30, desde: '2026-12-01', hasta: '2026-12-31' });
    responderCarga([producto()], [promo({ porcentaje: 30 })]);

    expect(toast.toasts().find((t) => t.tipo === 'ok')?.texto).toContain('actualizada');
    expect(raiz().querySelector('form h3')?.textContent).not.toContain('Editar');
    expect(raiz().querySelector('.activos')?.textContent).toContain('-30%');
  });

  it('cancelarLaEdicionVuelveAlModoCrearSinLlamarAlBackend', () => {
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();
    (raiz().querySelector('.activos .acciones .btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    const cancelar = Array.from(raiz().querySelectorAll('form .botones .btn'))
      .find((b) => b.textContent?.includes('Cancelar')) as HTMLButtonElement;
    cancelar.click();
    fixture.detectChanges();

    expect(raiz().querySelector('form h3')?.textContent).not.toContain('Editar');
    http.expectNone((r) => r.method === 'PUT');
  });

  it('unErrorDeSolapamientoAlEditarMuestraElMensajeDelBackend', () => {
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();
    (raiz().querySelector('.activos .acciones .btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    enviarFormularioPromocion();
    http.expectOne({ method: 'PUT', url: `${API_URL}/promociones/pr1` }).flush(
      { mensaje: 'El producto ya tiene una promoción en ese rango de fechas' },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(raiz().querySelector('form .error')?.textContent).toContain('ya tiene una promoción');
  });

  it('quitarLaPromocionHaceDeleteYRecargaLaListaYElCatalogo', async () => {
    abrirMisProductos([producto({ precioPromocional: 85000, porcentajePromocion: 15 })], [promo()]);
    irAPromociones();

    (raiz().querySelector('.activos .acciones .btn-peligro') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/promociones/pr1` }).flush(null, { status: 204, statusText: 'No Content' });
    responderCarga([producto()], []); // tras quitar se piden otra vez el catálogo y la lista

    expect(toast.toasts().find((t) => t.tipo === 'ok')?.texto).toContain('Promoción retirada');
    expect(raiz().querySelector('.activos .estado')).toBeNull();
  });

  it('siElVendedorNoConfirmaNoSeQuitaLaPromocion', async () => {
    vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(false);
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();

    (raiz().querySelector('.activos .acciones .btn-peligro') as HTMLButtonElement).click();
    await esperar();

    http.expectNone((r) => r.method === 'DELETE');
  });

  it('unErrorAlQuitarLaPromocionMuestraElMensajeYLaConserva', async () => {
    abrirMisProductos([producto()], [promo()]);
    irAPromociones();

    (raiz().querySelector('.activos .acciones .btn-peligro') as HTMLButtonElement).click();
    await esperar();
    http.expectOne({ method: 'DELETE', url: `${API_URL}/promociones/pr1` }).flush(
      { mensaje: 'La promoción no existe' },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('no existe');
    expect(raiz().querySelectorAll('.activos .activo').length).toBe(1);
  });

  // ---- hallazgo 3: avisos al acumular ----

  it('avisaAlPonerUnaPromocionSobreUnProductoConOfertaInmediata', () => {
    abrirMisProductos([conDescuento()]);
    irAPromociones();

    interno().promoProducto.set('p1');
    fixture.detectChanges();

    expect(raiz().querySelector('.aviso')?.textContent).toContain('oferta inmediata');
  });

  it('noAvisaSiElProductoNoTieneOfertaInmediata', () => {
    abrirMisProductos([producto()]);
    irAPromociones();

    interno().promoProducto.set('p1');
    fixture.detectChanges();

    expect(raiz().querySelector('.aviso')).toBeNull();
  });

  it('avisaAlAplicarUnaOfertaSobreUnProductoConPromocionVigente', () => {
    abrirMisProductos([producto()], [promo({ desde: diaISO(-1), hasta: diaISO(5) })]);
    irAPromociones();

    interno().descProducto.set('p1');
    fixture.detectChanges();

    expect(raiz().querySelector('.aviso')?.textContent).toContain('promoción de temporada');
  });

  it('noAvisaSiLaPromocionYaVencio', () => {
    abrirMisProductos([producto()], [promo({ desde: diaISO(-30), hasta: diaISO(-20) })]);
    irAPromociones();

    interno().descProducto.set('p1');
    fixture.detectChanges();

    expect(raiz().querySelector('.aviso')).toBeNull();
  });
});