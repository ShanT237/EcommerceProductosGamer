package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.PublicarProductoResponse;
import com.uniquindio.backend.domain.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public PublicarProductoResponse toPublicarResponse(Producto producto) {
        return new PublicarProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getGama(),
                producto.getExclusividad()
        );
    }
}