package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuenoDeRegaloTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        vendedorId = UUID.randomUUID();
    }

    private Producto producto(UUID dueno) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, "Mouse", "Desc", 100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void regaloDeProductoPropioEsDelVendedor() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), producto(vendedorId).getId(), null);

        assertTrue(DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository));
    }

    @Test
    void regaloDeProductoAjenoNoEsDelVendedor() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), producto(UUID.randomUUID()).getId(), null);

        assertFalse(DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository));
    }

    @Test
    void regaloDeComboSeResuelvePorElVendedorDelCombo() {
        Combo combo = Combo.crear(UUID.randomUUID(), vendedorId, "Combo", "Desc", 10.0,
                List.of(producto(vendedorId).getId(), producto(vendedorId).getId()));
        comboRepository.guardar(combo);
        Regalo regalo = Regalo.crear(UUID.randomUUID(), null, combo.getId());

        assertTrue(DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository));
        assertFalse(DuenoDeRegalo.esDelVendedor(regalo, UUID.randomUUID(), productoRepository, comboRepository));
    }

    @Test
    void siElProductoYaNoExisteNoEsDeNadie() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);

        assertFalse(DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository));
    }

    @Test
    void vendedorDeProductoYDeComboSeResuelve() {
        Regalo deProducto = Regalo.crear(UUID.randomUUID(), producto(vendedorId).getId(), null);
        Combo combo = Combo.crear(UUID.randomUUID(), vendedorId, "Combo", "Desc", 10.0,
                List.of(producto(vendedorId).getId(), producto(vendedorId).getId()));
        comboRepository.guardar(combo);
        Regalo deCombo = Regalo.crear(UUID.randomUUID(), null, combo.getId());

        assertEquals(Optional.of(vendedorId), DuenoDeRegalo.vendedorDe(deProducto, productoRepository, comboRepository));
        assertEquals(Optional.of(vendedorId), DuenoDeRegalo.vendedorDe(deCombo, productoRepository, comboRepository));
    }

    @Test
    void vendedorDeUnProductoInexistenteEsVacio() {
        Regalo sinProducto = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);

        assertTrue(DuenoDeRegalo.vendedorDe(sinProducto, productoRepository, comboRepository).isEmpty());
    }

    @Test
    void nombreDeDevuelveElDelProductoElDelComboOElGenerico() {
        Regalo deProducto = Regalo.crear(UUID.randomUUID(), producto(vendedorId).getId(), null);
        Combo combo = Combo.crear(UUID.randomUUID(), vendedorId, "Combo", "Desc", 10.0,
                List.of(producto(vendedorId).getId(), producto(vendedorId).getId()));
        comboRepository.guardar(combo);
        Regalo deCombo = Regalo.crear(UUID.randomUUID(), null, combo.getId());
        Regalo sinProducto = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);

        assertEquals("Mouse", DuenoDeRegalo.nombreDe(deProducto, productoRepository, comboRepository));
        assertEquals("Combo", DuenoDeRegalo.nombreDe(deCombo, productoRepository, comboRepository));
        assertEquals("regalo", DuenoDeRegalo.nombreDe(sinProducto, productoRepository, comboRepository));
    }
}
