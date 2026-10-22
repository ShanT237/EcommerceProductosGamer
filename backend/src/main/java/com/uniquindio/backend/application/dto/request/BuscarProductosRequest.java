package com.uniquindio.backend.application.dto.request;

import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Gama;

// Se llena con query params de filtros; page y size se reciben por separado en el controller.
// Ejemplo: ?texto=mouse&categoria=Perifericos&gama=ALTA&precioMin=10&precioMax=500&soloPreventa=true&page=0&size=20
// Todos los filtros son opcionales. Los rangos inválidos los rechaza FiltroProducto (400).
public record BuscarProductosRequest(
        String texto,
        String categoria,
        Gama gama,
        Double precioMin,
        Double precioMax,
        boolean soloPreventa
) {
    public FiltroProducto aFiltro() {
        return new FiltroProducto(texto, categoria, gama, precioMin, precioMax, soloPreventa);
    }
}