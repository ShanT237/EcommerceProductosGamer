package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ValoracionVendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValorarVendedorUseCaseTest {

    private CompraRepositoryEnMemoria compraRepository;
    private ValoracionVendedorRepositoryEnMemoria valoracionRepository;
    private ValorarVendedorUseCase useCase;
    private UUID usuarioId;
    private UUID vendedorId;
    private Compra compra;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        ProductoRepositoryEnMemoria productoRepository = new ProductoRepositoryEnMemoria();
        valoracionRepository = new ValoracionVendedorRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(LocalDateTime.of(2026, 10, 2, 10, 0).atZone(zona).toInstant(), zona);
        useCase = new ValorarVendedorUseCase(compraRepository, productoRepository, valoracionRepository, reloj);

        vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Desc",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        usuarioId = UUID.randomUUID();
        compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1,
                LocalDateTime.of(2026, 10, 1, 10, 0), new Precio(100));
        compra.confirmar();
        compraRepository.guardar(compra);
    }

    @Test
    void valoraAlVendedorDelProductoDeLaCompra() {
        ValoracionVendedor v = useCase.ejecutar(compra.getId(), usuarioId, 5, "Excelente atención");

        assertEquals(vendedorId, v.getVendedorId());
        assertEquals(5, v.getCalificacion().estrellas());
        assertEquals(1, valoracionRepository.obtenerPorVendedorId(vendedorId).size());
    }

    @Test
    void soloConCompraCompletada() {
        compra = Compra.realizar(UUID.randomUUID(), usuarioId, compra.getProductoId(), 1,
                LocalDateTime.of(2026, 10, 1, 10, 0), new Precio(100));
        compraRepository.guardar(compra);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, 5, "x"));
        assertTrue(valoracionRepository.obtenerPorVendedorId(vendedorId).isEmpty());
    }

    @Test
    void unaValoracionPorCompra() {
        useCase.ejecutar(compra.getId(), usuarioId, 5, "Bien");

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, 1, "Mal"));
        assertEquals(1, valoracionRepository.obtenerPorVendedorId(vendedorId).size());
    }

    @Test
    void compraAjenaOInexistenteRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(compra.getId(), UUID.randomUUID(), 5, "x"));
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), usuarioId, 5, "x"));
    }

    @Test
    void estrellasFueraDeRangoRechazadas() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, 0, "x"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, 6, "x"));
        assertTrue(valoracionRepository.obtenerPorVendedorId(vendedorId).isEmpty());
    }
}
