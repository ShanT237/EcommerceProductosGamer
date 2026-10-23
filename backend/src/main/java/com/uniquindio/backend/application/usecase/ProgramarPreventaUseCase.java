package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ProgramarPreventa (CU-37).
 * Orquesta la programación: vendedor activo, producto del vendedor y sin preventa previa;
 * la fecha futura la valida {@link Preventa#programar} con la fecha del servidor.
 */
@Service
@RequiredArgsConstructor
public class ProgramarPreventaUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    @Transactional
    public Preventa ejecutar(UUID vendedorId, UUID productoId, LocalDate fechaLanzamiento, String descripcion) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.validarPuedePublicar();

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarNoEliminado();
        producto.asegurarPerteneceA(vendedorId);

        if (preventaRepository.obtenerPorProductoId(productoId).isPresent()) {
            throw new ConflictoException("El producto ya tiene una preventa programada");
        }

        Preventa preventa = Preventa.programar(
                UUID.randomUUID(), productoId, fechaLanzamiento, descripcion, LocalDate.now(clock));

        preventaRepository.guardar(preventa);
        return preventa;
    }
}
