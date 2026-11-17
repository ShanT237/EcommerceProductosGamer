package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Consulta el perfil del vendedor autenticado (CU-26, lectura). */
@Service
@RequiredArgsConstructor
public class ObtenerPerfilVendedorUseCase {

    private final VendedorRepository vendedorRepository;

    public Vendedor ejecutar(UUID vendedorId) {
        return vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
    }
}
