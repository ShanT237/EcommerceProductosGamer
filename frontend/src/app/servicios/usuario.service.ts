import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_URL } from '../config';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);

  editarPerfil(nombre: string, correo: string) {
    return this.http.put(`${API_URL}/usuarios/me/perfil`, { nombre, correo });
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string) {
    return this.http.put<void>(`${API_URL}/usuarios/me/contrasena`, { contrasenaActual, contrasenaNueva });
  }
}
