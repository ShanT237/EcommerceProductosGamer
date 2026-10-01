package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ElegirRegalo (CU-07).
 * Asigna un regalo disponible a un usuario que cumplió una meta de compras (Regla 10).
 * Verifica que regalo y usuario existan, averigua vía repositorio si el usuario ya posee
 * el producto del regalo, y delega en Regalo.elegir(...) las reglas: el regalo debe estar
 * DISPONIBLE (no se elige dos veces) y el usuario no debe poseer ya ese producto.
 * No contiene ningún if de decisión de negocio: solo coordina.
 */
@Service
public class ElegirRegaloUseCase {

    private final RegaloRepository regaloRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompraRepository compraRepository;

    public ElegirRegaloUseCase(RegaloRepository regaloRepository,
                               UsuarioRepository usuarioRepository,
                               CompraRepository compraRepository) {
        this.regaloRepository = regaloRepository;
        this.usuarioRepository = usuarioRepository;
        this.compraRepository = compraRepository;
    }

    public Regalo ejecutar(UUID regaloId, UUID usuarioId) {
        Regalo regalo = regaloRepository.obtenerPorId(regaloId)
                .orElseThrow(() -> new NoSuchElementException("El regalo no existe"));

        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));

        // Un regalo de combo (idProducto == null) no se compara contra compras individuales.
        boolean usuarioYaPoseeElProducto = regalo.getIdProducto() != null
                && compraRepository.existeCompraDe(usuarioId, regalo.getIdProducto());

        regalo.elegir(usuarioId, usuarioYaPoseeElProducto);

        regaloRepository.guardar(regalo);

        return regalo;
    }
}