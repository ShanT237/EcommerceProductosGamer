import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Preventa, Producto } from '../../modelos/modelos';
import { TarjetaProducto } from './tarjeta-producto';


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


describe('TarjetaProducto (descuento)', () => {
  let fixture: ComponentFixture<TarjetaProducto>;
  const raiz = () => fixture.nativeElement as HTMLElement;

  function mostrar(p: Producto) {
    fixture = TestBed.createComponent(TarjetaProducto);
    fixture.componentRef.setInput('producto', p);
    fixture.detectChanges();
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [TarjetaProducto] }));

  it('conDescuentoMuestraLaInsigniaYElPrecioOriginalTachado', () => {
    mostrar(producto({ precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20 }));

    expect(raiz().querySelector('.etiqueta.descuento')?.textContent).toContain('-20%');
    expect(raiz().querySelector('s.precio-original')?.textContent).toContain('100.000');
    expect(raiz().querySelector('.precio')?.textContent).toContain('80.000');
  });

  it('conPromocionMuestraElPrecioFinalLaBaseTachadaYLaFechaDeFin', () => {
    mostrar(producto({ precioPromocional: 85000, porcentajePromocion: 15, promocionHasta: '2026-11-30' }));

    expect(raiz().querySelector('.etiqueta.promo')?.textContent).toContain('-15%');
    expect(raiz().querySelector('.precio')?.textContent).toContain('85.000');
    expect(raiz().querySelector('s.precio-original')?.textContent).toContain('100.000');
    expect(raiz().querySelector('.promo-fin')?.textContent).toMatch(/30/);
  });

  it('conDescuentoYPromocionMuestraAmbasInsigniasYCobraElPromocional', () => {
    mostrar(producto({
      precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20,
      precioPromocional: 72000, porcentajePromocion: 10, promocionHasta: '2026-11-30',
    }));

    expect(raiz().querySelector('.etiqueta.descuento')?.textContent).toContain('-20%');
    expect(raiz().querySelector('.etiqueta.promo')?.textContent).toContain('-10%');
    expect(raiz().querySelector('s.precio-original')?.textContent).toContain('100.000');
    expect(raiz().querySelector('.precio')?.textContent).toContain('72.000');
  });

  it('lasInsigniasSeDiferencianComoOfertaYTemporada', () => {
    mostrar(producto({
      precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20,
      precioPromocional: 72000, porcentajePromocion: 10,
    }));

    expect(raiz().querySelector('.etiqueta.descuento')?.textContent).toContain('Oferta');
    expect(raiz().querySelector('.etiqueta.promo')?.textContent).toContain('Temporada');
  });

  it('sinPromocionNoMuestraInsigniaNiFechaDeFin', () => {
    mostrar(producto());

    expect(raiz().querySelector('.etiqueta.promo')).toBeNull();
    expect(raiz().querySelector('.promo-fin')).toBeNull();
  });

  it('sinDescuentoNoMuestraInsigniaNiPrecioOriginal', () => {
    mostrar(producto({ precioOriginal: null, porcentajeDescuento: null }));

    expect(raiz().querySelector('.etiqueta.descuento')).toBeNull();
    expect(raiz().querySelector('.precio-original')).toBeNull();
    expect(raiz().querySelector('.precio')?.textContent).toContain('100.000');
  });

  it('unProductoSinLosCamposNuevosTampocoMuestraDescuento', () => {
    mostrar(producto());

    expect(raiz().querySelector('.etiqueta.descuento')).toBeNull();
  });

  it('muestraLaInsigniaDePreventaConLaFechaDeLanzamiento', () => {
    mostrar(producto());
    fixture.componentRef.setInput('preventa', {
      id: 'pr1', productoId: 'p1', fechaLanzamiento: '2026-12-01', descripcion: 'Edición especial',
    } satisfies Preventa);
    fixture.detectChanges();

    expect(raiz().querySelector('.etiqueta.preventa')?.textContent).toContain('Preventa');
    expect(raiz().querySelector('.etiqueta.preventa')?.textContent).toMatch(/1 de dic/i);
  });

  it('sinPreventaNoMuestraLaInsignia', () => {
    mostrar(producto());

    expect(raiz().querySelector('.etiqueta.preventa')).toBeNull();
  });
});


describe('TarjetaProducto (tipo de producto)', () => {
  let fixture: ComponentFixture<TarjetaProducto>;
  const raiz = () => fixture.nativeElement as HTMLElement;

  function mostrar(p: Producto) {
    fixture = TestBed.createComponent(TarjetaProducto);
    fixture.componentRef.setInput('producto', p);
    fixture.detectChanges();
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [TarjetaProducto] }));

  it('productoDigitalMuestraLaEtiquetaDigital', () => {
    mostrar(producto({ tipo: 'DIGITAL' }));

    expect(raiz().querySelector('.etiqueta.tipo-DIGITAL')?.textContent).toContain('Digital');
    expect(raiz().querySelector('.etiqueta.tipo-FISICO')).toBeNull();
  });

  it('productoFisicoMuestraLaEtiquetaFisico', () => {
    mostrar(producto({ tipo: 'FISICO' }));

    expect(raiz().querySelector('.etiqueta.tipo-FISICO')?.textContent).toContain('Físico');
    expect(raiz().querySelector('.etiqueta.tipo-DIGITAL')).toBeNull();
  });

  it('productoSinTipoSeTrataComoFisico', () => {
    mostrar(producto());

    expect(raiz().querySelector('.etiqueta.tipo-FISICO')?.textContent).toContain('Físico');
  });
});
