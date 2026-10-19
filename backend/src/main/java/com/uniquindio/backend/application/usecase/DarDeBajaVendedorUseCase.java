package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: DarDeBajaVendedor (CU-25).
 * Baja lógica del vendedor; conserva su historial de ventas.
 */
@Service
@RequiredArgsConstructor
public class DarDeBajaVendedorUseCase {

    private final VendedorRepository vendedorRepository;

    public Vendedor ejecutar(UUID vendedorId) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));

        vendedor.darDeBaja();

        vendedorRepository.guardar(vendedor);

        return vendedor;
    }
}
