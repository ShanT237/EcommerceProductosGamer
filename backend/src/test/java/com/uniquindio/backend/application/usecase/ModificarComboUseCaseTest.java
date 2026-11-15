package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModificarComboUseCaseTest {

    private ComboRepositoryEnMemoria comboRepository;
    private ModificarComboUseCase useCase;
    private UUID vendedorId;
    private UUID comboId;

    @BeforeEach
    void setUp() {
        comboRepository = new ComboRepositoryEnMemoria();
        useCase = new ModificarComboUseCase(comboRepository);
        vendedorId = UUID.randomUUID();
        comboId = UUID.randomUUID();
        comboRepository.guardar(Combo.crear(comboId, vendedorId, "Combo Gamer", "Setup", 15.0,
                List.of(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void modificaYPersisteLosDatos() {
        Combo combo = useCase.ejecutar(comboId, vendedorId, "Combo Pro", "Setup pro", 30.0);

        assertEquals("Combo Pro", combo.getNombre());
        Combo guardado = comboRepository.obtenerPorId(comboId).get();
        assertEquals("Setup pro", guardado.getDescripcion());
        assertEquals(30.0, guardado.getDescuento());
        assertEquals(2, guardado.getProductos().size());
    }

    @Test
    void soloElDuenoPuedeModificar() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(comboId, UUID.randomUUID(), "Hack", "Hack", 50.0));

        assertEquals("Combo Gamer", comboRepository.obtenerPorId(comboId).get().getNombre());
    }

    @Test
    void datosInvalidosLanzanReglaDominioYNoCambianNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(comboId, vendedorId, "Nuevo", "Desc", 100.0));

        assertEquals(15.0, comboRepository.obtenerPorId(comboId).get().getDescuento());
    }

    @Test
    void comboInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, "Nombre", "Desc", 10.0));
    }
}
