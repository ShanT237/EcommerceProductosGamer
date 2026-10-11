package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.*;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RealizarCompraUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private ProductoRepository productoRepository;
    private PreventaRepository preventaRepository;
    private UsuarioRepository usuarioRepository;
    private RealizarCompraUseCase useCase;
    private UUID usuarioId;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        VendedorRepository vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new RealizarCompraUseCase(productoRepository, new CompraRepositoryEnMemoria(),
                usuarioRepository, vendedorRepository, preventaRepository);

        vendedorId = UUID.randomUUID();
        vendedorRepository.guardar(Vendedor.registrar(vendedorId, "GamerZone", "v@gamerzone.com",
                TipoVendedor.EMPRESA, "900123456-7"));
        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "gamer@test.com"));
        producto = nuevoProducto(Exclusividad.NORMAL);
    }

    private Producto nuevoProducto(Exclusividad exclusividad) {
        Producto p = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, exclusividad);
        productoRepository.guardar(p);
        return p;
    }

    private Compra comprar(Producto p, int cantidad, LocalDate fecha) {
        return useCase.ejecutar(UUID.randomUUID(), usuarioId, p.getId(), cantidad, fecha);
    }

    @Test
    @DisplayName("Una compra válida queda PENDIENTE y descuenta el stock")
    void compraValida() {
        Compra compra = comprar(producto, 2, HOY);

        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());
        assertEquals(3, producto.getStock());
    }

    @Test
    @DisplayName("Rechaza comprar más que el stock y no lo modifica (Regla 1)")
    void rechazaSinStock() {
        assertThrows(ReglaDominioException.class, () -> comprar(producto, 6, HOY));
        assertEquals(5, producto.getStock());
    }

    @Test
    @DisplayName("Rechaza comprar una preventa antes de su fecha y la permite desde ese día (Regla 4)")
    void preventa() {
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), producto.getId(),
                LocalDate.of(2026, 12, 1), "Lanzamiento diciembre"));

        assertThrows(ReglaDominioException.class, () -> comprar(producto, 1, LocalDate.of(2026, 11, 30)));
        assertEquals(5, producto.getStock());
        assertDoesNotThrow(() -> comprar(producto, 1, LocalDate.of(2026, 12, 1)));
    }

    @Test
    @DisplayName("Un exclusivo solo se compra una vez y de a una unidad (Regla 9)")
    void exclusivo() {
        Producto exclusivo = nuevoProducto(Exclusividad.EXCLUSIVO);

        assertThrows(ReglaDominioException.class, () -> comprar(exclusivo, 2, HOY));
        comprar(exclusivo, 1, HOY);
        assertThrows(ReglaDominioException.class, () -> comprar(exclusivo, 1, HOY));
    }

    @Test
    @DisplayName("Rechaza comprar si el usuario está inactivo")
    void usuarioInactivo() {
        usuarioRepository.obtenerPorId(usuarioId).get().desactivar();

        assertThrows(ReglaDominioException.class, () -> comprar(producto, 1, HOY));
    }
}