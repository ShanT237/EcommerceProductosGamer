package com.uniquindio.backend.application.dto.request;

import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Gama;

// Mapea a: BuscarProductosUseCase.ejecutar(FiltroProducto)
// Se llena con query params: ?texto=mouse&categoria=Perifericos&gama=ALTA&precioMin=10&precioMax=500&soloPreventa=true
// Todos son opcionales. Los rangos de precio inválidos los rechaza FiltroProducto (400).
public record BuscarProductosRequest(
        String texto,
        String categoria,
        Gama gama,
        Double precioMin,
        Double precioMax,
        Boolean soloPreventa
) {
    public FiltroProducto aFiltro() {
        return new FiltroProducto(texto, categoria, gama, precioMin, precioMax, soloPreventaOrDefault());
    }

    public boolean soloPreventaOrDefault() {
        return Boolean.TRUE.equals(soloPreventa);
    }
}