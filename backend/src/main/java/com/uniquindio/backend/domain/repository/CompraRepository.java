package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Compra.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar compras
 */
public interface CompraRepository {

    Optional<Compra> obtenerPorId(UUID id);

    void guardar(Compra compra);

    /**
     * Todas las compras del usuario (cualquier estado). La usa ConsultarHistorialComprasUseCase.
     */
    List<Compra> obtenerPorUsuarioId(UUID usuarioId);

    /**
     * Indica si el usuario ya tiene una compra activa de ese producto.
     * Una compra es activa cuando está PENDIENTE o COMPLETADA; las REEMBOLSADA,
     * CANCELADA y EXPIRADA no cuentan.
     * La usa RealizarCompraUseCase para validar la Regla 9 (un producto exclusivo
     * se compra una sola vez por usuario).
     */
    boolean existeCompraDe(UUID usuarioId, UUID productoId);

    /**
     * Indica si el producto tiene alguna compra activa (PENDIENTE o COMPLETADA).
     * La usa EliminarProductoUseCase para validar la Regla 3.
     */
    boolean existeCompraActivaDeProducto(UUID productoId);


    long contarCompletadasDe(UUID usuarioId);

    boolean existeCompraDeAlguno(UUID usuarioId, Collection<UUID> productoIds);

    /**
     * Todas las compras (cualquier estado). La usan los casos de uso de reportes.
     */
    List<Compra> obtenerTodas();
}