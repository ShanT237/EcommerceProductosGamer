import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { API_URL } from '../../config';
import { Pagina, Producto } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { CarritoService } from '../../servicios/carrito.service';
import { CompraService } from '../../servicios/compra.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ProductoService } from '../../servicios/producto.service';
import { WishlistService } from '../../servicios/wishlist.service';
import { Catalogo } from './catalogo';

describe('Catalogo (preventa)', () => {
  let fixture: ComponentFixture<Catalogo>;
  let http: HttpTestingController;
  let agregar: ReturnType<typeof vi.fn>;
  let crearPedido: ReturnType<typeof vi.fn>;

  const producto: Producto = {
    id: 'p1', vendedorId: 'v1', nombre: 'Mouse', descripcion: 'Mouse RGB',
    precio: 100000, stock: 5, gama: 'MEDIA', exclusividad: 'NORMAL', categoria: null,
  };
  const pagina: Pagina<Producto> = {
    contenido: [producto], pagina: 0, tamano: 6, totalElementos: 1, totalPaginas: 1,
  };
  const raiz = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    agregar = vi.fn();
    crearPedido = vi.fn(() => of({ pedidoId: 'ped1', compras: [], total: 0 }));
    TestBed.configureTestingModule({
      imports: [Catalogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { rol: () => 'COMPRADOR', sesion: signal({ id: 'u1', rol: 'COMPRADOR' }) },
        },
        { provide: ProductoService, useValue: { buscar: () => of(pagina) } },
        { provide: CompraService, useValue: { crearPedido } },
        { provide: CarritoService, useValue: { agregar } },
        {
          provide: WishlistService,
          useValue: { ids: signal(new Set<string>()), esFavorito: () => false, alternar: () => of(undefined), productos: signal([]) },
        },
        { provide: ConfirmacionService, useValue: { pedir: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deshabilitaComprarYAñadirAlCarritoMientrasElProductoEstaEnPreventa', () => {
    fixture = TestBed.createComponent(Catalogo);
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: `${API_URL}/preventas` }).flush([
      { id: 'pr1', productoId: 'p1', fechaLanzamiento: '2099-12-01', descripcion: 'Edición especial' },
    ]);
    fixture.detectChanges();
    (raiz().querySelector('app-tarjeta-producto .tarjeta') as HTMLElement).click();
    fixture.detectChanges();

    expect(raiz().textContent).toContain('Disponible desde el');
    const botones = [...raiz().querySelectorAll('.modal-info .acciones button')] as HTMLButtonElement[];
    expect(botones.find((boton) => boton.textContent?.includes('Comprar'))?.disabled).toBe(true);
    expect(botones.find((boton) => boton.textContent?.includes('Añadir al carrito'))?.disabled).toBe(true);
    expect(agregar).not.toHaveBeenCalled();
    expect(crearPedido).not.toHaveBeenCalled();
  });
});
