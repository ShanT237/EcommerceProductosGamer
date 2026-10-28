import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_URL } from '../config';
import { Rol, Sesion, TipoVendedor } from '../modelos/modelos';

const CLAVE = 'gamerzone.token';

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

  private _token = signal<string | null>(this.leer());
  readonly token = this._token.asReadonly();
  readonly sesion = computed(() => decodificar(this._token()));
  readonly autenticado = computed(() => this.sesion() !== null);
  readonly rol = computed(() => this.sesion()?.rol ?? null);
  readonly nombre = computed(() => (this.sesion()?.correo ?? '').split('@')[0]);

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

  cerrarSesion(expirada = false) {
    this.guardar(null);
    this.router.navigate(['/login'], expirada ? { queryParams: { expirada: 1 } } : {});
  }
}
