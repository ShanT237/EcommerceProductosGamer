package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmarPedidoUseCaseTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private CompraRepositoryEnMemoria compraRepository;
    private ConfirmarPedidoUseCase useCase;
    private UUID usuarioId;
    private UUID pedidoId;
    private Compra linea1;
    private Compra linea2;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        useCase = new ConfirmarPedidoUseCase(compraRepository, TestNotificador.nuevo());
        usuarioId = UUID.randomUUID();
        pedidoId = UUID.randomUUID();
        linea1 = linea(usuarioId, pedidoId);
        linea2 = linea(usuarioId, pedidoId);
    }

    private Compra linea(UUID dueno, UUID pedido) {
        Compra compra = Compra.realizar(UUID.randomUUID(), dueno, UUID.randomUUID(), 1, AHORA,
                new Precio(100), pedido);
        compraRepository.guardar(compra);
        return compra;
    }

    @Test
    void confirmaTodasLasLineas() {
        List<Compra> confirmadas = useCase.ejecutar(pedidoId, usuarioId);

        assertEquals(2, confirmadas.size());
        assertTrue(confirmadas.stream().allMatch(c -> c.getEstado() == EstadoCompra.COMPLETADA));
        assertEquals(EstadoCompra.COMPLETADA, compraRepository.obtenerPorId(linea1.getId()).get().getEstado());
        assertEquals(EstadoCompra.COMPLETADA, compraRepository.obtenerPorId(linea2.getId()).get().getEstado());
    }

    @Test
    void pedidoAjenoRechazaYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, UUID.randomUUID()));

        assertEquals(EstadoCompra.PENDIENTE, linea1.getEstado());
        assertEquals(EstadoCompra.PENDIENTE, linea2.getEstado());
    }

    @Test
    void siUnaLineaNoEstaPendienteNoCambiaNinguna() {
        linea2.cancelar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, usuarioId));

        assertEquals(EstadoCompra.PENDIENTE, linea1.getEstado());
        assertEquals(EstadoCompra.CANCELADA, linea2.getEstado());
    }

    @Test
    void unPedidoYaConfirmadoNoSeConfirmaDeNuevo() {
        useCase.ejecutar(pedidoId, usuarioId);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, usuarioId));
    }

    @Test
    void noTocaLasLineasDeOtrosPedidos() {
        Compra deOtroPedido = linea(usuarioId, UUID.randomUUID());
        Compra suelta = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1, AHORA, new Precio(100));
        compraRepository.guardar(suelta);

        useCase.ejecutar(pedidoId, usuarioId);

        assertEquals(EstadoCompra.PENDIENTE, deOtroPedido.getEstado());
        assertEquals(EstadoCompra.PENDIENTE, suelta.getEstado());
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), usuarioId));
    }
}
