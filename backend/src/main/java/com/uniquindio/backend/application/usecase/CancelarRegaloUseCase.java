package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CancelarRegalo.
 * Lo cancela el vendedor dueño (del producto o del combo) desde cualquier estado anterior a
 * ENTREGADO; la máquina de estados de {@link Regalo} valida la transición. Si el regalo ya
 * tenía comprador (ELEGIDO o ENVIADO) se le avisa; el comprador recupera su cupo porque los
 * regalos CANCELADOS no cuentan como asignados.
 */
@Service
@RequiredArgsConstructor
public class CancelarRegaloUseCase {

    private final RegaloRepository regaloRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;
    private final NotificadorEventos notificador;

    @Transactional
    public Regalo ejecutar(UUID regaloId, UUID vendedorId) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        if (!DuenoDeRegalo.esDelVendedor(regalo, vendedorId, productoRepository, comboRepository)) {
            throw new ReglaDominioException("El regalo no pertenece al vendedor");
        }

        regalo.cancelarRegalo();

        regaloRepository.guardar(regalo);
        notificador.regaloCancelado(regalo, DuenoDeRegalo.nombreDe(regalo, productoRepository, comboRepository));

        return regalo;
    }
}
