package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarRegalosPorEnviarUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ConsultarRegalosPorEnviarUseCase useCase;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ConsultarRegalosPorEnviarUseCase(regaloRepository, productoRepository,
                new ComboRepositoryEnMemoria());
        vendedorId = UUID.randomUUID();
    }

    private Regalo regalo(UUID duenoDelProducto, boolean elegido) {
        Producto producto = Producto.publicar(UUID.randomUUID(), duenoDelProducto, "Mouse", "Desc", 100.0, 10,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        Regalo regalo = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        if (elegido) {
            regalo.elegir(UUID.randomUUID(), false);
        }
        regaloRepository.guardar(regalo);
        return regalo;
    }

    @Test
    void porEnviarSoloLosDelVendedor() {
        Regalo propio = regalo(vendedorId, true);
        regalo(UUID.randomUUID(), true); // elegido pero de otro vendedor

        List<Regalo> resultado = useCase.ejecutar(vendedorId);

        assertEquals(1, resultado.size());
        assertEquals(propio.getId(), resultado.get(0).getId());
    }

    @Test
    void soloIncluyeLosElegidosNoLosDisponiblesNiLosEnviados() {
        Regalo elegido = regalo(vendedorId, true);
        regalo(vendedorId, false); // sigue DISPONIBLE
        Regalo enviado = regalo(vendedorId, true);
        enviado.enviar();
        regaloRepository.guardar(enviado);

        List<Regalo> resultado = useCase.ejecutar(vendedorId);

        assertEquals(1, resultado.size());
        assertEquals(elegido.getId(), resultado.get(0).getId());
    }

    @Test
    void vendedorSinRegalosPorEnviarDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }
}
