package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ComboEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.NotificacionEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PreventaEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.RegaloEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMapperRoundTripTest {

    private final ComboPersistenceMapper comboMapper = new ComboPersistenceMapper();
    private final NotificacionPersistenceMapper notificacionMapper = new NotificacionPersistenceMapper();
    private final PreventaPersistenceMapper preventaMapper = new PreventaPersistenceMapper();
    private final ProductoPersistenceMapper productoMapper = new ProductoPersistenceMapper();
    private final RegaloPersistenceMapper regaloMapper = new RegaloPersistenceMapper();
    private final UsuarioPersistenceMapper usuarioMapper = new UsuarioPersistenceMapper();

    @Test
    @DisplayName("Combo: toEntity → toDomain conserva id, descuento y productos")
    void comboIdaYVuelta() {
        Combo original = Combo.crear(UUID.randomUUID(), UUID.randomUUID(), "Combo RGB",
                "Teclado y mouse", 15, List.of(UUID.randomUUID(), UUID.randomUUID()));

        ComboEntity entity = comboMapper.toEntity(original);
        Combo reconstruido = comboMapper.toDomain(entity);

        assertEquals(original.getId(), reconstruido.getId());
        assertEquals(original.getVendedorId(), reconstruido.getVendedorId());
        assertEquals(original.getNombre(), reconstruido.getNombre());
        assertEquals(original.getDescuento(), reconstruido.getDescuento());
        assertEquals(original.getProductos(), reconstruido.getProductos());
    }

    @Test
    @DisplayName("Notificacion: toEntity → toDomain conserva fecha y leída")
    void notificacionIdaYVuelta() {
        LocalDateTime fecha = LocalDateTime.of(2024, 6, 1, 12, 0);
        Notificacion original = Notificacion.reconstruir(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), TipoNotificacion.DESCUENTO, "Bajó de precio", fecha, true);

        NotificacionEntity entity = notificacionMapper.toEntity(original);
        Notificacion reconstruida = notificacionMapper.toDomain(entity);

        assertEquals(original.getId(), reconstruida.getId());
        assertEquals(original.getUsuarioId(), reconstruida.getUsuarioId());
        assertEquals(original.getProductoId(), reconstruida.getProductoId());
        assertEquals(original.getTipo(), reconstruida.getTipo());
        assertEquals(original.getMensaje(), reconstruida.getMensaje());
        assertEquals(fecha, reconstruida.getFecha());
        assertTrue(reconstruida.isLeida());
    }

    @Test
    @DisplayName("Preventa: toEntity → toDomain conserva producto y fecha de lanzamiento")
    void preventaIdaYVuelta() {
        Preventa original = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 12, 1), "Edición coleccionista");

        PreventaEntity entity = preventaMapper.toEntity(original);
        Preventa reconstruida = preventaMapper.toDomain(entity);

        assertEquals(original.getId(), reconstruida.getId());
        assertEquals(original.getProductoId(), reconstruida.getProductoId());
        assertEquals(original.getFechaLanzamiento(), reconstruida.getFechaLanzamiento());
        assertEquals(original.getDescripcion(), reconstruida.getDescripcion());
    }

    @Test
    @DisplayName("Producto: toEntity → toDomain conserva precioOriginal tras un descuento")
    void productoIdaYVueltaPreservaPrecioOriginal() {
        Producto original = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse",
                "Inalámbrico", 100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        original.aplicarDescuento(10);

        ProductoEntity entity = productoMapper.toEntity(original);
        Producto reconstruido = productoMapper.toDomain(entity);

        assertEquals(90.0, reconstruido.getPrecio(), 0.001);
        assertEquals(100.0, reconstruido.getPrecioOriginal());
        assertEquals(original.getId(), reconstruido.getId());
    }

    @Test
    @DisplayName("Regalo: toEntity → toDomain no pisa fechaCreacion con now()")
    void regaloIdaYVueltaPreservaFechaCreacion() {
        LocalDateTime fechaCreacion = LocalDateTime.of(2023, 3, 10, 8, 15);
        LocalDateTime fechaEntrega = LocalDateTime.of(2023, 3, 20, 18, 0);
        UUID idUsuario = UUID.randomUUID();
        Regalo original = Regalo.reconstruir(UUID.randomUUID(), EstadoRegalo.ENTREGADO, fechaCreacion,
                fechaEntrega, UUID.randomUUID(), idUsuario, null, true);

        RegaloEntity entity = regaloMapper.toEntity(original);
        Regalo reconstruido = regaloMapper.toDomain(entity);

        assertEquals(fechaCreacion, reconstruido.getFechaCreacion());
        assertEquals(fechaEntrega, reconstruido.getFechaEntrega());
        assertEquals(EstadoRegalo.ENTREGADO, reconstruido.getEstado());
        assertEquals(idUsuario, reconstruido.getIdUsuario());
        assertTrue(reconstruido.isNotificado());
    }

    @Test
    @DisplayName("Usuario: toEntity → toDomain conserva puntos, activo y version")
    void usuarioIdaYVueltaPreservaVersion() {
        Usuario original = Usuario.reconstruir(UUID.randomUUID(), "Gamer", "gamer@test.com",
                40, true, 3L);

        UsuarioEntity entity = usuarioMapper.toEntity(original);
        Usuario reconstruido = usuarioMapper.toDomain(entity);

        assertEquals(original.getId(), reconstruido.getId());
        assertEquals(40, reconstruido.getPuntos());
        assertTrue(reconstruido.isActivo());
        assertEquals(3L, reconstruido.getVersion());
    }
}
