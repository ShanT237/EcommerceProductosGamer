package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Consulta las preventas cuyo lanzamiento todavía es futuro, de la más próxima a la más lejana. */
@Service
@RequiredArgsConstructor
public class ConsultarPreventasUseCase {

    private final PreventaRepository preventaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Preventa> ejecutar() {
        return preventaRepository.obtenerFuturas(LocalDate.now(clock)).stream()
                .sorted(Comparator.comparing(Preventa::getFechaLanzamiento))
                .toList();
    }
}
