package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EditarPerfilVendedor (CU-26). Cambia nombre y correo; el tipo y el documento
 * no se editan. El correo debe seguir siendo único entre vendedores (conservar el propio
 * está permitido) y tampoco puede ser el de un comprador: el inicio de sesión busca primero
 * entre compradores, así que un correo compartido dejaría al vendedor sin poder ingresar.
 */
@Service
@RequiredArgsConstructor
public class EditarPerfilVendedorUseCase {

    private final VendedorRepository vendedorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Vendedor ejecutar(UUID vendedorId, String nombre, String correo) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.asegurarActivo();

        boolean correoDeOtroVendedor = vendedorRepository.obtenerPorCorreo(correo)
                .filter(existente -> !existente.getId().equals(vendedorId))
                .isPresent();
        if (correoDeOtroVendedor || usuarioRepository.obtenerPorCorreo(correo).isPresent()) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }

        vendedor.actualizarPerfil(nombre, correo);

        vendedorRepository.guardar(vendedor);

        return vendedor;
    }
}
