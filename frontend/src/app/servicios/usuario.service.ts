import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { PerfilCuenta } from '../modelos/modelos';

export interface PerfilUsuario extends PerfilCuenta {
  puntos: number;
}

/** Operaciones de la cuenta del COMPRADOR autenticado (todas sobre `/usuarios/me`). */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);

  obtenerPerfil(): Observable<PerfilUsuario> {
    return this.http.get<PerfilUsuario>(`${API_URL}/usuarios/me`);
  }

  editarPerfil(nombre: string, correo: string) {
    return this.http.put(`${API_URL}/usuarios/me/perfil`, { nombre, correo });
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string) {
    return this.http.put<void>(`${API_URL}/usuarios/me/contrasena`, { contrasenaActual, contrasenaNueva });
  }

  /** PUT /usuarios/me/baja (COMPRADOR): baja lógica; se conserva el historial. 422 si ya estaba de baja. */
  darDeBaja() {
    return this.http.put<PerfilUsuario>(`${API_URL}/usuarios/me/baja`, {});
  }
}
