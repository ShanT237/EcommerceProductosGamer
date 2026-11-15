package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Combo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComboRepository {
    Optional<Combo> obtenerPorId(UUID id);
    void guardar(Combo combo);

    /** Todos los combos (de cualquier vendedor). */
    List<Combo> obtenerTodos();

    /** Combos del vendedor indicado. */
    List<Combo> obtenerPorVendedorId(UUID vendedorId);

    /** Elimina el combo y sus productos asociados. No falla si no existe. */
    void eliminar(UUID id);
}
