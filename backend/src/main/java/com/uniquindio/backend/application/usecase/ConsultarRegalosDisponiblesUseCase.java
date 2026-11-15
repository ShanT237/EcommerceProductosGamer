package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: ConsultarRegalosDisponibles.
 * Los regalos que un comprador puede elegir: solo los que siguen en estado DISPONIBLE.
 */
@Service
@RequiredArgsConstructor
public class ConsultarRegalosDisponiblesUseCase {

    private final RegaloRepository regaloRepository;

    @Transactional(readOnly = true)
    public List<Regalo> ejecutar() {
        return regaloRepository.obtenerPorEstado(EstadoRegalo.DISPONIBLE);
    }
}
