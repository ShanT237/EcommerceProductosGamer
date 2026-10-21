package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EnviarRegalo (CU-24).
 * Un regalo solo se envía si ya fue elegido; lo valida la entidad.
 */
@Service
@RequiredArgsConstructor
public class EnviarRegaloUseCase {

    private final RegaloRepository regaloRepository;

    public Regalo ejecutar(UUID regaloId) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        regalo.enviar();

        regaloRepository.guardar(regalo);

        return regalo;
    }
}
