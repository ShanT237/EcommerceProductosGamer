package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CuponRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: CrearCupon.
 * Un vendedor activo crea un cupón propio: el descuento solo aplicará a sus productos.
 * El código es único (sin distinguir mayúsculas) y el vencimiento no puede estar en el pasado.
 */
@Service
@RequiredArgsConstructor
public class CrearCuponUseCase {

    private final CuponRepository cuponRepository;
    private final VendedorRepository vendedorRepository;
    private final Clock clock;

    @Transactional
    public Cupon ejecutar(UUID id, UUID vendedorId, String codigo, double porcentaje,
                          LocalDate vence, int usosMaximos) {
        vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new ReglaDominioException("El vendedor no existe"))
                .asegurarActivo();

        // Crea (y valida/normaliza) antes de consultar la unicidad, para comparar el código ya normalizado
        Cupon cupon = Cupon.crear(id, vendedorId, codigo, porcentaje, vence, usosMaximos);
        if (vence.isBefore(LocalDate.now(clock))) {
            throw new ReglaDominioException("La fecha de vencimiento del cupón no puede estar en el pasado");
        }
        if (cuponRepository.existePorCodigo(cupon.getCodigo())) {
            throw new ConflictoException("Ya existe un cupón con el código " + cupon.getCodigo());
        }

        cuponRepository.guardar(cupon);
        return cupon;
    }
}
