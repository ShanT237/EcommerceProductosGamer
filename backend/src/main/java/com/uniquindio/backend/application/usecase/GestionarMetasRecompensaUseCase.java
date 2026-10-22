package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.MetaRecompensaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: GestionarMetasRecompensa (CU-33).
 * El admin crea metas, cambia su umbral o las desactiva. El producto regalo debe existir
 * y no estar eliminado.
 */
@Service
@RequiredArgsConstructor
public class GestionarMetasRecompensaUseCase {

    private final MetaRecompensaRepository metaRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public MetaRecompensa crear(UUID id, int comprasRequeridas, UUID productoRegaloId) {
        Producto regalo = productoRepository.obtenerPorId(productoRegaloId)
                .orElseThrow(() -> new NoSuchElementException("El producto regalo no existe"));
        regalo.asegurarNoEliminado();

        MetaRecompensa meta = MetaRecompensa.crear(id, comprasRequeridas, productoRegaloId);

        metaRepository.guardar(meta);

        return meta;
    }

    public MetaRecompensa actualizarUmbral(UUID metaId, int nuevoUmbral) {
        MetaRecompensa meta = obtener(metaId);

        meta.actualizarUmbral(nuevoUmbral);

        metaRepository.guardar(meta);

        return meta;
    }

    public MetaRecompensa desactivar(UUID metaId) {
        MetaRecompensa meta = obtener(metaId);

        meta.desactivar();

        metaRepository.guardar(meta);

        return meta;
    }

    private MetaRecompensa obtener(UUID metaId) {
        return metaRepository.obtenerPorId(metaId)
                .orElseThrow(() -> new NoSuchElementException("La meta no existe"));
    }
}
