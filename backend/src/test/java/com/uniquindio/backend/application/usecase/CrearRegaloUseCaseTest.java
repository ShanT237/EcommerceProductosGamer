package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrearRegaloUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private RegaloRepositoryEnMemoria regaloRepository;
    private CrearRegaloUseCase useCase;
    private Vendedor vendedor;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        regaloRepository = new RegaloRepositoryEnMemoria();
        useCase = new CrearRegaloUseCase(vendedorRepository, productoRepository, comboRepository, regaloRepository);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123");
        vendedorRepository.guardar(vendedor);
    }

    private Producto producto(UUID dueno) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, "Teclado", "Desc", 100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void creaRegaloDeProductoPropio() {
        Producto producto = producto(vendedor.getId());
        UUID id = UUID.randomUUID();

        Regalo regalo = useCase.ejecutar(id, vendedor.getId(), producto.getId(), null);

        assertEquals(EstadoRegalo.DISPONIBLE, regalo.getEstado());
        assertEquals(producto.getId(), regalo.getIdProducto());
        assertTrue(regaloRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void creaRegaloDeComboPropio() {
        Combo combo = Combo.crear(UUID.randomUUID(), vendedor.getId(), "Combo", "Desc", 10.0,
                List.of(producto(vendedor.getId()).getId(), producto(vendedor.getId()).getId()));
        comboRepository.guardar(combo);
        UUID id = UUID.randomUUID();

        Regalo regalo = useCase.ejecutar(id, vendedor.getId(), null, combo.getId());

        assertEquals(combo.getId(), regalo.getIdCombo());
        assertNull(regalo.getIdProducto());
        assertTrue(regaloRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void productoAjenoRechaza() {
        Producto ajeno = producto(UUID.randomUUID());
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), ajeno.getId(), null));

        assertTrue(regaloRepository.obtenerPorId(id).isEmpty());
    }

    @Test
    void comboAjenoRechaza() {
        UUID otroVendedor = UUID.randomUUID();
        Combo combo = Combo.crear(UUID.randomUUID(), otroVendedor, "Combo", "Desc", 10.0,
                List.of(producto(otroVendedor).getId(), producto(otroVendedor).getId()));
        comboRepository.guardar(combo);
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), null, combo.getId()));

        assertTrue(regaloRepository.obtenerPorId(id).isEmpty());
    }

    @Test
    void productoEliminadoRechaza() {
        Producto producto = producto(vendedor.getId());
        producto.eliminar(false);
        productoRepository.guardar(producto);
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), producto.getId(), null));

        assertTrue(regaloRepository.obtenerPorId(id).isEmpty());
    }

    @Test
    void productoInexistenteRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), UUID.randomUUID(), null));
    }

    @Test
    void sinProductoNiComboRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), null, null));
    }

    @Test
    void conProductoYComboAlMismoTiempoRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void vendedorDadoDeBajaRechaza() {
        Producto producto = producto(vendedor.getId());
        vendedor.darDeBaja();
        vendedorRepository.guardar(vendedor);
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, vendedor.getId(), producto.getId(), null));

        assertTrue(regaloRepository.obtenerPorId(id).isEmpty());
    }
}
