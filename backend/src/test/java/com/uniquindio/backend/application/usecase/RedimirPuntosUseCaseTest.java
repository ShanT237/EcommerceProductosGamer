package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;
import com.uniquindio.backend.infrastructure.persistence.MovimientoPuntosRepositoryEnMemoria;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedimirPuntosUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private RedimirPuntosUseCase useCase;
    private MovimientoPuntosRepositoryEnMemoria movimientos;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        movimientos = new MovimientoPuntosRepositoryEnMemoria();
        useCase = new RedimirPuntosUseCase(usuarioRepository, movimientos, Clock.system(ZoneId.systemDefault()));
        id = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(id, "Gamer1", "gamer@test.com");
        usuario.acumularPuntos(50);
        usuarioRepository.guardar(usuario);
    }

    @Test
    void redimeYGuarda() {
        assertEquals(20, useCase.ejecutar(id, 30).getPuntos());
        assertEquals(20, usuarioRepository.obtenerPorId(id).get().getPuntos());
    }

    @Test
    void saldoInsuficienteNoCambiaSaldo() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, 80));
        assertEquals(50, usuarioRepository.obtenerPorId(id).get().getPuntos());
    }

    @Test
    void usuarioInactivoLanzaReglaDominio() {
        usuarioRepository.obtenerPorId(id).get().desactivar();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, 10));
        assertEquals(50, usuarioRepository.obtenerPorId(id).get().getPuntos());
    }

    @Test
    void usuarioInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), 10));
    }

    @Test
    void redimirRegistraMovimientoYUnaRedencionFallidaNo() {
        useCase.ejecutar(id, 30);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, 80));

        assertEquals(1, movimientos.obtenerPorUsuarioId(id).size());
        assertEquals(TipoMovimientoPuntos.REDIMIDO, movimientos.obtenerPorUsuarioId(id).get(0).getTipo());
        assertEquals(30, movimientos.obtenerPorUsuarioId(id).get(0).getCantidad());
    }
}
