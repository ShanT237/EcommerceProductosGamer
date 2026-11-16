package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarMisProductosUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ConsultarMisProductosUseCase useCase;
    private final UUID vendedorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ConsultarMisProductosUseCase(productoRepository);
    }

    private Producto guardar(UUID dueno, String nombre) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, nombre, "Descripcion",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void incluyeLosProductosPausadosDelVendedor() {
        Producto activo = guardar(vendedorId, "Activo");
        Producto pausado = guardar(vendedorId, "Pausado");
        pausado.pausar();
        productoRepository.guardar(pausado);

        List<Producto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(activo));
        assertTrue(resultado.contains(pausado));
        assertTrue(resultado.stream().filter(p -> p.equals(pausado)).findFirst().orElseThrow().isPausado());
    }

    @Test
    void excluyeLosProductosEliminados() {
        Producto vivo = guardar(vendedorId, "Vivo");
        Producto eliminado = guardar(vendedorId, "Eliminado");
        eliminado.eliminar(false);
        productoRepository.guardar(eliminado);

        List<Producto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(List.of(vivo), resultado);
    }

    @Test
    void noDevuelveProductosDeOtrosVendedores() {
        Producto mio = guardar(vendedorId, "Mio");
        guardar(UUID.randomUUID(), "Ajeno");

        assertEquals(List.of(mio), useCase.ejecutar(vendedorId));
    }

    @Test
    void ordenaPorNombreSinDistinguirMayusculas() {
        guardar(vendedorId, "teclado");
        guardar(vendedorId, "Auriculares");
        guardar(vendedorId, "Mouse");

        List<String> nombres = useCase.ejecutar(vendedorId).stream().map(Producto::getNombre).toList();

        assertEquals(List.of("Auriculares", "Mouse", "teclado"), nombres);
    }

    @Test
    void vendedorSinProductosDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(vendedorId).isEmpty());
    }
}
