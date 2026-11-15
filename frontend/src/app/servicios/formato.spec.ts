import { Producto } from '../modelos/modelos';
import { fechaDia, precioTachado, precioVigente } from './formato';

function producto(extra: Partial<Producto> = {}): Producto {
  return {
    id: 'p1', vendedorId: 'v1', nombre: 'Mouse', descripcion: 'Mouse RGB', precio: 100000, stock: 10,
    gama: 'MEDIA', exclusividad: 'NORMAL', categoria: null, ...extra,
  };
}

describe('formato (precio vigente)', () => {
  it('sinRebajasElPrecioVigenteEsElPrecioYNadaSeTacha', () => {
    const p = producto();
    expect(precioVigente(p)).toBe(100000);
    expect(precioTachado(p)).toBeNull();
  });

  it('conDescuentoInmediatoSeTachaElPrecioOriginal', () => {
    const p = producto({ precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20 });
    expect(precioVigente(p)).toBe(80000);
    expect(precioTachado(p)).toBe(100000);
  });

  it('conPromocionElPrecioVigenteEsElPromocionalYSeTachaElActual', () => {
    const p = producto({ precioPromocional: 85000, porcentajePromocion: 15, promocionHasta: '2026-11-30' });
    expect(precioVigente(p)).toBe(85000);
    expect(precioTachado(p)).toBe(100000);
  });

  it('conDescuentoYPromocionSeTachaElOriginalYSeCobraElPromocional', () => {
    const p = producto({ precio: 80000, precioOriginal: 100000, precioPromocional: 72000 });
    expect(precioVigente(p)).toBe(72000);
    expect(precioTachado(p)).toBe(100000);
  });

  it('fechaDiaFormateaElDiaYElMesSinCorrerLaFecha', () => {
    const texto = fechaDia('2026-11-30');
    expect(texto).toMatch(/30/);
    expect(texto).toMatch(/nov/i);
  });
});
