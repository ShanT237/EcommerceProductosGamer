package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.RegistrarUsuarioResponse;
import com.uniquindio.backend.domain.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public RegistrarUsuarioResponse toRegistrarResponse(Usuario usuario) {
        return new RegistrarUsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getPuntos(),
                usuario.isActivo()
        );
    }
}