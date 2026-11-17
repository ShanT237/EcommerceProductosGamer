package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelarRegaloUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private RegaloRepositoryEnMemoria regaloRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private NotificacionRepositoryEnMemoria notificaciones;
    private CancelarRegaloUseCase useCase;
    private UUID vendedorId;
    private UUID destinatarioId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        notificaciones = new NotificacionRepositoryEnMemoria();
        useCase = new CancelarRegaloUseCase(regaloRepository, productoRepository, comboRepository,
                TestNotificador.con(notificaciones, new WishlistRepositoryEnMemoria()));
        vendedorId = UUID.randomUUID();
        destinatarioId = UUID.randomUUID();
        producto = producto(vendedorId);
    }

    private Producto producto(UUID dueno) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, "Mouse", "Desc", 100.0, 10,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private Regalo regaloDisponible() {
        Regalo r = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        regaloRepository.guardar(r);
        return r;
    }

    private Regalo regaloElegido() {
        Regalo r = regaloDisponible();
        r.elegir(destinatarioId, false);
        regaloRepository.guardar(r);
        return r;
    }

    private EstadoRegalo estado(Regalo r) {
        return regaloRepository.obtenerPorId(r.getId()).orElseThrow().getEstado();
    }

    private long avisosDeCancelacion() {
        return notificaciones.obtenerPorUsuarioId(destinatarioId).stream()
                .filter(n -> n.getTipo() == TipoNotificacion.REGALO_CANCELADO).count();
    }

    // Review Focus 4: un regalo DISPONIBLE no tiene comprador y no genera aviso.
    @Test
    void vendedorCancelaUnRegaloDisponibleSinAvisarANadie() {
        Regalo regalo = regaloDisponible();

        assertEquals(EstadoRegalo.CANCELADO, useCase.ejecutar(regalo.getId(), vendedorId).getEstado());

        assertEquals(EstadoRegalo.CANCELADO, estado(regalo));
        assertTrue(notificaciones.obtenerPorUsuarioId(destinatarioId).isEmpty());
        assertTrue(notificaciones.obtenerPorUsuarioId(vendedorId).isEmpty());
    }

    @Test
    void cancelarUnRegaloElegidoAvisaAlCompradorYLeDevuelveElCupo() {
        for (int i = 0; i < 5; i++) {
            Compra c = Compra.realizar(UUID.randomUUID(), destinatarioId, UUID.randomUUID(), 1, FECHA, new Precio(100));
            c.confirmar();
            compraRepository.guardar(c);
        }
        ConsultarProgresoRegalosUseCase progreso = new ConsultarProgresoRegalosUseCase(compraRepository, regaloRepository);
        Regalo regalo = regaloElegido();
        assertEquals(0, progreso.ejecutar(destinatarioId).regalosDisponibles());

        useCase.ejecutar(regalo.getId(), vendedorId);

        assertEquals(TipoNotificacion.REGALO_CANCELADO,
                notificaciones.obtenerPorUsuarioId(destinatarioId).get(0).getTipo());
        assertEquals(1, progreso.ejecutar(destinatarioId).regalosDisponibles());
    }

    @Test
    void cancelarUnRegaloEnviadoAvisaAlComprador() {
        Regalo regalo = regaloElegido();
        regalo.enviar();
        regaloRepository.guardar(regalo);

        useCase.ejecutar(regalo.getId(), vendedorId);

        assertEquals(EstadoRegalo.CANCELADO, estado(regalo));
        assertEquals(1, avisosDeCancelacion());
    }

    @Test
    void unRegaloEntregadoNoSePuedeCancelar() {
        Regalo regalo = regaloElegido();
        regalo.enviar();
        regalo.confirmarEntrega(destinatarioId);
        regaloRepository.guardar(regalo);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regalo.getId(), vendedorId));

        assertEquals(EstadoRegalo.ENTREGADO, estado(regalo));
        assertEquals(0, avisosDeCancelacion());
    }

    @Test
    void cancelarDosVecesRechaza() {
        Regalo regalo = regaloElegido();
        useCase.ejecutar(regalo.getId(), vendedorId);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regalo.getId(), vendedorId));

        assertEquals(1, avisosDeCancelacion());   // el segundo intento no duplica el aviso
    }

    @Test
    void vendedorAjenoRechazaYNoCambia() {
        Regalo regalo = regaloElegido();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(regalo.getId(), UUID.randomUUID()));

        assertEquals(EstadoRegalo.ELEGIDO, estado(regalo));
        assertEquals(0, avisosDeCancelacion());
    }

    @Test
    void regaloDeComboLoCancelaElVendedorDelCombo() {
        Combo combo = Combo.crear(UUID.randomUUID(), vendedorId, "Combo", "Setup", 10.0,
                List.of(producto(vendedorId).getId(), producto(vendedorId).getId()));
        comboRepository.guardar(combo);
        Regalo regaloDeCombo = Regalo.crear(UUID.randomUUID(), null, combo.getId());
        regaloRepository.guardar(regaloDeCombo);

        assertEquals(EstadoRegalo.CANCELADO, useCase.ejecutar(regaloDeCombo.getId(), vendedorId).getEstado());
    }

    @Test
    void regaloInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));
    }
}
