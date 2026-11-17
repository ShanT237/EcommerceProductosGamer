package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.service.HashContrasena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CambiarContrasenaVendedor (CU-26, seguridad).
 * Cambio directo exigiendo la contraseña actual; la nueva debe ser segura y distinta.
 */
@Service
@RequiredArgsConstructor
public class CambiarContrasenaVendedorUseCase {

    private final VendedorRepository vendedorRepository;
    private final HashContrasena hashContrasena;

    public void ejecutar(UUID vendedorId, String contrasenaActual, String contrasenaNueva) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.cambiarContrasena(contrasenaActual, contrasenaNueva, hashContrasena);

        vendedorRepository.guardar(vendedor);
    }
}
