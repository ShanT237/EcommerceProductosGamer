import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../config';
import { PerfilCuenta, TipoVendedor } from '../modelos/modelos';

export interface PerfilVendedor extends PerfilCuenta {
  tipo: TipoVendedor;
  documento: string;
}

/** Operaciones de la cuenta del VENDEDOR autenticado (todas sobre `/vendedores/me`). */
@Injectable({ providedIn: 'root' })
export class VendedorService {
  private http = inject(HttpClient);

  obtenerPerfil(): Observable<PerfilVendedor> {
    return this.http.get<PerfilVendedor>(`${API_URL}/vendedores/me`);
  }

  /** PUT /vendedores/me/perfil: solo nombre y correo; el tipo y el documento no se editan. 409 si el correo ya es de otra cuenta. */
  editarPerfil(nombre: string, correo: string) {
    return this.http.put(`${API_URL}/vendedores/me/perfil`, { nombre, correo });
  }

  cambiarContrasena(contrasenaActual: string, contrasenaNueva: string) {
    return this.http.put<void>(`${API_URL}/vendedores/me/contrasena`, { contrasenaActual, contrasenaNueva });
  }

  /** PUT /vendedores/me/baja: baja lógica; se conserva el historial de ventas. 422 si ya estaba de baja. */
  darDeBaja() {
    return this.http.put<PerfilVendedor>(`${API_URL}/vendedores/me/baja`, {});
  }
}
