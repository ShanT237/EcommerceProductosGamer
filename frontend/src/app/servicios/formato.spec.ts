import { Producto } from '../modelos/modelos';
import { descuentoTotal, estadoPromocion, fechaDia, precioTachado, precioVigente } from './formato';

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

describe('formato (descuento total)', () => {
  it('acumulaOfertaYPromocionEnCadena', () => {
    const p = producto({ precio: 80000, precioOriginal: 100000, precioPromocional: 72000 });
    expect(descuentoTotal(p)).toBe(28); // 20% y luego 10%: no suman 30%
  });

  it('conSoloOfertaEsElPorcentajeDeLaOferta', () => {
    const p = producto({ precio: 80000, precioOriginal: 100000, porcentajeDescuento: 20 });
    expect(descuentoTotal(p)).toBe(20);
  });

  it('conSoloPromocionEsElPorcentajeDeLaPromocion', () => {
    const p = producto({ precioPromocional: 85000, porcentajePromocion: 15 });
    expect(descuentoTotal(p)).toBe(15);
  });

  it('sinRebajaDevuelveNull', () => {
    expect(descuentoTotal(producto())).toBeNull();
  });
});

describe('formato (estado de la promoción)', () => {
  const hoy = '2026-12-15';

  it('elDiaDeInicioYElDiaFinalSonVigentes', () => {
    expect(estadoPromocion('2026-12-15', '2026-12-31', hoy)).toBe('vigente');
    expect(estadoPromocion('2026-12-01', '2026-12-15', hoy)).toBe('vigente');
  });

  it('antesDelInicioEsProgramada', () => {
    expect(estadoPromocion('2026-12-16', '2026-12-31', hoy)).toBe('programada');
  });

  it('despuesDelFinEsVencida', () => {
    expect(estadoPromocion('2026-12-01', '2026-12-14', hoy)).toBe('vencida');
  });
});
