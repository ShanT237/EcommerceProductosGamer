package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;

import java.util.List;
import java.util.UUID;

public interface ValoracionVendedorRepository {
    void guardar(ValoracionVendedor valoracion);
    boolean existePorCompra(UUID compraId);
    List<ValoracionVendedor> obtenerPorVendedorId(UUID vendedorId);
}
