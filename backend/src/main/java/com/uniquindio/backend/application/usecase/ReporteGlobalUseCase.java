package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ReporteGlobalResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: ReporteGlobalMarketplace (CU-36).
 * Consolidado para el admin: compras COMPLETADAS e ingresos, productos no eliminados y
 * vendedores activos.
 */
@Service
@RequiredArgsConstructor
public class ReporteGlobalUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final VendedorRepository vendedorRepository;

    public ReporteGlobalResponse ejecutar() {
        List<Compra> completadas = compraRepository.obtenerTodas().stream()
                .filter(Compra::estaCompletada)
                .toList();
        double ingresos = completadas.stream().mapToDouble(c -> c.getSubtotal().valor()).sum();
        int productosActivos = (int) productoRepository.obtenerTodos().stream()
                .filter(p -> !p.isEliminado())
                .count();
        int vendedoresActivos = (int) vendedorRepository.obtenerTodos().stream()
                .filter(Vendedor::isActivo)
                .count();

        return new ReporteGlobalResponse(completadas.size(), ingresos, productosActivos, vendedoresActivos);
    }
}
