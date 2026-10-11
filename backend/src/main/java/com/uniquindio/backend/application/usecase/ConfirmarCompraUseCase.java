package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: ConfirmarCompra.
 * Orquesta la confirmación del pago de una compra: consulta, invoca
 * compra.confirmar() y persiste el cambio.
 */
@Service
@RequiredArgsConstructor
public class ConfirmarCompraUseCase {

    private final CompraRepository compraRepository;

    public Compra ejecutar(UUID idCompra, UUID usuarioId) {
        Compra compra = compraRepository.obtenerPorId(idCompra)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(usuarioId);

        compra.confirmar();

        compraRepository.guardar(compra);

        return compra;
    }
}
