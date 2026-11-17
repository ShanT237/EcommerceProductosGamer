package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import org.springframework.stereotype.Component;

/**
 * Consulta el estado real de la cuenta en el repositorio que corresponde al rol del token.
 * Una cuenta que ya no existe se trata como inactiva (fail-closed).
 */
@Component
public class VerificadorCuentaActivaRepositorio implements VerificadorCuentaActiva {

    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;

    public VerificadorCuentaActivaRepositorio(UsuarioRepository usuarioRepository,
                                              VendedorRepository vendedorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.vendedorRepository = vendedorRepository;
    }

    @Override
    public boolean estaActiva(SesionToken sesion) {
        return switch (sesion.rol()) {
            case COMPRADOR -> usuarioRepository.obtenerPorId(sesion.usuarioId())
                    .map(Usuario::isActivo)
                    .orElse(false);
            case VENDEDOR -> vendedorRepository.obtenerPorId(sesion.usuarioId())
                    .map(Vendedor::isActivo)
                    .orElse(false);
        };
    }
}
