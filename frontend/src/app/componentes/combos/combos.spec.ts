import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { API_URL } from '../../config';
import { Combo, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ToastService } from '../../servicios/toast.service';
import { Combos } from './combos';

function combo(extra: Partial<Combo> = {}): Combo {
  return {
    id: 'c1',
    vendedorId: 'v1',
    nombre: 'Combo Gamer',
    descripcion: 'Teclado + Mouse',
    descuento: 20,
    productos: [
      { id: 'p1', nombre: 'Teclado', precio: 100000, stock: 5 },
      { id: 'p2', nombre: 'Mouse', precio: 50000, stock: 3 },
    ],
    precioSinDescuento: 150000,
    precioConDescuento: 120000,
    ...extra,
  };
}

describe('Combos (pantalla)', () => {
  let fixture: ComponentFixture<Combos>;
  let http: HttpTestingController;
  let toast: ToastService;
  let router: Router;

  const getLista = () => ({ method: 'GET', url: `${API_URL}/combos` });
  const postComprar = (id: string) => ({ method: 'POST', url: `${API_URL}/combos/${id}/comprar` });
  const raiz = () => fixture.nativeElement as HTMLElement;
  const boton = (selector: string) => raiz().querySelector(selector) as HTMLButtonElement | null;

  function configurar(rol: Sesion['rol']) {
    const sesion = signal<Sesion | null>({ id: 'u1', correo: 'jose@correo.com', rol, exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [Combos],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  }

  function abrir(lista: Combo[]) {
    fixture = TestBed.createComponent(Combos);
    fixture.detectChanges();
    http.expectOne(getLista()).flush(lista);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('muestraLosProductosYLosPreciosConYSinDescuento', () => {
    configurar('COMPRADOR');
    abrir([combo()]);

    const texto = raiz().textContent ?? '';
    expect(texto).toContain('Combo Gamer');
    expect(texto).toContain('Teclado');
    expect(texto).toContain('Mouse');
    expect(texto).toContain('-20%');
    expect(raiz().querySelector('.precio-original')).not.toBeNull();
    expect(raiz().querySelector('.ahorro')?.textContent).toContain('Ahorras');
  });

  it('sinCombosMuestraMensajeVacio', () => {
    configurar('COMPRADOR');
    abrir([]);

    expect(raiz().querySelector('.vacio')?.textContent).toContain('Todavía no hay combos');
  });

  it('comprarEnviaPostConLaCantidadYNavegaAMisCompras', () => {
    configurar('COMPRADOR');
    abrir([combo()]);

    boton('.mas')!.click();
    fixture.detectChanges();
    boton('.comprar')!.click();

    const req = http.expectOne(postComprar('c1'));
    expect(req.request.body).toEqual({ cantidad: 2 });
    req.flush({ comboId: 'c1', compras: [{}, {}], total: 240000 });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/mis-compras');
    expect(toast.toasts().some((t) => t.tipo === 'ok')).toBe(true);
  });

  it('laCantidadNoSuperaElStockDelProductoConMenosUnidades', () => {
    configurar('COMPRADOR');
    abrir([combo()]); // el Mouse solo tiene 3

    for (let i = 0; i < 5; i++) {
      boton('.mas')!.click();
      fixture.detectChanges();
    }
    boton('.comprar')!.click();

    const req = http.expectOne(postComprar('c1'));
    expect(req.request.body).toEqual({ cantidad: 3 });
    req.flush({ comboId: 'c1', compras: [], total: 0 });
  });

  it('un422MuestraElMensajeDelBackendYNoNavega', () => {
    configurar('COMPRADOR');
    abrir([combo()]);

    boton('.comprar')!.click();
    http.expectOne(postComprar('c1')).flush(
      { mensaje: 'No hay suficiente stock disponible' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    fixture.detectChanges();

    expect(toast.toasts().find((t) => t.tipo === 'error')?.texto).toContain('No hay suficiente stock');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(boton('.comprar')!.disabled).toBe(false);
  });

  it('unComboSinStockNoSePuedeComprar', () => {
    configurar('COMPRADOR');
    abrir([combo({ productos: [{ id: 'p1', nombre: 'Teclado', precio: 100000, stock: 0 }, { id: 'p2', nombre: 'Mouse', precio: 50000, stock: 3 }] })]);

    expect(boton('.comprar')).toBeNull();
    expect(raiz().textContent).toContain('Agotado');
  });

  it('unVendedorNoVeElBotonComprar', () => {
    configurar('VENDEDOR');
    abrir([combo()]);

    expect(boton('.comprar')).toBeNull();
    expect(raiz().textContent).toContain('Combo Gamer');
  });

  it('unErrorAlCargarMuestraElMensajeYPermiteReintentar', () => {
    configurar('COMPRADOR');
    fixture = TestBed.createComponent(Combos);
    fixture.detectChanges();
    http.expectOne(getLista()).flush({ mensaje: 'Fallo' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(raiz().querySelector('.vacio')?.textContent).toContain('Fallo');

    (raiz().querySelector('.vacio button') as HTMLButtonElement).click();
    http.expectOne(getLista()).flush([combo()]);
    fixture.detectChanges();

    expect(raiz().textContent).toContain('Combo Gamer');
  });
});
