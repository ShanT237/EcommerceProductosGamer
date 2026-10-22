package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;
import com.uniquindio.backend.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubirResenaUseCaseTest {

    private CompraRepositoryEnMemoria compraRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private MovimientoPuntosRepositoryEnMemoria movimientos;
    private SubirResenaUseCase useCase;
    private UUID usuarioId;
    private Compra compra;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        movimientos = new MovimientoPuntosRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(LocalDateTime.of(2026, 10, 1, 10, 0).atZone(zona).toInstant(), zona);
        useCase = new SubirResenaUseCase(compraRepository, new ResenaRepositoryEnMemoria(),
                usuarioRepository, movimientos, reloj);

        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "g@test.com"));
        compra = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1,
                LocalDateTime.of(2026, 9, 30, 10, 0), new Precio(100));
        compra.confirmar();
        compraRepository.guardar(compra);
    }

    @Test
    void reseniaAcumulaPuntosYRegistraElMovimientoGanado() {
        useCase.ejecutar(compra.getId(), usuarioId, 5, "Excelente");

        assertEquals(10, usuarioRepository.obtenerPorId(usuarioId).get().getPuntos());
        List<MovimientoPuntos> historial = movimientos.obtenerPorUsuarioId(usuarioId);
        assertEquals(1, historial.size());
        assertEquals(TipoMovimientoPuntos.GANADO, historial.get(0).getTipo());
        assertEquals(10, historial.get(0).getCantidad());
        assertEquals(LocalDateTime.of(2026, 10, 1, 10, 0), historial.get(0).getFecha());
    }

    @Test
    void siLaReseniaFallaNoSeRegistraMovimiento() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(compra.getId(), usuarioId, 5, " "));

        assertTrue(movimientos.obtenerPorUsuarioId(usuarioId).isEmpty());
    }
}
