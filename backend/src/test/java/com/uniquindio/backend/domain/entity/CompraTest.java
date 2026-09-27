package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Precio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompraTest {

    private static final LocalDate FECHA_COMPRA = LocalDate.of(2026, 9, 17);

    private Compra crearCompraValida() {
        return new Compra(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, FECHA_COMPRA, new Precio(50000));
    }

    /**
     * Atajo para los tests que necesitan una compra ya COMPLETADA
     * (la mayoría de las reglas de reseña/reembolso dependen de ese estado).
     */
    private Compra crearCompraCompletada() {
        Compra compra = crearCompraValida();
        compra.confirmar();
        return compra;
    }

    @Test
    @DisplayName("Crea una compra válida con su precio unitario, en estado PENDIENTE, y falla con datos inválidos")
    void creaCompraValidaYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Precio precio = new Precio(50000);

        Compra compra = new Compra(id, usuarioId, productoId, 3, FECHA_COMPRA, precio);

        assertEquals(id, compra.getId());
        assertEquals(usuarioId, compra.getUsuarioId());
        assertEquals(productoId, compra.getProductoId());
        assertEquals(3, compra.getCantidad());
        assertEquals(FECHA_COMPRA, compra.getFecha());
        assertEquals(precio, compra.getPrecioUnitario());
        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());
        assertFalse(compra.isResenada());
        assertFalse(compra.isPuntosAsignados());
        assertFalse(compra.isDescargado());

        assertThrows(ReglaDominioException.class,
                () -> new Compra(id, usuarioId, productoId, 0, FECHA_COMPRA, precio));
        assertThrows(ReglaDominioException.class,
                () -> new Compra(id, usuarioId, productoId, -1, FECHA_COMPRA, precio));
        assertThrows(NullPointerException.class,
                () -> new Compra(id, usuarioId, productoId, 1, null, precio));
        assertThrows(NullPointerException.class,
                () -> new Compra(id, usuarioId, productoId, 1, FECHA_COMPRA, null));
    }

    @Test
    @DisplayName("El subtotal se calcula con el precio guardado en la compra, no con el actual del producto")
    void calculaSubtotalConElPrecioGuardado() {
        Compra compra = new Compra(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                3, FECHA_COMPRA, new Precio(10000));

        assertEquals(new Precio(30000), compra.getSubtotal());
    }

    // ===================== confirmar() =====================

    @Test
    @DisplayName("Confirma una compra pendiente y pasa a COMPLETADA")
    void confirmarCompraPendiente() {
        Compra compra = crearCompraValida();

        compra.confirmar();

        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
    }

    @Test
    @DisplayName("Rechaza confirmar una compra que ya está COMPLETADA")
    void rechazaConfirmarCompraYaCompletada() {
        Compra compra = crearCompraCompletada();

        assertThrows(ReglaDominioException.class, compra::confirmar);
    }

    @Test
    @DisplayName("Rechaza confirmar una compra que ya fue REEMBOLSADA")
    void rechazaConfirmarCompraReembolsada() {
        Compra compra = crearCompraCompletada();
        compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2);

        assertThrows(ReglaDominioException.class, compra::confirmar);
    }

    // ===================== marcarDescargado() =====================

    @Test
    @DisplayName("Marca como descargada una compra completada")
    void marcarDescargadoCompraCompletada() {
        Compra compra = crearCompraCompletada();

        compra.marcarDescargado();

        assertTrue(compra.isDescargado());
    }

    @Test
    @DisplayName("Rechaza marcar como descargada una compra que sigue PENDIENTE")
    void rechazaMarcarDescargadoSiNoEstaCompletada() {
        Compra compra = crearCompraValida();

        assertThrows(ReglaDominioException.class, compra::marcarDescargado);
        assertFalse(compra.isDescargado());
    }

    // ===================== solicitarReembolso() =====================

    @Test
    @DisplayName("Reembolsa una compra completada dentro del plazo y sin descargar")
    void reembolsaDentroDePlazoYSinDescargar() {
        Compra compra = crearCompraCompletada();

        compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2);

        assertEquals(EstadoCompra.REEMBOLSADA, compra.getEstado());
    }

    @Test
    @DisplayName("Rechaza reembolsar una compra que sigue PENDIENTE (no confirmada)")
    void rechazaReembolsarCompraPendiente() {
        Compra compra = crearCompraValida();

        assertThrows(ReglaDominioException.class,
                () -> compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2));
    }

    @Test
    @DisplayName("Rechaza reembolsar una compra cuyo archivo ya fue descargado")
    void rechazaReembolsarSiYaFueDescargado() {
        Compra compra = crearCompraCompletada();
        compra.marcarDescargado();

        assertThrows(ReglaDominioException.class,
                () -> compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2));
        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
    }

    @Test
    @DisplayName("Rechaza reembolsar una compra fuera del plazo definido")
    void rechazaReembolsarFueraDePlazo() {
        Compra compra = crearCompraCompletada();

        assertThrows(ReglaDominioException.class,
                () -> compra.solicitarReembolso(FECHA_COMPRA.plusDays(3), 2));
        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
    }

    @Test
    @DisplayName("Rechaza reembolsar una compra que ya fue REEMBOLSADA")
    void rechazaReembolsarDosVeces() {
        Compra compra = crearCompraCompletada();
        compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2);

        assertThrows(ReglaDominioException.class,
                () -> compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2));
    }

    // ===================== registrarResena() — Regla 2 =====================

    @Test
    @DisplayName("Registra una reseña sobre una compra completada, y rechaza reseñar dos veces (Regla 2)")
    void registrarResena() {
        Compra compra = crearCompraCompletada();

        compra.registrarResena();
        assertTrue(compra.isResenada());

        assertThrows(ReglaDominioException.class, compra::registrarResena);
    }

    @Test
    @DisplayName("Rechaza reseñar una compra que sigue PENDIENTE (Regla 2: sin compra confirmada no hay reseña)")
    void rechazaResenarCompraPendiente() {
        Compra compra = crearCompraValida();

        assertThrows(ReglaDominioException.class, compra::registrarResena);
        assertFalse(compra.isResenada());
    }

    @Test
    @DisplayName("Rechaza reseñar una compra que fue REEMBOLSADA")
    void rechazaResenarCompraReembolsada() {
        Compra compra = crearCompraCompletada();
        compra.solicitarReembolso(FECHA_COMPRA.plusDays(1), 2);

        assertThrows(ReglaDominioException.class, compra::registrarResena);
        assertFalse(compra.isResenada());
    }

    // ===================== asignarPuntos() — Regla 6 =====================

    @Test
    @DisplayName("Asigna puntos solo después de la reseña (Regla 6)")
    void asignarPuntosDespuesDeResena() {
        Compra compra = crearCompraCompletada();
        compra.registrarResena();

        int puntos = compra.asignarPuntos(10);

        assertEquals(10, puntos);
        assertTrue(compra.isPuntosAsignados());
    }

    @Test
    @DisplayName("Rechaza asignar puntos sin haber reseñado (Regla 6)")
    void rechazaAsignarPuntosSinResena() {
        Compra compra = crearCompraCompletada();

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(10));
        assertFalse(compra.isPuntosAsignados());
    }

    @Test
    @DisplayName("Rechaza asignar puntos dos veces a la misma compra")
    void rechazaAsignarPuntosDosVeces() {
        Compra compra = crearCompraCompletada();
        compra.registrarResena();
        compra.asignarPuntos(10);

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(10));
    }

    @Test
    @DisplayName("Rechaza asignar puntos con valor no positivo")
    void rechazaAsignarPuntosNoPositivos() {
        Compra compra = crearCompraCompletada();
        compra.registrarResena();

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(0));
        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(-5));
    }

    // ===================== identidad =====================

    @Test
    @DisplayName("La identidad de la compra depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Precio precio = new Precio(50000);
        Compra compra1 = new Compra(id, UUID.randomUUID(), UUID.randomUUID(), 1, FECHA_COMPRA, precio);
        Compra compra2 = new Compra(id, UUID.randomUUID(), UUID.randomUUID(), 5, FECHA_COMPRA, precio);
        Compra compra3 = new Compra(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, FECHA_COMPRA, precio);

        assertEquals(compra1, compra2);
        assertEquals(compra1.hashCode(), compra2.hashCode());
        assertNotEquals(compra1, compra3);
    }
}