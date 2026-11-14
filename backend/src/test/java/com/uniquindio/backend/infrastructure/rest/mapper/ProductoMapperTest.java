package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.BuscarProductoResponse;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductoMapperTest {

    private final ProductoMapper mapper = new ProductoMapper();

    private Producto producto() {
        return Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    void conDescuentoExponePrecioOriginalYPorcentaje() {
        Producto p = producto();
        p.aplicarDescuento(20);

        BuscarProductoResponse respuesta = mapper.toBuscarResponse(p);

        assertEquals(80.0, respuesta.precio(), 0.001);
        assertEquals(100.0, respuesta.precioOriginal());
        assertEquals(20.0, respuesta.porcentajeDescuento(), 0.001);
    }

    @Test
    void sinDescuentoPrecioOriginalYPorcentajeSonNulos() {
        BuscarProductoResponse respuesta = mapper.toBuscarResponse(producto());

        assertEquals(100.0, respuesta.precio(), 0.001);
        assertNull(respuesta.precioOriginal());
        assertNull(respuesta.porcentajeDescuento());
    }
}
