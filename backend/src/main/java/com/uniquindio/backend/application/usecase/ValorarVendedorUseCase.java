package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.ValoracionVendedorRepository;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso: ValorarVendedor (CU-28).
 * El comprador califica al vendedor del producto de una compra COMPLETADA suya; una
 * valoración por compra.
 */
@Service
@RequiredArgsConstructor
public class ValorarVendedorUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final ValoracionVendedorRepository valoracionRepository;
    private final Clock clock;

    @Transactional
    public ValoracionVendedor ejecutar(UUID compraId, UUID usuarioId, int estrellas, String comentario) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(usuarioId);

        if (valoracionRepository.existePorCompra(compraId)) {
            throw new ConflictoException("La compra ya tiene una valoración del vendedor");
        }

        Producto producto = productoRepository.obtenerPorId(compra.getProductoId())
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        ValoracionVendedor valoracion = ValoracionVendedor.crear(UUID.randomUUID(), compraId, usuarioId,
                producto.getVendedorId(), compra.estaCompletada(), new Calificacion(estrellas),
                comentario, LocalDateTime.now(clock));

        valoracionRepository.guardar(valoracion);

        return valoracion;
    }
}
