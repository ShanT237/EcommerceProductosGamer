package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.application.usecase.ComboConProductos;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Precio;

import java.util.List;
import java.util.UUID;

// Usado en: GET /api/combos y GET /api/combos/mios
public record ComboDetalleResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        String descripcion,
        double descuento,
        List<ProductoDelCombo> productos,
        double precioSinDescuento,
        double precioConDescuento
) {
    public record ProductoDelCombo(UUID id, String nombre, double precio, int stock) {
        static ProductoDelCombo desde(Producto p) {
            return new ProductoDelCombo(p.getId(), p.getNombre(), p.getPrecio(), p.getStock());
        }
    }

    public static ComboDetalleResponse desde(ComboConProductos detalle) {
        Combo combo = detalle.combo();
        List<Producto> productos = detalle.productos();
        double sinDescuento = productos.stream().mapToDouble(Producto::getPrecio).sum();
        return new ComboDetalleResponse(
                combo.getId(),
                combo.getVendedorId(),
                combo.getNombre(),
                combo.getDescripcion(),
                combo.getDescuento(),
                productos.stream().map(ProductoDelCombo::desde).toList(),
                sinDescuento,
                precioConDescuento(combo, productos, sinDescuento)
        );
    }

    private static double precioConDescuento(Combo combo, List<Producto> productos, double sinDescuento) {
        if (productos.size() == combo.getProductos().size()) {
            return combo.calcularPrecioTotal(productos);
        }
        // Combo con productos que ya no existen (solo en la vista del vendedor): precio de los que quedan.
        return sinDescuento > 0 ? new Precio(sinDescuento).aplicarDescuento(combo.getDescuento()).valor() : 0;
    }
}
