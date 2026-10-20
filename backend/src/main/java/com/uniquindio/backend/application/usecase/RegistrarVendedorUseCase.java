package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: RegistrarVendedor (CU-11).
 * Impide registros duplicados por documento (cédula o NIT).
 */
@Service
@RequiredArgsConstructor
public class RegistrarVendedorUseCase {

    private final VendedorRepository vendedorRepository;

    @Transactional
    public Vendedor ejecutar(UUID id, String nombre, String correo, TipoVendedor tipo, String documento) {
        if (vendedorRepository.existeConDocumento(documento)) {
            throw new ReglaDominioException("Ya existe un vendedor con ese documento");
        }

        Vendedor vendedor = Vendedor.registrar(id, nombre, correo, tipo, documento);

        vendedorRepository.guardar(vendedor);

        return vendedor;
    }
}
