package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelarYReembolsarUseCaseTest {

    private CompraRepository compraRepository;
    private ProductoRepository productoRepository;
    private CancelarCompraUseCase cancelarUseCase;
    private SolicitarReembolsoUseCase reembolsoUseCase;

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 18, 10, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        cancelarUseCase = new CancelarCompraUseCase(compraRepository, productoRepository);
        reembolsoUseCase = new SolicitarReembolsoUseCase(compraRepository, productoRepository, RELOJ);
    }

    @Test
    @DisplayName("Cancelar una compra PENDIENTE cambia estado a CANCELADA y restituye stock al producto")
    void cancelarCompraRestituyeStock() {
        UUID usuarioId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Silla Gamer", "Ergonómica", 200.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(2); // Quedan 8
        productoRepository.guardar(producto);

        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 2, AHORA.minusHours(24), new Precio(200.0));
        compraRepository.guardar(compra);

        Compra compraCancelada = cancelarUseCase.ejecutar(compra.getId(), usuarioId);

        assertEquals(EstadoCompra.CANCELADA, compraCancelada.getEstado());
        assertEquals(10, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }

    @Test
    @DisplayName("Reembolsar una compra COMPLETADA cambia estado a REEMBOLSADA y restituye stock al producto")
    void reembolsarCompraRestituyeStock() {
        UUID usuarioId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Headset Gamer", "7.1 Surround", 80.0, 5, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(1); // Quedan 4
        productoRepository.guardar(producto);

        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, AHORA.minusHours(24), new Precio(80.0));
        compra.confirmar(); // PENDIENTE -> COMPLETADA
        compraRepository.guardar(compra);

        Compra compraReembolsada = reembolsoUseCase.ejecutar(compra.getId(), usuarioId);

        assertEquals(EstadoCompra.REEMBOLSADA, compraReembolsada.getEstado());
        assertEquals(5, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }
}
