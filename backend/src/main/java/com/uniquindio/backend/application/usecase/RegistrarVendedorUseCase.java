package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.service.HashContrasena;
import com.uniquindio.backend.domain.valueobject.Contrasena;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: RegistrarVendedor (CU-11)
 * Impide registros duplicados por documento (cédula o NIT), exige una contraseña
 * segura y guarda solo su hash.
 */
@Service
@RequiredArgsConstructor
public class RegistrarVendedorUseCase {

    private final VendedorRepository vendedorRepository;
    private final HashContrasena hashContrasena;

    @Transactional
    public Vendedor ejecutar(UUID id, String nombre, String correo, TipoVendedor tipo,
                             String documento, String contrasena) {
        if (vendedorRepository.existeConDocumento(documento)) {
            throw new ConflictoException("Ya existe un vendedor con ese documento");
        }
        Contrasena.validarFortaleza(contrasena);

        Vendedor vendedor = Vendedor.registrar(id, nombre, correo, tipo, documento);
        vendedor.definirContrasena(hashContrasena.hashear(contrasena));

        vendedorRepository.guardar(vendedor);

        return vendedor;
    }
}