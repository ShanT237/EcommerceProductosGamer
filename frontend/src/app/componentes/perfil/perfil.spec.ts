import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { API_URL } from '../../config';
import { Rol } from '../../modelos/modelos';
import { AuthService } from '../../servicios/auth.service';
import { ConfirmacionService } from '../../servicios/confirmacion.service';
import { ToastService } from '../../servicios/toast.service';
import { Perfil } from './perfil';

const COMPRADOR = { id: 'u1', nombre: 'Jose', correo: 'jose@correo.com', puntos: 30, activo: true };
const VENDEDOR = {
  id: 'v1', nombre: 'GamerZone', correo: 'ventas@gamerzone.com', tipo: 'EMPRESA', documento: '900123', activo: true,
};

describe('Perfil (comprador y vendedor)', () => {
  let fixture: ComponentFixture<Perfil>;
  let http: HttpTestingController;
  let toast: ToastService;
  let confirmacion: ConfirmacionService;
  let cerrarSesion: ReturnType<typeof vi.fn>;

  const raiz = () => fixture.nativeElement as HTMLElement;
  const texto = (selector: string) => raiz().querySelector(selector)?.textContent?.trim() ?? '';
  const boton = (contiene: string) =>
    Array.from(raiz().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(contiene))!;

  /** Abre la pantalla con la sesión del rol indicado y responde la carga del perfil. */
  async function abrir(rol: Rol, perfil: object) {
    cerrarSesion = vi.fn();
    const sesion = signal({ id: 'x', correo: 'cuenta@correo.com', rol, exp: 9999999999 });
    TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { sesion, rol: () => rol, cerrarSesion } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    confirmacion = TestBed.inject(ConfirmacionService);
    fixture = TestBed.createComponent(Perfil);
    fixture.detectChanges();
    const base = rol === 'VENDEDOR' ? 'vendedores' : 'usuarios';
    http.expectOne({ method: 'GET', url: `${API_URL}/${base}/me` }).flush(perfil);
    fixture.detectChanges();
    await fixture.whenStable(); // ngModel escribe los valores en un microtask
  }

  afterEach(() => http.verify());

  it('el comprador carga su perfil desde /usuarios/me y no ve tipo ni documento', async () => {
    await abrir('COMPRADOR', COMPRADOR);

    expect((raiz().querySelector('#nombre') as HTMLInputElement).value).toBe('Jose');
    expect((raiz().querySelector('#correo') as HTMLInputElement).value).toBe('jose@correo.com');
    expect(raiz().querySelector('.fijos')).toBeNull();
  });

  it('el vendedor carga su perfil desde /vendedores/me y ve su tipo y documento sin poder editarlos', async () => {
    await abrir('VENDEDOR', VENDEDOR);

    expect((raiz().querySelector('#nombre') as HTMLInputElement).value).toBe('GamerZone');
    const fijos = texto('.fijos');
    expect(fijos).toContain('Empresa');
    expect(fijos).toContain('NIT');
    expect(fijos).toContain('900123');
    expect(raiz().querySelector('#documento')).toBeNull();
  });

  it('el vendedor guarda nombre y correo con PUT /vendedores/me/perfil', async () => {
    await abrir('VENDEDOR', VENDEDOR);
    const ok = vi.spyOn(toast, 'ok');

    const nombre = raiz().querySelector('#nombre') as HTMLInputElement;
    nombre.value = 'GamerZone Pro';
    nombre.dispatchEvent(new Event('input'));
    raiz().querySelector('form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/perfil` });
    expect(req.request.body).toEqual({ nombre: 'GamerZone Pro', correo: 'ventas@gamerzone.com' });
    req.flush({ ...VENDEDOR, nombre: 'GamerZone Pro' });
    expect(ok).toHaveBeenCalledWith('Perfil actualizado.');
  });

  it('el vendedor cambia su contraseña con PUT /vendedores/me/contrasena', async () => {
    await abrir('VENDEDOR', VENDEDOR);
    const escribir = (id: string, valor: string) => {
      const input = raiz().querySelector(`#${id}`) as HTMLInputElement;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    };
    escribir('actual', 'Vendedor2026');
    escribir('nueva', 'Nueva2027x');
    escribir('repetida', 'Nueva2027x');
    raiz().querySelectorAll('form')[1].dispatchEvent(new Event('submit'));

    const req = http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/contrasena` });
    expect(req.request.body).toEqual({ contrasenaActual: 'Vendedor2026', contrasenaNueva: 'Nueva2027x' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('el comprador se da de baja con PUT /usuarios/me/baja y se cierra su sesión', async () => {
    await abrir('COMPRADOR', COMPRADOR);

    boton('Dar de baja mi cuenta').click();
    expect(confirmacion.actual()?.mensaje).toContain('historial de compras');
    confirmacion.responder(true);
    await Promise.resolve();

    http.expectOne({ method: 'PUT', url: `${API_URL}/usuarios/me/baja` }).flush({ ...COMPRADOR, activo: false });
    expect(cerrarSesion).toHaveBeenCalled();
  });

  it('el vendedor se da de baja con PUT /vendedores/me/baja y se cierra su sesión', async () => {
    await abrir('VENDEDOR', VENDEDOR);

    boton('Dar de baja mi cuenta').click();
    expect(confirmacion.actual()?.mensaje).toContain('historial de ventas');
    confirmacion.responder(true);
    await Promise.resolve();

    http.expectOne({ method: 'PUT', url: `${API_URL}/vendedores/me/baja` }).flush({ ...VENDEDOR, activo: false });
    expect(cerrarSesion).toHaveBeenCalled();
  });

  it('si la persona cancela la confirmación no se llama a la API ni se cierra la sesión', async () => {
    await abrir('COMPRADOR', COMPRADOR);

    boton('Dar de baja mi cuenta').click();
    confirmacion.responder(false);
    await Promise.resolve();

    http.expectNone(`${API_URL}/usuarios/me/baja`);
    expect(cerrarSesion).not.toHaveBeenCalled();
  });

  it('si la baja falla muestra el error del backend y no cierra la sesión', async () => {
    await abrir('COMPRADOR', COMPRADOR);

    boton('Dar de baja mi cuenta').click();
    confirmacion.responder(true);
    await Promise.resolve();

    http.expectOne({ method: 'PUT', url: `${API_URL}/usuarios/me/baja` })
      .flush({ mensaje: 'El usuario ya está dado de baja' }, { status: 422, statusText: 'Unprocessable Content' });
    fixture.detectChanges();

    expect(texto('.baja .error')).toContain('El usuario ya está dado de baja');
    expect(cerrarSesion).not.toHaveBeenCalled();
  });
});
