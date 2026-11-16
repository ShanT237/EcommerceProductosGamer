import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';

export interface PerfilUsuario {
  id: string;
  nombre: string;
  correo: string;
  puntos: number;
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);

  obtenerPerfil() {
    return this.http.get<PerfilUsuario>(`${API_URL}/usuarios/me`);
  }

  editarPerfil(nombre: string, correo: string) {
    return this.http.put(`${API_URL}/usuarios/me/perfil`, { nombre, correo });
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string) {
    return this.http.put<void>(`${API_URL}/usuarios/me/contrasena`, { contrasenaActual, contrasenaNueva });
  }
}