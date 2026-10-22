package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.AutenticacionException;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.HashContrasena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: IniciarSesion (CU-27).
 * Verifica credenciales y devuelve un token JWT. El error de credenciales es siempre el
 * mismo (no revela si falló el correo o la contraseña); la cuenta inactiva se informa
 * solo después de verificar la contraseña.
 */
@Service
@RequiredArgsConstructor
public class IniciarSesionUseCase {

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final HashContrasena hashContrasena;
    private final EmisorToken emisorToken;

    public String ejecutar(String correo, String contrasena) {
        if (correo == null || contrasena == null || contrasena.isEmpty()) {
            throw new AutenticacionException(CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = usuarioRepository.obtenerPorCorreo(correo)
                .orElseThrow(() -> new AutenticacionException(CREDENCIALES_INVALIDAS));
        if (!usuario.verificarContrasena(contrasena, hashContrasena)) {
            throw new AutenticacionException(CREDENCIALES_INVALIDAS);
        }
        usuario.asegurarActivo();

        return emisorToken.emitir(usuario.getId(), usuario.getCorreo());
    }
}
