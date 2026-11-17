package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: MarcarDescargaCompra.
 * Marca como descargada una compra COMPLETADA de un producto DIGITAL; no entrega ningún archivo.
 * Funciona también para líneas de un pedido o combo. Efecto aceptado: tras descargar, el
 * reembolso queda bloqueado (también el del pedido completo).
 * Un producto pausado o eliminado sigue siendo descargable: el comprador ya lo pagó.
 */
@Service
@RequiredArgsConstructor
public class MarcarDescargaCompraUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public Compra ejecutar(UUID compraId, UUID usuarioId) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(usuarioId);

        Producto producto = productoRepository.obtenerPorId(compra.getProductoId())
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarEsDigital();

        compra.marcarDescargado();
        compraRepository.guardar(compra);
        return compra;
    }
}
