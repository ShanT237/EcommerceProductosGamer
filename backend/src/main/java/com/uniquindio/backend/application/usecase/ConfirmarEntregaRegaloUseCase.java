package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ConfirmarEntregaRegalo (CU-24).
 * Un regalo solo se entrega si ya fue enviado y solo lo confirma el usuario destinatario;
 * ambas reglas las valida la entidad.
 */
@Service
@RequiredArgsConstructor
public class ConfirmarEntregaRegaloUseCase {

    private final RegaloRepository regaloRepository;

    public Regalo ejecutar(UUID regaloId, UUID usuarioId) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        regalo.confirmarEntrega(usuarioId);

        regaloRepository.guardar(regalo);

        return regalo;
    }
}
