package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;

/**
 * Value Object: criterios de búsqueda del catálogo. Todos son opcionales (null = sin filtro);
 * el rango de precios es inclusivo. soloPreventa lo resuelve el caso de uso, porque la fecha
 * de lanzamiento vive en Preventa.
 */
public record FiltroProducto(String texto, String categoria, Gama gama,
                             Double precioMin, Double precioMax, boolean soloPreventa) {

    public FiltroProducto {
        if (precioMin != null && precioMin < 0) {
            throw new ReglaDominioException("El precio mínimo no puede ser negativo");
        }
        if (precioMax != null && precioMax < 0) {
            throw new ReglaDominioException("El precio máximo no puede ser negativo");
        }
        if (precioMin != null && precioMax != null && precioMin > precioMax) {
            throw new ReglaDominioException("El precio mínimo no puede superar al máximo");
        }
    }

    public static FiltroProducto sinFiltros() {
        return new FiltroProducto(null, null, null, null, null, false);
    }

    public boolean coincide(Producto producto) {
        if (texto != null && !texto.isBlank()
                && !producto.getNombre().toLowerCase().contains(texto.trim().toLowerCase())) {
            return false;
        }
        if (categoria != null && !categoria.isBlank()
                && !producto.getCategoria().equalsIgnoreCase(categoria.trim())) {
            return false;
        }
        if (gama != null && producto.getGama() != gama) {
            return false;
        }
        if (precioMin != null && producto.getPrecio() < precioMin) {
            return false;
        }
        return precioMax == null || producto.getPrecio() <= precioMax;
    }
}
