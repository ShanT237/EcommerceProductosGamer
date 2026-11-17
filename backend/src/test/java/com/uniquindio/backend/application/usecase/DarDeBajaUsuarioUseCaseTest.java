package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DarDeBajaUsuarioUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private DarDeBajaUsuarioUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        useCase = new DarDeBajaUsuarioUseCase(usuarioRepository);
        id = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(id, "Gamer", "gamer@test.com"));
    }

    @Test
    void daDeBajaYGuarda() {
        assertFalse(useCase.ejecutar(id).isActivo());
        assertFalse(usuarioRepository.obtenerPorId(id).get().isActivo());
    }

    @Test
    void conservaPuntosYDatosDelHistorial() {
        usuarioRepository.obtenerPorId(id).get().acumularPuntos(40);

        useCase.ejecutar(id);

        Usuario guardado = usuarioRepository.obtenerPorId(id).get();
        assertEquals(40, guardado.getPuntos());
        assertEquals("gamer@test.com", guardado.getCorreo());
    }

    @Test
    void segundaBajaLanzaReglaDominio() {
        useCase.ejecutar(id);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id));
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
