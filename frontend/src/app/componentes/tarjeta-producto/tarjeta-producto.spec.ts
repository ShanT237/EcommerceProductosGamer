import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Producto } from '../../modelos/modelos';
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
});
