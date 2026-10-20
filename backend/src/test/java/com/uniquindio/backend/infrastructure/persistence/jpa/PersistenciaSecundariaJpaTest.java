package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ComboPersistenceMapper;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.NotificacionPersistenceMapper;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.PreventaPersistenceMapper;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ProductoPersistenceMapper;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.RegaloPersistenceMapper;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.UsuarioPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({
        ComboRepositoryJpa.class, ComboPersistenceMapper.class,
        NotificacionRepositoryJpa.class, NotificacionPersistenceMapper.class,
        PreventaRepositoryJpa.class, PreventaPersistenceMapper.class,
        ProductoRepositoryJpa.class, ProductoPersistenceMapper.class,
        RegaloRepositoryJpa.class, RegaloPersistenceMapper.class,
        UsuarioRepositoryJpa.class, UsuarioPersistenceMapper.class
})
class PersistenciaSecundariaJpaTest {

    @Autowired
    private ComboRepository comboRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private PreventaRepository preventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private RegaloRepository regaloRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("ComboRepositoryJpa guarda y recupera el combo")
    void comboGuardarYLeer() {
        Combo combo = Combo.crear(UUID.randomUUID(), UUID.randomUUID(), "Pack",
                "Dos productos", 10, List.of(UUID.randomUUID(), UUID.randomUUID()));

        comboRepository.guardar(combo);
        entityManager.flush();
        entityManager.clear();

        Combo leido = comboRepository.obtenerPorId(combo.getId()).orElseThrow();
        assertEquals(combo.getNombre(), leido.getNombre());
        assertEquals(combo.getProductos(), leido.getProductos());
    }

    @Test
    @DisplayName("NotificacionRepositoryJpa guarda y lista por usuario")
    void notificacionGuardarYListar() {
        UUID usuarioId = UUID.randomUUID();
        Notificacion notificacion = Notificacion.reconstruir(UUID.randomUUID(), usuarioId,
                UUID.randomUUID(), TipoNotificacion.DESCUENTO, "Bajó", LocalDateTime.now(), false);

        notificacionRepository.guardar(notificacion);
        entityManager.flush();
        entityManager.clear();

        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(usuarioId).size());
    }

    @Test
    @DisplayName("PreventaRepositoryJpa rechaza una segunda preventa del mismo producto")
    void preventaUnicaPorProducto() {
        UUID productoId = UUID.randomUUID();
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), productoId,
                LocalDate.now().plusDays(10), "Primera"));
        entityManager.flush();

        assertThrows(RuntimeException.class, () -> {
            preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), productoId,
                    LocalDate.now().plusDays(20), "Segunda"));
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("ProductoRepositoryJpa conserva precioOriginal al releer un descuento")
    void productoConservaPrecioOriginal() {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse",
                "Inalámbrico", 100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        producto.aplicarDescuento(10);

        productoRepository.guardar(producto);
        entityManager.flush();
        entityManager.clear();

        Producto leido = productoRepository.obtenerPorId(producto.getId()).orElseThrow();
        assertEquals(90.0, leido.getPrecio(), 0.001);
        assertEquals(100.0, leido.getPrecioOriginal());
    }

    @Test
    @DisplayName("RegaloRepositoryJpa conserva fechaCreacion al releer")
    void regaloConservaFechaCreacion() {
        LocalDateTime fecha = LocalDateTime.of(2022, 11, 5, 9, 0);
        Regalo regalo = Regalo.reconstruir(UUID.randomUUID(), EstadoRegalo.DISPONIBLE, fecha,
                null, UUID.randomUUID(), null, null, false);

        regaloRepository.guardar(regalo);
        entityManager.flush();
        entityManager.clear();

        Regalo leido = regaloRepository.obtenerPorId(regalo.getId()).orElseThrow();
        assertEquals(fecha, leido.getFechaCreacion());
    }

    @Test
    @DisplayName("UsuarioEntity @Version detecta dos escrituras concurrentes")
    void lockOptimistaEnUsuario() {
        UUID id = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(id, "Gamer", "gamer@test.com");
        usuario.acumularPuntos(100);
        usuarioRepository.guardar(usuario);
        entityManager.flush();
        entityManager.clear();

        Usuario primera = usuarioRepository.obtenerPorId(id).orElseThrow();
        entityManager.clear();
        Usuario segunda = usuarioRepository.obtenerPorId(id).orElseThrow();

        primera.redimirPuntos(60);
        usuarioRepository.guardar(primera);
        entityManager.flush();
        entityManager.clear();

        segunda.redimirPuntos(60);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            usuarioRepository.guardar(segunda);
            entityManager.flush();
        });
    }
}
