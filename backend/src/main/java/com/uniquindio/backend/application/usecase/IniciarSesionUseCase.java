package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.AutenticacionException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.HashContrasena;
import com.uniquindio.backend.domain.valueobject.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Caso de uso: IniciarSesion (CU-27).
 * Verifica credenciales de comprador o vendedor y devuelve un token JWT con su rol.
 * El error de credenciales es siempre el mismo (no revela si falló el correo o la
 * contraseña); la cuenta inactiva se informa solo después de verificar la contraseña.
 */
@Service
@RequiredArgsConstructor
public class IniciarSesionUseCase {

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final HashContrasena hashContrasena;
    private final EmisorToken emisorToken;

    public String ejecutar(String correo, String contrasena) {
        if (correo == null || contrasena == null || contrasena.isEmpty()) {
            throw new AutenticacionException(CREDENCIALES_INVALIDAS);
        }

        Optional<Usuario> comprador = usuarioRepository.obtenerPorCorreo(correo)
                .filter(u -> u.verificarContrasena(contrasena, hashContrasena));
        if (comprador.isPresent()) {
            Usuario usuario = comprador.get();
            usuario.asegurarActivo();
            return emisorToken.emitir(usuario.getId(), usuario.getCorreo(), Rol.COMPRADOR);
        }

        Optional<Vendedor> vendedorEncontrado = vendedorRepository.obtenerPorCorreo(correo)
                .filter(v -> v.verificarContrasena(contrasena, hashContrasena));
        if (vendedorEncontrado.isPresent()) {
            Vendedor vendedor = vendedorEncontrado.get();
            if (!vendedor.isActivo()) {
                throw new ReglaDominioException("El vendedor se encuentra dado de baja");
            }
            return emisorToken.emitir(vendedor.getId(), vendedor.getCorreo(), Rol.VENDEDOR);
        }

        throw new AutenticacionException(CREDENCIALES_INVALIDAS);
    }
}