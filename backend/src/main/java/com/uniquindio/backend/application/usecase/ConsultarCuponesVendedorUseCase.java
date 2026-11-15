package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarCuponesVendedor.
 * Los cupones propios del vendedor (vigentes, vencidos o agotados), ordenados por código.
 */
@Service
@RequiredArgsConstructor
public class ConsultarCuponesVendedorUseCase {

    private final CuponRepository cuponRepository;

    @Transactional(readOnly = true)
    public List<Cupon> ejecutar(UUID vendedorId) {
        return cuponRepository.obtenerPorVendedorId(vendedorId).stream()
                .sorted(Comparator.comparing(Cupon::getCodigo))
                .toList();
    }
}
