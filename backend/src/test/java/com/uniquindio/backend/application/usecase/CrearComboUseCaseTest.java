package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrearComboUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private CrearComboUseCase useCase;
    private Vendedor vendedor;
    private Producto p1;
    private Producto p2;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        useCase = new CrearComboUseCase(vendedorRepository, productoRepository, comboRepository);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123");
        vendedorRepository.guardar(vendedor);
        p1 = producto(vendedor.getId(), "Teclado");
        p2 = producto(vendedor.getId(), "Mouse");
        productoRepository.guardar(p1);
        productoRepository.guardar(p2);
    }

    private Producto producto(UUID vendedorId, String nombre) {
        return Producto.publicar(UUID.randomUUID(), vendedorId, nombre, "Desc", 100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    void creaComboConProductosDelVendedor() {
        UUID id = UUID.randomUUID();

        Combo combo = useCase.ejecutar(id, vendedor.getId(), "Combo Gamer", "Teclado + Mouse", 15.0,
                List.of(p1.getId(), p2.getId()));

        assertEquals(2, combo.getProductos().size());
        assertTrue(comboRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void productoInexistenteLanzaReglaDominioYNoGuarda() {
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), "Combo", "Desc", 10.0,
                List.of(p1.getId(), UUID.randomUUID())));

        assertTrue(comboRepository.obtenerPorId(id).isEmpty());
    }

    @Test
    void productoDeOtroVendedorLanzaReglaDominioYNoGuarda() {
        UUID id = UUID.randomUUID();
        Producto ajeno = producto(UUID.randomUUID(), "Ajeno");
        productoRepository.guardar(ajeno);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), "Combo", "Desc", 10.0,
                List.of(p1.getId(), ajeno.getId())));

        assertTrue(comboRepository.obtenerPorId(id).isEmpty());
    }

    @Test
    void vendedorDadoDeBajaLanzaReglaDominio() {
        vendedor.darDeBaja();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "Combo",
                "Desc", 10.0, List.of(p1.getId(), p2.getId())));
    }

    @Test
    void vendedorInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), UUID.randomUUID(),
                "Combo", "Desc", 10.0, List.of(p1.getId(), p2.getId())));
    }
}
