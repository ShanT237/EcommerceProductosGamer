package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoProducto;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ProductoPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({ProductoRepositoryJpa.class, ProductoPersistenceMapper.class})
class ProductoTipoPersistenciaTest {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void conservaElTipoAlLeer() {
        UUID id = UUID.randomUUID();
        productoRepository.guardar(Producto.publicar(id, UUID.randomUUID(), "Juego", "Juego digital",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL, "General", TipoProducto.DIGITAL));
        entityManager.flush();
        entityManager.clear();

        assertEquals(TipoProducto.DIGITAL, productoRepository.obtenerPorId(id).orElseThrow().getTipo());
    }

    @Test
    void filaSinTipoSeLeeComoFisico() {
        UUID id = UUID.randomUUID();
        ProductoEntity fila = new ProductoEntity();
        fila.setId(id);
        fila.setVendedorId(UUID.randomUUID());
        fila.setNombre("Mouse");
        fila.setDescripcion("Mouse RGB");
        fila.setPrecio(100);
        fila.setStock(5);
        fila.setGama(Gama.MEDIA);
        fila.setExclusividad(Exclusividad.NORMAL);
        fila.setTipo(null);
        entityManager.persist(fila);
        entityManager.flush();
        entityManager.clear();

        assertEquals(TipoProducto.FISICO, productoRepository.obtenerPorId(id).orElseThrow().getTipo());
    }
}
