package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AsignarRegaloPorMetaUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private RegaloRepositoryEnMemoria regaloRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private AsignarRegaloPorMetaUseCase useCase;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        regaloRepository = new RegaloRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        useCase = new AsignarRegaloPorMetaUseCase(usuarioRepository, compraRepository,
                regaloRepository, comboRepository);

        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "gamer@test.com"));
    }

    /** Crea n compras COMPLETADAS de productos distintos; devuelve el id del primer producto. */
    private UUID comprasCompletadas(int n) {
        UUID primero = null;
        for (int i = 0; i < n; i++) {
            UUID productoId = UUID.randomUUID();
            if (primero == null) primero = productoId;
            Compra c = Compra.realizar(UUID.randomUUID(), usuarioId, productoId, 1, FECHA, new Precio(100));
            c.confirmar();
            compraRepository.guardar(c);
        }
        return primero;
    }

    private Regalo regaloDeProducto(UUID productoId) {
        Regalo r = Regalo.crear(UUID.randomUUID(), productoId, null);
        regaloRepository.guardar(r);
        return r;
    }

    private Regalo regaloDisponible() {
        return regaloDeProducto(UUID.randomUUID());
    }

    @Test
    @DisplayName("Con 5 compras completadas se asigna el regalo y queda ELEGIDO para el usuario")
    void asignaAlAlcanzarLaMeta() {
        comprasCompletadas(5);
        Regalo regalo = regaloDisponible();

        Regalo r = useCase.ejecutar(usuarioId, regalo.getId());

        assertEquals(EstadoRegalo.ELEGIDO, r.getEstado());
        assertEquals(usuarioId, r.getIdUsuario());
        assertEquals(EstadoRegalo.ELEGIDO, regaloRepository.obtenerPorId(regalo.getId()).get().getEstado());
    }

    @Test
    @DisplayName("Con 4 compras no alcanza la meta y el regalo sigue DISPONIBLE")
    void rechazaSiNoAlcanzaLaMeta() {
        comprasCompletadas(4);
        Regalo regalo = regaloDisponible();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regalo.getId()));
        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(regalo.getId()).get().getEstado());
    }

    @Test
    @DisplayName("Las compras PENDIENTES o CANCELADAS no cuentan para la meta")
    void soloCuentanLasCompletadas() {
        comprasCompletadas(4);
        compraRepository.guardar(Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1, FECHA, new Precio(100)));
        Compra cancelada = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1, FECHA, new Precio(100));
        cancelada.cancelar();
        compraRepository.guardar(cancelada);
        Regalo regalo = regaloDisponible();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regalo.getId()));
    }

    @Test
    @DisplayName("Un segundo regalo exige otras 5 compras: con 5 solo hay derecho a uno")
    void noPermiteSegundoRegaloSinNuevaMeta() {
        comprasCompletadas(5);
        useCase.ejecutar(usuarioId, regaloDisponible().getId());
        Regalo segundo = regaloDisponible();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, segundo.getId()));
        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(segundo.getId()).get().getEstado());
    }

    @Test
    @DisplayName("Con 10 compras completadas se permiten dos regalos")
    void diezComprasPermitenDosRegalos() {
        comprasCompletadas(10);

        useCase.ejecutar(usuarioId, regaloDisponible().getId());

        assertDoesNotThrow(() -> useCase.ejecutar(usuarioId, regaloDisponible().getId()));
    }

    @Test
    @DisplayName("Un regalo CANCELADO no cuenta como regalo ya asignado")
    void regaloCanceladoLiberaElDerecho() {
        comprasCompletadas(5);
        Regalo primero = useCase.ejecutar(usuarioId, regaloDisponible().getId());
        primero.cancelarRegalo();
        regaloRepository.guardar(primero);

        assertDoesNotThrow(() -> useCase.ejecutar(usuarioId, regaloDisponible().getId()));
    }

    @Test
    @DisplayName("Rechaza el regalo si el usuario ya posee ese producto")
    void rechazaSiYaPoseeElProducto() {
        UUID yaComprado = comprasCompletadas(5);
        Regalo regalo = regaloDeProducto(yaComprado);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regalo.getId()));
        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(regalo.getId()).get().getEstado());
    }

    @Test
    @DisplayName("Rechaza un regalo-combo si el usuario ya posee alguno de sus productos")
    void rechazaComboConProductoYaPoseido() {
        UUID yaComprado = comprasCompletadas(5);
        Combo combo = Combo.crear(UUID.randomUUID(), UUID.randomUUID(), "Combo", "Setup", 10.0,
                List.of(yaComprado, UUID.randomUUID()));
        comboRepository.guardar(combo);
        Regalo regalo = Regalo.crear(UUID.randomUUID(), null, combo.getId());
        regaloRepository.guardar(regalo);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regalo.getId()));
    }

    @Test
    @DisplayName("Rechaza un regalo que ya no está DISPONIBLE")
    void rechazaRegaloYaElegido() {
        comprasCompletadas(5);
        Regalo regalo = regaloDisponible();
        regalo.cancelarRegalo();
        regaloRepository.guardar(regalo);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regalo.getId()));
    }

    @Test
    @DisplayName("Usuario inactivo no recibe regalos")
    void rechazaUsuarioInactivo() {
        comprasCompletadas(5);
        usuarioRepository.obtenerPorId(usuarioId).get().desactivar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, regaloDisponible().getId()));
    }

    @Test
    @DisplayName("Usuario o regalo inexistente lanza NoSuchElementException")
    void inexistentes() {
        comprasCompletadas(5);

        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), regaloDisponible().getId()));
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(usuarioId, UUID.randomUUID()));
    }
}