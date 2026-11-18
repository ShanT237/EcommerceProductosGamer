import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_URL } from '../config';
import { Rol, Sesion, TipoVendedor } from '../modelos/modelos';
import { ToastService } from './toast.service';

const CLAVE = 'gamerzone.token';
const INTERVALO_VERIFICACION_MS = 3000;

function decodificar(token: string | null): Sesion | null {
  if (!token) return null;
  try {
    const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(carga));
    if (!json.sub || !json.rol || json.exp * 1000 < Date.now()) return null;
    return { id: json.sub, correo: json.email, rol: json.rol as Rol, exp: json.exp };
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private toast = inject(ToastService);

  private _token = signal<string | null>(this.leer());
  readonly token = this._token.asReadonly();
  readonly sesion = computed(() => decodificar(this._token()));
  readonly autenticado = computed(() => this.sesion() !== null);
  readonly rol = computed(() => this.sesion()?.rol ?? null);
  readonly nombre = computed(() => (this.sesion()?.correo ?? '').split('@')[0]);

  constructor() {
    if (typeof window === 'undefined') return;
    // Borrar el token a mano (DevTools) en esta pestaña no avisa a Angular: se revisa de forma activa.
    window.addEventListener('storage', () => this.verificarSesion()); // cambios desde otra pestaña
    window.addEventListener('focus', () => this.verificarSesion());
    document.addEventListener('visibilitychange', () => this.verificarSesion());
    setInterval(() => this.verificarSesion(), INTERVALO_VERIFICACION_MS); // también detecta el vencimiento
  }

  /**
   * Comprueba que el token guardado siga existiendo, sea el mismo y no haya vencido.
   * Si no, cierra la sesión (aviso + login) y devuelve false. Sin sesión iniciada no hace nada.
   */
  verificarSesion(): boolean {
    const enMemoria = this._token();
    if (!enMemoria) return true;
    let guardado: string | null;
    try {
      guardado = localStorage.getItem(CLAVE);
    } catch {
      guardado = enMemoria; // sin almacenamiento: solo se valida el vencimiento
    }
    if (guardado === enMemoria && decodificar(enMemoria)) return true;
    this.cerrarSesion(true);
    return false;
  }

  private leer(): string | null {
    try {
      const guardado = localStorage.getItem(CLAVE);
      return decodificar(guardado) ? guardado : null;
    } catch {
      return null;
    }
  }

  private guardar(token: string | null) {
    this._token.set(token);
    try {
      if (token) localStorage.setItem(CLAVE, token);
      else localStorage.removeItem(CLAVE);
    } catch { /* sin almacenamiento disponible */ }
  }

  login(correo: string, contrasena: string) {
    return this.http
      .post<{ token: string }>(`${API_URL}/auth/login`, { correo, contrasena })
      .pipe(tap((r) => this.guardar(r.token)));
  }

  registrarComprador(nombre: string, correo: string, contrasena: string) {
    return this.http.post(`${API_URL}/usuarios`, { nombre, correo, contrasena });
  }

  registrarVendedor(datos: { nombre: string; correo: string; tipo: TipoVendedor; documento: string; contrasena: string }) {
    return this.http.post(`${API_URL}/vendedores`, datos);
  }

  /** Paso 1 de la recuperación: pide el enlace por correo. Siempre responde igual, exista o no el correo. */
  solicitarRecuperacion(correo: string) {
    return this.http.post<{ mensaje: string }>(`${API_URL}/auth/olvide-contrasena`, { correo });
  }

  /** Paso 2: elige la contraseña nueva con el token que llegó en el enlace del correo. */
  restablecerContrasena(token: string, contrasenaNueva: string) {
    return this.http.post<{ mensaje: string }>(`${API_URL}/auth/restablecer-contrasena`, { token, contrasenaNueva });
  }

  /** Cierra la sesión. Con `expirada` avisa al usuario; `mensaje` cambia el texto del aviso. */
  cerrarSesion(expirada = false, mensaje?: string) {
    this.guardar(null);
    if (expirada) this.toast.info(mensaje ?? 'Tu sesión terminó o expiró. Vuelve a ingresar.');
    this.router.navigate(['/login']);
  }
}