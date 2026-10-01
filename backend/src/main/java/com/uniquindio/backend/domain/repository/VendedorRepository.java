package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Vendedor;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Vendedor.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar vendedores.
 */
public interface VendedorRepository {

    Optional<Vendedor> obtenerPorId(UUID id);

    void guardar(Vendedor vendedor);

    /**
     * Indica si ya existe un vendedor registrado con ese documento (cédula o NIT).
     * La usa RegistrarVendedorUseCase para impedir registros duplicados sin que
     * el caso de uso tenga que decidir nada por su cuenta.
     */
    boolean existeConDocumento(String documento);
}