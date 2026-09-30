package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: ConfirmarCompra.
 * Recibe la intención de confirmar el pago de una compra ya creada
 * (simulación de pago exitosa) y delega en el dominio la transición
 * PENDIENTE -> COMPLETADA.
 *
 * Se mantiene separado de RealizarCompraUseCase a propósito: la creación
 * de la compra (RealizarCompraUseCase) y la confirmación del pago son dos
 * pasos independientes del flujo. Esto es lo que habilita, por ejemplo,
 * que SubirResenaUseCase pueda apoyarse en el estado real de la compra
 * (compra.getEstado() == EstadoCompra.COMPLETADA) en vez de asumirlo.
 *
 * No contiene ningún if de decisión de negocio: solo coordina.
 */
@Service
public class ConfirmarCompraUseCase {

    private final CompraRepository compraRepository;

    public ConfirmarCompraUseCase(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    public Compra ejecutar(UUID idCompra, UUID usuarioId) {
        Compra compra = compraRepository.obtenerPorId(idCompra)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));

        if (!compra.getUsuarioId().equals(usuarioId)) {
            throw new ReglaDominioException("La compra no pertenece a este usuario");
        }

        // El dominio decide si la transición es válida (Compra.confirmar()
        // ya rechaza confirmar una compra que no está PENDIENTE).
        compra.confirmar();

        compraRepository.guardar(compra);

        return compra;
    }
}