package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarRegalosPorEnviar.
 * Los regalos ya ELEGIDOS por un comprador que el vendedor todavía debe enviar
 * (solo los de sus productos o combos).
 */
@Service
@RequiredArgsConstructor
public class ConsultarRegalosPorEnviarUseCase {

    private final RegaloRepository regaloRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;

    @Transactional(readOnly = true)
    public List<Regalo> ejecutar(UUID vendedorId) {
        return regaloRepository.obtenerPorEstado(EstadoRegalo.ELEGIDO).stream()
                .filter(regalo -> DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository))
                .toList();
    }
}
