package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.RegistrarVendedorResponse;
import com.uniquindio.backend.domain.entity.Vendedor;
import org.springframework.stereotype.Component;

@Component
public class VendedorMapper {

    public RegistrarVendedorResponse toRegistrarResponse(Vendedor vendedor) {
        return new RegistrarVendedorResponse(
                vendedor.getId(),
                vendedor.getNombre(),
                vendedor.getCorreo(),
                vendedor.getTipo(),
                vendedor.getDocumento(),
                vendedor.isActivo()
        );
    }
}