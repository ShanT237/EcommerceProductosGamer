package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.CuponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: AplicarCupon (CU-17).
 * Aplica el descuento del cupón al precio unitario de una compra PENDIENTE. El uso del
 * cupón solo se consume si la compra aceptó el descuento.
 */
@Service
@RequiredArgsConstructor
public class AplicarCuponUseCase {

    private final CompraRepository compraRepository;
    private final CuponRepository cuponRepository;
    private final Clock clock;

    @Transactional
    public Compra ejecutar(UUID compraId, UUID usuarioId, String codigo) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(usuarioId);

        Cupon cupon = cuponRepository.obtenerPorCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("El cupón no existe"));
        cupon.validarUso(LocalDate.now(clock));

        compra.aplicarCupon(compra.getPrecioUnitario().aplicarDescuento(cupon.getPorcentaje()));
        cupon.registrarUso();

        compraRepository.guardar(compra);
        cuponRepository.guardar(cupon);

        return compra;
    }
}
