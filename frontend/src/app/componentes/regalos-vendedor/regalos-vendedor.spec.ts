import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Combo, Producto, Regalo } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ToastService } from '../../servicios/toast.service';
import { RegalosVendedor } from './regalos-vendedor';

const VENDEDOR = 'v1';

/** Deja correr las promesas pendientes (la confirmación se resuelve de forma asíncrona). */
const esperar = () => new Promise<void>((r) => setTimeout(r));

function producto(extra: Partial<Producto> = {}): Producto {
  return {
    id: 'p1', vendedorId: VENDEDOR, nombre: 'Mouse gamer', descripcion: 'Desc', precio: 100, stock: 5,
    gama: 'MEDIA', exclusividad: 'NORMAL', categoria: null, ...extra,
  };
}

function combo(extra: Partial<Combo> = {}): Combo {
  return {
    id: 'c1', vendedorId: VENDEDOR, nombre: 'Combo setup', descripcion: 'Desc', descuento: 10,
    productos: [], precioSinDescuento: 200, precioConDescuento: 180, ...extra,
  };
}

function regalo(extra: Partial<Regalo> = {}): Regalo {
  return {
    id: 'r1', estado: 'ELEGIDO', idProducto: 'p1', idCombo: null, idUsuario: 'u1',
    notificado: false, fechaCreacion: '2026-11-15T10:00:00', fechaEntrega: null, ...extra,
  };
}

/** Signals protegidos del componente que las pruebas fijan directamente. */
type Interno = {
  tipo: WritableSignal<'producto' | 'combo'>;
  productoId: WritableSignal<string>;
  comboId: WritableSignal<string>;
};

describe('RegalosVendedor', () => {
  let fixture: ComponentFixture<RegalosVendedor>;
  let http: HttpTestingController;
  let toast: ToastService;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const interno = () => fixture.componentInstance as unknown as Interno;
  const botones = (clase: string) => Array.from(raiz().querySelectorAll<HTMLButtonElement>(`button.${clase}`));
  const textoError = () => raiz().querySelector('.error')?.textContent ?? '';

  function abrir(porEnviar: Regalo[] = [], combos: Combo[] = []) {
    TestBed.configureTestingModule({
      imports: [RegalosVendedor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion: () => ({ id: VENDEDOR }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(RegalosVendedor);
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: `${API_URL}/regalos/por-enviar` }).flush(porEnviar);
    http.match((r) => r.url.startsWith(`${API_URL}/productos`))
      .forEach((r) => r.flush({ contenido: [producto(), producto({ id: 'p2', vendedorId: 'otro', nombre: 'Ajeno' })] }));
    http.expectOne({ method: 'GET', url: `${API_URL}/combos/mios` }).flush(combos);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('crearConProductoOComboHacePost', () => {
    abrir([], [combo()]);

    // Con producto: el POST lleva solo productoId
    interno().tipo.set('producto');
    interno().productoId.set('p1');
    fixture.detectChanges();
    botones('crear')[0].click();
    const conProducto = http.expectOne({ method: 'POST', url: `${API_URL}/regalos` });
    expect(conProducto.request.body).toEqual({ productoId: 'p1' });
    conProducto.flush(regalo({ estado: 'DISPONIBLE', idUsuario: null }));
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);

    // Con combo: el POST lleva solo comboId
    interno().tipo.set('combo');
    interno().comboId.set('c1');
    fixture.detectChanges();
    botones('crear')[0].click();
    const conCombo = http.expectOne({ method: 'POST', url: `${API_URL}/regalos` });
    expect(conCombo.request.body).toEqual({ comboId: 'c1' });
    conCombo.flush(regalo({ estado: 'DISPONIBLE', idProducto: null, idCombo: 'c1', idUsuario: null }));
  });

  it('sin elegir producto o combo no envía la petición', () => {
    abrir();

    botones('crear')[0].click();
    fixture.detectChanges();

    http.expectNone({ method: 'POST', url: `${API_URL}/regalos` });
    expect(textoError()).toContain('Elige un producto');
  });

  it('solo ofrece los productos del propio vendedor', () => {
    abrir();

    const opciones = Array.from(raiz().querySelectorAll('#rg-producto option')).map((o) => o.textContent?.trim());
    expect(opciones).toContain('Mouse gamer');
    expect(opciones).not.toContain('Ajeno');
  });

  it('un error del backend al crear se muestra en el formulario', () => {
    abrir();
    interno().productoId.set('p1');

    botones('crear')[0].click();
    http.expectOne({ method: 'POST', url: `${API_URL}/regalos` })
      .flush({ mensaje: 'El producto no pertenece al vendedor' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    expect(textoError()).toContain('no pertenece al vendedor');
  });

  it('enviarHacePutYSacaElRegaloDeLaLista', () => {
    abrir([regalo({ id: 'r1' }), regalo({ id: 'r2' })]);
    expect(botones('enviar').length).toBe(2);

    botones('enviar')[0].click();
    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/enviar` }).flush(regalo({ id: 'r1', estado: 'ENVIADO' }));
    fixture.detectChanges();

    expect(botones('enviar').length).toBe(1);
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
  });

  it('si falla el envío el regalo sigue en la lista y se avisa el motivo', () => {
    abrir([regalo({ id: 'r1' })]);

    botones('enviar')[0].click();
    http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/enviar` })
      .flush({ mensaje: 'El regalo no pertenece al vendedor' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    expect(botones('enviar').length).toBe(1);
    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('no pertenece al vendedor');
  });

  describe('cancelar', () => {
    const articulos = () => raiz().querySelectorAll('article.regalo');

    function abrirConConfirmacion(acepta: boolean) {
      abrir([regalo()]);
      return vi.spyOn(TestBed.inject(ConfirmacionService), 'pedir').mockResolvedValue(acepta);
    }

    it('cancelar confirma, hace PUT /regalos/{id}/cancelar y quita el regalo de la lista', async () => {
      const pedir = abrirConConfirmacion(true);

      botones('cancelar')[0].click();
      await esperar();

      expect(pedir.mock.calls[0][0].mensaje).toContain('El comprador será avisado y recuperará su cupo');
      http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/cancelar` }).flush(regalo({ estado: 'CANCELADO' }));
      fixture.detectChanges();

      expect(articulos()).toHaveLength(0);
      expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
    });

    it('si no se confirma no se llama al backend', async () => {
      abrirConConfirmacion(false);

      botones('cancelar')[0].click();
      await esperar();

      http.expectNone(`${API_URL}/regalos/r1/cancelar`);
      expect(articulos()).toHaveLength(1);
    });

    it('un error del backend se muestra en un toast y el regalo sigue en la lista', async () => {
      abrirConConfirmacion(true);

      botones('cancelar')[0].click();
      await esperar();
      http.expectOne({ method: 'PUT', url: `${API_URL}/regalos/r1/cancelar` })
        .flush({ mensaje: 'Un regalo entregado no puede cambiar de estado' }, { status: 422, statusText: 'Unprocessable Content' });
      fixture.detectChanges();

      expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('entregado');
      expect(articulos()).toHaveLength(1);
    });
  });
});
