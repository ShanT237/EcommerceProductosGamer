package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModificarProductosComboUseCaseTest {

    private ComboRepositoryEnMemoria comboRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ModificarProductosComboUseCase useCase;
    private UUID vendedorId;
    private UUID comboId;
    private Producto p1;
    private Producto p2;
    private Producto p3;

    @BeforeEach
    void setUp() {
        comboRepository = new ComboRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ModificarProductosComboUseCase(comboRepository, productoRepository);

        vendedorId = UUID.randomUUID();
        p1 = guardarProducto(vendedorId, "Teclado");
        p2 = guardarProducto(vendedorId, "Mouse");
        p3 = guardarProducto(vendedorId, "Headset");
        comboId = UUID.randomUUID();
        comboRepository.guardar(Combo.crear(comboId, vendedorId, "Combo Gamer", "Setup", 15.0,
                List.of(p1.getId(), p2.getId())));
    }

    private Producto guardarProducto(UUID duenoId, String nombre) {
        Producto p = Producto.publicar(UUID.randomUUID(), duenoId, nombre, "Desc", 100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void agregaProductoDelMismoVendedor() {
        Combo combo = useCase.agregar(comboId, vendedorId, p3.getId());

        assertEquals(3, combo.getProductos().size());
        assertEquals(3, comboRepository.obtenerPorId(comboId).get().getProductos().size());
    }

    @Test
    void agregarProductoDeOtroVendedorLanzaReglaDominio() {
        Producto ajeno = guardarProducto(UUID.randomUUID(), "Ajeno");

        assertThrows(ReglaDominioException.class, () -> useCase.agregar(comboId, vendedorId, ajeno.getId()));

        assertEquals(2, comboRepository.obtenerPorId(comboId).get().getProductos().size());
    }

    @Test
    void agregarProductoInexistenteLanzaReglaDominio() {
        assertThrows(ReglaDominioException.class, () -> useCase.agregar(comboId, vendedorId, UUID.randomUUID()));
    }

    @Test
    void quitaProductoSiQuedanAlMenosDos() {
        useCase.agregar(comboId, vendedorId, p3.getId());

        Combo combo = useCase.quitar(comboId, vendedorId, p3.getId());

        assertEquals(2, combo.getProductos().size());
    }

    @Test
    void quitarDejandoMenosDeDosLanzaReglaDominio() {
        assertThrows(ReglaDominioException.class, () -> useCase.quitar(comboId, vendedorId, p1.getId()));

        assertEquals(2, comboRepository.obtenerPorId(comboId).get().getProductos().size());
    }

    @Test
    void soloElDuenoDelComboModificaSusProductos() {
        UUID ajeno = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> useCase.agregar(comboId, ajeno, p3.getId()));
        assertThrows(ReglaDominioException.class, () -> useCase.quitar(comboId, ajeno, p1.getId()));

        assertEquals(2, comboRepository.obtenerPorId(comboId).get().getProductos().size());
    }

    @Test
    void comboInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.agregar(UUID.randomUUID(), vendedorId, p3.getId()));
        assertThrows(NoSuchElementException.class,
                () -> useCase.quitar(UUID.randomUUID(), vendedorId, p1.getId()));
    }

    @Test
    void agregarProductoYaPresenteLanzaConflicto() {
        assertThrows(ConflictoException.class, () -> useCase.agregar(comboId, vendedorId, p1.getId()));

        assertEquals(2, comboRepository.obtenerPorId(comboId).get().getProductos().size());
    }
}
