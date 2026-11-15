package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.BuscarProductoResponse;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

    private Promocion promocion(double porcentaje) {
        return Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), porcentaje,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void conPromocionVigenteExponePrecioPromocionalPorcentajeYFechaDeFin() {
        BuscarProductoResponse respuesta = mapper.toBuscarResponse(producto(), promocion(15));

        assertEquals(100.0, respuesta.precio(), 0.001);        // el precio base no cambia
        assertEquals(85.0, respuesta.precioPromocional(), 0.001);
        assertEquals(15.0, respuesta.porcentajePromocion(), 0.001);
        assertEquals(LocalDate.of(2026, 11, 30), respuesta.promocionHasta());
    }

    @Test
    void laPromocionSeAplicaSobreElPrecioYaConDescuentoInmediato() {
        Producto p = producto();
        p.aplicarDescuento(20);                                 // 100 -> 80

        BuscarProductoResponse respuesta = mapper.toBuscarResponse(p, promocion(10));

        assertEquals(80.0, respuesta.precio(), 0.001);
        assertEquals(100.0, respuesta.precioOriginal());
        assertEquals(72.0, respuesta.precioPromocional(), 0.001);   // 80 - 10 %
    }

    @Test
    void sinPromocionLosCamposPromocionalesSonNulos() {
        BuscarProductoResponse respuesta = mapper.toBuscarResponse(producto(), null);

        assertNull(respuesta.precioPromocional());
        assertNull(respuesta.porcentajePromocion());
        assertNull(respuesta.promocionHasta());
    }

    @Test
    void sinDescuentoPrecioOriginalYPorcentajeSonNulos() {
        BuscarProductoResponse respuesta = mapper.toBuscarResponse(producto());

        assertEquals(100.0, respuesta.precio(), 0.001);
        assertNull(respuesta.precioOriginal());
        assertNull(respuesta.porcentajeDescuento());
    }
}
