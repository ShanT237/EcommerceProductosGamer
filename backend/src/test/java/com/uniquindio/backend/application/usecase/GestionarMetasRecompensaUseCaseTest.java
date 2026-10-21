package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.MetaRecompensaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GestionarMetasRecompensaUseCaseTest {

    private MetaRecompensaRepositoryEnMemoria metaRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private GestionarMetasRecompensaUseCase useCase;
    private Producto regalo;

    @BeforeEach
    void setUp() {
        metaRepository = new MetaRecompensaRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new GestionarMetasRecompensaUseCase(metaRepository, productoRepository);
        regalo = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mousepad", "XL",
                20, 10, Gama.BAJA, Exclusividad.NORMAL);
        productoRepository.guardar(regalo);
    }

    @Test
    void creaUnaMetaActivaYLaGuarda() {
        UUID id = UUID.randomUUID();

        MetaRecompensa meta = useCase.crear(id, 5, regalo.getId());

        assertTrue(meta.isActiva());
        assertEquals(5, metaRepository.obtenerPorId(id).get().getComprasRequeridas());
        assertEquals(regalo.getId(), meta.getProductoRegaloId());
    }

    @Test
    void rechazaUmbralNoPositivoAlCrearYAlActualizar() {
        assertThrows(ReglaDominioException.class, () -> useCase.crear(UUID.randomUUID(), 0, regalo.getId()));
        UUID id = UUID.randomUUID();
        useCase.crear(id, 5, regalo.getId());

        assertThrows(ReglaDominioException.class, () -> useCase.actualizarUmbral(id, -1));
        assertEquals(5, metaRepository.obtenerPorId(id).get().getComprasRequeridas());
    }

    @Test
    void actualizaElUmbral() {
        UUID id = UUID.randomUUID();
        useCase.crear(id, 5, regalo.getId());

        assertEquals(8, useCase.actualizarUmbral(id, 8).getComprasRequeridas());
    }

    @Test
    void desactivarDosVecesLanzaYSoloLasActivasSeListan() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        useCase.crear(id1, 5, regalo.getId());
        useCase.crear(id2, 10, regalo.getId());

        assertFalse(useCase.desactivar(id1).isActiva());

        assertThrows(ReglaDominioException.class, () -> useCase.desactivar(id1));
        assertEquals(1, metaRepository.obtenerActivas().size());
        assertEquals(id2, metaRepository.obtenerActivas().get(0).getId());
    }

    @Test
    void elProductoRegaloDebeExistirYNoEstarEliminado() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.crear(UUID.randomUUID(), 5, UUID.randomUUID()));

        regalo.eliminar(false);
        assertThrows(ReglaDominioException.class, () -> useCase.crear(UUID.randomUUID(), 5, regalo.getId()));
    }

    @Test
    void metaInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.actualizarUmbral(UUID.randomUUID(), 3));
        assertThrows(NoSuchElementException.class, () -> useCase.desactivar(UUID.randomUUID()));
    }
}
