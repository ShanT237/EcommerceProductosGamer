import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from '../../config';
import { Producto, Sesion } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { FormularioPublicar } from './formulario-publicar';

const creado: Producto = {
  id: 'p-nuevo',
  vendedorId: 'v1',
  nombre: 'Teclado de ensayo',
  descripcion: 'Switches rojos',
  precio: 150000,
  stock: 2,
  gama: 'MEDIA',
  exclusividad: 'NORMAL',
  categoria: null,
};

describe('FormularioPublicar', () => {
  let fixture: ComponentFixture<FormularioPublicar>;
  let http: HttpTestingController;

  const postProducto = () => ({ method: 'POST', url: `${API_URL}/productos` });
  const boton = () => fixture.nativeElement.querySelector('button.enviar') as HTMLButtonElement;
  const campo = (id: string) => fixture.nativeElement.querySelector(`#fp-${id}`) as HTMLInputElement;
  const erroresDeCampo = () =>
    Array.from(fixture.nativeElement.querySelectorAll('.error-campo')).map((e) => (e as HTMLElement).textContent?.trim());

  function escribir(id: string, valor: string) {
    const el = campo(id);
    el.value = valor;
    el.dispatchEvent(new Event('input'));
  }

  function llenarValido() {
    escribir('nombre', 'Teclado de ensayo');
    escribir('descripcion', 'Switches rojos');
    escribir('precio', '150000');
    escribir('stock', '2');
  }

  beforeEach(() => {
    const sesion = signal<Sesion | null>({ id: 'v1', correo: 'demo@gamerzone.com', rol: 'VENDEDOR', exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [FormularioPublicar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { sesion } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FormularioPublicar);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('con el formulario vacío no envía y muestra los errores', () => {
    boton().click();
    fixture.detectChanges();

    http.expectNone(postProducto());
    expect(erroresDeCampo()).toContain('El nombre es obligatorio');
    expect(erroresDeCampo()).toContain('La descripción es obligatoria');
  });

  it('un nombre de solo espacios cuenta como vacío', () => {
    llenarValido();
    escribir('nombre', '   ');

    boton().click();
    fixture.detectChanges();

    http.expectNone(postProducto());
    expect(erroresDeCampo()).toContain('El nombre es obligatorio');
  });

  it('rechaza precio no positivo y stock decimal', () => {
    llenarValido();
    escribir('precio', '0');
    escribir('stock', '2.5');

    boton().click();
    fixture.detectChanges();

    http.expectNone(postProducto());
    expect(erroresDeCampo()).toContain('El precio debe ser mayor a cero');
    expect(erroresDeCampo()).toContain('El stock debe ser un número entero de cero o más');
  });

  it('rechaza un stock negativo', () => {
    llenarValido();
    escribir('stock', '-1');

    boton().click();
    fixture.detectChanges();

    http.expectNone(postProducto());
    expect(erroresDeCampo()).toContain('El stock debe ser un número entero de cero o más');
  });

  it('envía el cuerpo esperado, bloquea el botón y emite publicado', () => {
    const emitidos: Producto[] = [];
    fixture.componentInstance.publicado.subscribe((p) => emitidos.push(p));
    llenarValido();

    boton().click();
    const req = http.expectOne(postProducto());
    expect(req.request.body).toEqual({
      nombre: 'Teclado de ensayo',
      descripcion: 'Switches rojos',
      precio: 150000,
      stock: 2,
      gama: 'MEDIA',
      exclusividad: 'NORMAL',
    });

    fixture.detectChanges();
    expect(boton().disabled).toBe(true);
    expect(boton().textContent?.trim()).toBe('Publicando…');

    req.flush(creado, { status: 201, statusText: 'Created' });
    fixture.detectChanges();

    expect(emitidos).toEqual([creado]);
    expect(boton().disabled).toBe(false);
    expect(campo('nombre').value).toBe('');
    expect(campo('stock').value).toBe('1');
  });

  it('un 403 muestra el mensaje por defecto y vuelve a habilitar el botón', () => {
    llenarValido();

    boton().click();
    http.expectOne(postProducto()).flush(null, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    const aviso = fixture.nativeElement.querySelector('.error') as HTMLElement;
    expect(aviso.textContent).toContain('No tienes permiso para hacer esto.');
    expect(boton().disabled).toBe(false);
  });

  it('un 422 muestra el mensaje del backend y conserva lo escrito', () => {
    llenarValido();

    boton().click();
    http
      .expectOne(postProducto())
      .flush({ mensaje: 'Un vendedor dado de baja no puede publicar' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    const aviso = fixture.nativeElement.querySelector('.error') as HTMLElement;
    expect(aviso.textContent).toContain('Un vendedor dado de baja no puede publicar');
    expect(campo('nombre').value).toBe('Teclado de ensayo');
    expect(boton().disabled).toBe(false);
  });

  it('un segundo clic mientras publica no genera otro POST', () => {
    llenarValido();

    boton().click();
    boton().click();

    // expectOne falla si hubiera dos peticiones POST.
    http.expectOne(postProducto());
  });
});
