package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AsignarRegaloPorMetaUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private AsignarRegaloPorMetaUseCase useCase;
    private UUID usuarioId;
    private UUID regaloId;
    private UUID productoRegalo;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        useCase = new AsignarRegaloPorMetaUseCase(regaloRepository, usuarioRepository, compraRepository);

        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer1", "gamer@test.com"));
        regaloId = UUID.randomUUID();
        productoRegalo = UUID.randomUUID();
        regaloRepository.guardar(Regalo.crear(regaloId, productoRegalo, null));
    }

    private void comprar(UUID productoId, boolean completada) {
        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, productoId, 1, LocalDate.of(2026, 10, 1).atStartOfDay(), new Precio(100));
        if (completada) {
            compra.confirmar();
        }
        compraRepository.guardar(compra);
    }

    @Test
    void cumpleMetaAsignaYEstadoElegido() {
        comprar(UUID.randomUUID(), true);
        comprar(UUID.randomUUID(), true);
        comprar(UUID.randomUUID(), true);

        Regalo r = useCase.ejecutar(regaloId, usuarioId, 3);

        assertEquals(EstadoRegalo.ELEGIDO, r.getEstado());
        assertEquals(usuarioId, r.getIdUsuario());
        assertEquals(EstadoRegalo.ELEGIDO, regaloRepository.obtenerPorId(regaloId).get().getEstado());
    }

    @Test
    void noCumpleMetaLanzaReglaDominio() {
        comprar(UUID.randomUUID(), true);
        comprar(UUID.randomUUID(), true);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regaloId, usuarioId, 3));

        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(regaloId).get().getEstado());
    }

    @Test
    void usuarioQueYaPoseeElProductoLanzaReglaDominio() {
        comprar(productoRegalo, true);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regaloId, usuarioId, 1));

        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(regaloId).get().getEstado());
    }

    @Test
    void comprasPendientesNoCuentanParaLaMeta() {
        comprar(UUID.randomUUID(), true);
        comprar(UUID.randomUUID(), true);
        comprar(UUID.randomUUID(), false);
        comprar(UUID.randomUUID(), false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regaloId, usuarioId, 3));
    }

    @Test
    void regaloOUsuarioInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), usuarioId, 1));
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(regaloId, UUID.randomUUID(), 1));
    }
}
