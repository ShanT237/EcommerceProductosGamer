package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: AsignarRegaloPorMeta (CU-10, Regla 10).
 * Un usuario gana un regalo por cada COMPRAS_POR_REGALO compras COMPLETADAS.
 * Valida la meta y que el usuario no posea ya el producto; las transiciones
 * de estado del regalo las valida la entidad Regalo.
 */
@Service
@RequiredArgsConstructor
public class AsignarRegaloPorMetaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final CompraRepository compraRepository;
    private final RegaloRepository regaloRepository;
    private final ComboRepository comboRepository;

    @Transactional
    public Regalo ejecutar(UUID usuarioId, UUID regaloId) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
        usuario.asegurarActivo();

        usuario.asegurarPuedeElegirRegalo(
                compraRepository.contarCompletadasDe(usuarioId),
                regaloRepository.contarAsignadosA(usuarioId));

        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        regalo.elegir(usuarioId, usuarioYaPosee(usuarioId, regalo));

        regaloRepository.guardar(regalo);
        return regalo;
    }

    /** Si el regalo es un combo, basta con poseer uno de sus productos para rechazarlo. */
    private boolean usuarioYaPosee(UUID usuarioId, Regalo regalo) {
        regalo.asegurarDestinoValido();
        if (regalo.getIdProducto() != null) {
            return compraRepository.existeCompraDe(usuarioId, regalo.getIdProducto());
        }
        Combo combo = comboRepository.obtenerPorId(regalo.getIdCombo())
                .orElseThrow(() -> new NoSuchElementException("El combo del regalo no existe"));
        return compraRepository.existeCompraDeAlguno(usuarioId, combo.getProductos());
    }
}