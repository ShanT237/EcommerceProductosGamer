package com.uniquindio.backend.domain.valueobject;

import java.util.List;

public record Pagina<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
    public Pagina {
        contenido = List.copyOf(contenido);
    }
}
