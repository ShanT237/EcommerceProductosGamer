import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Compra, MovimientoPuntos } from '../../modelos/modelos';
import { MisCompras } from './mis-compras';

const compra: Compra = {
  id: 'c1',
  pedidoId: null,
  usuarioId: 'u1',
  productoId: 'p1',
  cantidad: 1,
  precioUnitario: 100,
  subtotal: 100,
  fecha: '2026-11-14T10:00:00',
  estado: 'COMPLETADA',
};
const ganado = (n: number): MovimientoPuntos => ({ id: 'm' + n, cantidad: n, tipo: 'GANADO', fecha: '2026-11-14T10:00:00' });

describe('MisCompras (puntos)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisCompras],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('tras publicar una reseña vuelve a pedir el historial y el saldo se actualiza sin recargar', () => {
    const fixture = TestBed.createComponent(MisCompras);
    fixture.detectChanges();
    http.match((r) => r.url.startsWith(`${API_URL}/productos`)).forEach((r) => r.flush({ contenido: [] }));
    http.expectOne(`${API_URL}/compras`).flush([compra]);
    http.expectOne(`${API_URL}/puntos/historial`).flush([]);
    const c = fixture.componentInstance as any;
    expect(c.puntos()).toBe(0);

    c.abrirDialogo('resena', compra);
    c.comentario.set('Muy bueno');
    c.enviarDialogo();
    http.expectOne({ method: 'POST', url: `${API_URL}/resenas` }).flush({});

    http.expectOne(`${API_URL}/compras`).flush([{ ...compra }]);
    http.expectOne(`${API_URL}/puntos/historial`).flush([ganado(10)]);
    expect(c.puntos()).toBe(10);
    http.verify();
  });
});
