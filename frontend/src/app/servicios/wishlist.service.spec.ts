import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../config';
import { Producto, Sesion } from '../modelos/modelos';
import { AuthService } from './auth.service';
import { WishlistService } from './wishlist.service';

function producto(id = 'p1'): Producto {
  return {
    id,
    vendedorId: 'v1',
    nombre: `Producto ${id}`,
    descripcion: 'Descripción',
    precio: 100,
    stock: 5,
    gama: 'MEDIA',
    exclusividad: 'NORMAL',
    categoria: null,
  };
}

function sesionDe(id: string, rol: Sesion['rol']): Sesion {
  return { id, correo: `${id}@correo.com`, rol, exp: 9999999999 };
}

describe('WishlistService', () => {
  let servicio: WishlistService;
  let http: HttpTestingController;
  let sesion: ReturnType<typeof signal<Sesion | null>>;

  const get = () => ({ method: 'GET', url: `${API_URL}/wishlists` });
  const post = () => ({ method: 'POST', url: `${API_URL}/wishlists` });
  const del = (id: string) => ({ method: 'DELETE', url: `${API_URL}/wishlists/${id}` });

  beforeEach(() => {
    sesion = signal<Sesion | null>(sesionDe('u1', 'COMPRADOR'));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    servicio = TestBed.inject(WishlistService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga los ids desde el servidor al iniciar sesión como comprador', () => {
    TestBed.tick(); // primera ejecución del efecto

    http.expectOne(get()).flush([producto('p1'), producto('p2')]);

    expect(servicio.esFavorito('p1')).toBe(true);
    expect(servicio.esFavorito('p2')).toBe(true);
    expect(servicio.productos().length).toBe(2);
    expect(servicio.cargando()).toBe(false);
  });

  it('el corazón sigue marcado tras una recarga simulada (el estado viene del servidor)', () => {
    TestBed.tick();
    http.expectOne(get()).flush([producto('p1')]);

    expect(servicio.ids().has('p1')).toBe(true);
    expect(servicio.esFavorito('otro')).toBe(false);
  });

  it('un vendedor no pide la wishlist', () => {
    sesion.set(sesionDe('v1', 'VENDEDOR'));

    TestBed.tick();

    http.expectNone(get());
    expect(servicio.ids().size).toBe(0);
  });

  it('un error al cargar deja el mensaje y la lista vacía', () => {
    TestBed.tick();

    http.expectOne(get()).flush({ mensaje: 'Algo falló' }, { status: 500, statusText: 'Server Error' });

    expect(servicio.error()).toBe('Algo falló');
    expect(servicio.productos().length).toBe(0);
    expect(servicio.cargando()).toBe(false);
  });

  it('alternar añade con POST y marca el id', () => {
    TestBed.tick();
    http.expectOne(get()).flush([]);

    servicio.alternar(producto('p1')).subscribe();
    const req = http.expectOne(post());
    expect(req.request.body).toEqual({ productoId: 'p1' });
    req.flush({ id: 'w1', usuarioId: 'u1', productos: ['p1'] }, { status: 201, statusText: 'Created' });

    expect(servicio.esFavorito('p1')).toBe(true);
    expect(servicio.productos().map((p) => p.id)).toEqual(['p1']);
  });

  it('alternar quita con DELETE y desmarca el id', () => {
    TestBed.tick();
    http.expectOne(get()).flush([producto('p1'), producto('p2')]);

    servicio.alternar(producto('p1')).subscribe();
    http.expectOne(del('p1')).flush({ id: 'w1', usuarioId: 'u1', productos: ['p2'] });

    expect(servicio.esFavorito('p1')).toBe(false);
    expect(servicio.esFavorito('p2')).toBe(true);
    expect(servicio.productos().map((p) => p.id)).toEqual(['p2']);
  });

  it('un POST 409 se trata como ya favorito', () => {
    TestBed.tick();
    http.expectOne(get()).flush([]);
    const errores: unknown[] = [];

    servicio.alternar(producto('p1')).subscribe({ error: (e) => errores.push(e) });
    http
      .expectOne(post())
      .flush({ mensaje: 'El producto ya se encuentra en la wishlist' }, { status: 409, statusText: 'Conflict' });

    expect(errores.length).toBe(0);
    expect(servicio.esFavorito('p1')).toBe(true);
  });

  it('otro error del POST se propaga y no marca el favorito', () => {
    TestBed.tick();
    http.expectOne(get()).flush([]);
    const errores: unknown[] = [];

    servicio.alternar(producto('p1')).subscribe({ error: (e) => errores.push(e) });
    http.expectOne(post()).flush({ mensaje: 'El producto no existe' }, { status: 422, statusText: 'Unprocessable Content' });

    expect(errores.length).toBe(1);
    expect(servicio.esFavorito('p1')).toBe(false);
  });

  it('se vacía al cambiar de usuario', () => {
    TestBed.tick();
    http.expectOne(get()).flush([producto('p1')]);
    expect(servicio.esFavorito('p1')).toBe(true);

    sesion.set(null);
    TestBed.tick();

    http.expectNone(get());
    expect(servicio.ids().size).toBe(0);
    expect(servicio.productos().length).toBe(0);
  });

  it('al entrar otro comprador recarga su propia wishlist', () => {
    TestBed.tick();
    http.expectOne(get()).flush([producto('p1')]);

    sesion.set(sesionDe('u2', 'COMPRADOR'));
    TestBed.tick();

    expect(servicio.ids().size).toBe(0); // no hereda los favoritos del usuario anterior
    http.expectOne(get()).flush([producto('p9')]);
    expect(servicio.esFavorito('p9')).toBe(true);
    expect(servicio.esFavorito('p1')).toBe(false);
  });
});
