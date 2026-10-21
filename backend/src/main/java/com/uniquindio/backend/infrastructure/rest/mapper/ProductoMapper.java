package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.PublicarProductoResponse;
import com.uniquindio.backend.application.dto.response.ModificarProductoResponse;
import com.uniquindio.backend.domain.entity.Producto;
import org.springframework.stereotype.Component;
import com.uniquindio.backend.application.dto.response.EditarInformacionProductoResponse;
import com.uniquindio.backend.application.dto.response.CambiarVisibilidadProductoResponse;

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

    public ModificarProductoResponse toModificarResponse(Producto producto) {
        return new ModificarProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getPrecio()
        );
    }

    public EditarInformacionProductoResponse toEditarInformacionResponse(Producto producto) {
        return new EditarInformacionProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getDescripcion()
        );
    }

    public CambiarVisibilidadProductoResponse toVisibilidadResponse(Producto producto) {
        return new CambiarVisibilidadProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.isPausado()
        );
    }
}