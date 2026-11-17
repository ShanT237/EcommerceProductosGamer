package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EnviarYConfirmarRegaloUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private NotificacionRepositoryEnMemoria notificaciones;
    private EnviarRegaloUseCase enviar;
    private ConfirmarEntregaRegaloUseCase confirmar;
    private UUID id;
    private UUID destinatarioId;
    private UUID vendedorId;
    private UUID productoId;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        notificaciones = new NotificacionRepositoryEnMemoria();
        enviar = new EnviarRegaloUseCase(regaloRepository, productoRepository, new ComboRepositoryEnMemoria(),
                TestNotificador.con(notificaciones, new WishlistRepositoryEnMemoria()));
        confirmar = new ConfirmarEntregaRegaloUseCase(regaloRepository);
        id = UUID.randomUUID();
        destinatarioId = UUID.randomUUID();
        vendedorId = UUID.randomUUID();
        productoId = UUID.randomUUID();
        // El regalo apunta a un producto del vendedor: él es el único que puede enviarlo
        productoRepository.guardar(Producto.publicar(productoId, vendedorId, "Mouse", "Desc", 100.0, 10,
                Gama.MEDIA, Exclusividad.NORMAL));
    }

    private void guardarRegaloElegido() {
        Regalo regalo = Regalo.crear(id, productoId, null);
        regalo.elegir(destinatarioId, false);
        regaloRepository.guardar(regalo);
    }

    @Test
    void alEnviarQuedaNotificadoYSeAvisaAlComprador() {
        guardarRegaloElegido();

        Regalo r = enviar.ejecutar(id, vendedorId);

        assertTrue(r.isNotificado());
        assertEquals(EstadoRegalo.ENVIADO, r.getEstado());
        assertEquals(TipoNotificacion.REGALO_ENVIADO, notificaciones.obtenerPorUsuarioId(destinatarioId).get(0).getTipo());
    }

    @Test
    void enviarUnRegaloYaEnviadoRechazaSinCambiarElEstado() {
        guardarRegaloElegido();
        enviar.ejecutar(id, vendedorId);

        assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(id, vendedorId));

        assertEquals(EstadoRegalo.ENVIADO, regaloRepository.obtenerPorId(id).get().getEstado());
        assertEquals(1, notificaciones.obtenerPorUsuarioId(destinatarioId).size());   // sin aviso duplicado
    }

    @Test
    void flujoElegidoEnviadoEntregado() {
        guardarRegaloElegido();

        assertEquals(EstadoRegalo.ENVIADO, enviar.ejecutar(id, vendedorId).getEstado());
        Regalo r = confirmar.ejecutar(id, destinatarioId);

        assertEquals(EstadoRegalo.ENTREGADO, r.getEstado());
        assertNotNull(r.getFechaEntrega());
        assertEquals(EstadoRegalo.ENTREGADO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void enviarRegaloDisponibleLanzaReglaDominio() {
        regaloRepository.guardar(Regalo.crear(id, productoId, null));

        assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(id, vendedorId));
        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void confirmarSinEnviarLanzaReglaDominio() {
        guardarRegaloElegido();

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(id, destinatarioId));
        assertEquals(EstadoRegalo.ELEGIDO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void confirmarPorUsuarioQueNoEsElDestinatarioLanzaReglaDominio() {
        guardarRegaloElegido();
        enviar.ejecutar(id, vendedorId);

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(id, UUID.randomUUID()));
        assertEquals(EstadoRegalo.ENVIADO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void regaloInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> enviar.ejecutar(UUID.randomUUID(), vendedorId));
        assertThrows(NoSuchElementException.class, () -> confirmar.ejecutar(UUID.randomUUID(), destinatarioId));
    }

    @Test
    void vendedorAjenoRechazaYNoCambia() {
        guardarRegaloElegido();

        assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(id, UUID.randomUUID()));

        assertEquals(EstadoRegalo.ELEGIDO, regaloRepository.obtenerPorId(id).get().getEstado());
    }
}
