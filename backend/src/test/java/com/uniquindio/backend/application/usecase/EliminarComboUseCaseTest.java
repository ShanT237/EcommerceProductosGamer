package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EliminarComboUseCaseTest {

    private ComboRepositoryEnMemoria comboRepository;
    private RegaloRepositoryEnMemoria regaloRepository;
    private EliminarComboUseCase useCase;
    private UUID vendedorId;
    private UUID comboId;

    @BeforeEach
    void setUp() {
        comboRepository = new ComboRepositoryEnMemoria();
        regaloRepository = new RegaloRepositoryEnMemoria();
        useCase = new EliminarComboUseCase(comboRepository, regaloRepository);
        vendedorId = UUID.randomUUID();
        comboId = UUID.randomUUID();
        comboRepository.guardar(Combo.crear(comboId, vendedorId, "Combo Gamer", "Setup", 15.0,
                List.of(UUID.randomUUID(), UUID.randomUUID())));
    }

    private Regalo regaloDelCombo() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), null, comboId);
        regaloRepository.guardar(regalo);
        return regalo;
    }

    @Test
    void eliminaElComboDelVendedor() {
        useCase.ejecutar(comboId, vendedorId);

        assertTrue(comboRepository.obtenerPorId(comboId).isEmpty());
    }

    @Test
    void soloElDuenoPuedeEliminar() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(comboId, UUID.randomUUID()));

        assertTrue(comboRepository.obtenerPorId(comboId).isPresent());
    }

    @Test
    void comboInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));
    }

    @Test
    void conUnRegaloPendienteLanzaReglaDominioYNoElimina() {
        regaloDelCombo(); // DISPONIBLE

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(comboId, vendedorId));

        assertTrue(comboRepository.obtenerPorId(comboId).isPresent());
    }

    @Test
    void unRegaloCanceladoNoImpideEliminar() {
        Regalo regalo = regaloDelCombo();
        regalo.cancelarRegalo();
        regaloRepository.guardar(regalo);

        useCase.ejecutar(comboId, vendedorId);

        assertTrue(comboRepository.obtenerPorId(comboId).isEmpty());
    }
}
