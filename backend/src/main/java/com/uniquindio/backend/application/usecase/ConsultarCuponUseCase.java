package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CuponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ConsultarCupon (vista previa para el comprador).
 * Devuelve el cupón solo si se puede usar hoy y el comprador no lo ha usado antes; no consume ningún uso.
 */
@Service
@RequiredArgsConstructor
public class ConsultarCuponUseCase {

    private final CuponRepository cuponRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Cupon ejecutar(String codigo, UUID usuarioId) {
        Cupon cupon = cuponRepository.obtenerPorCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("El cupón no existe"));
        cupon.validarUso(LocalDate.now(clock));
        if (cuponRepository.fueUsadoPor(cupon.getId(), usuarioId)) {
            throw new ReglaDominioException("Ya usaste este cupón: solo se puede usar una vez por comprador");
        }
        return cupon;
    }
}
