package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: AsignarRegaloPorMeta (CU-10, Regla 10).
 * La meta es un mínimo de compras COMPLETADAS del usuario (metaCompras).
 * Que el usuario no posea ya el producto lo decide Regalo.elegir.
 */
@Service
@RequiredArgsConstructor
public class AsignarRegaloPorMetaUseCase {

    private final RegaloRepository regaloRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraRepository compraRepository;

    public Regalo ejecutar(UUID regaloId, UUID usuarioId, int metaCompras) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
        usuario.asegurarActivo();

        if (compraRepository.contarCompletadasDe(usuarioId) < metaCompras) {
            throw new ReglaDominioException("El usuario no cumple la meta de compras");
        }

        boolean yaPosee = regalo.getIdProducto() != null
                && compraRepository.existeCompraDe(usuarioId, regalo.getIdProducto());

        regalo.elegir(usuarioId, yaPosee);

        regaloRepository.guardar(regalo);

        return regalo;
    }
}
