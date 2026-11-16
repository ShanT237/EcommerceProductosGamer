package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EnviarRegalo (CU-24).
 * Un regalo solo se envía si ya fue elegido (lo valida la entidad) y solo lo envía el
 * vendedor dueño de su producto o combo.
 */
@Service
@RequiredArgsConstructor
public class EnviarRegaloUseCase {

    private final RegaloRepository regaloRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;
    private final NotificadorEventos notificador;

    public Regalo ejecutar(UUID regaloId, UUID vendedorId) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        if (!DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository)) {
            throw new ReglaDominioException("El regalo no pertenece al vendedor");
        }

        regalo.enviar();

        regaloRepository.guardar(regalo);
        notificador.regaloEnviado(regalo);

        return regalo;
    }
}
