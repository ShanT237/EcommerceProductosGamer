package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CatalogoRepository;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: FiltrarYBuscarProductos (CU-20).
 * Valida la paginación y delega la búsqueda al repositorio de catálogo, que aplica
 * filtros, visibilidad y paginación en la base de datos.
 */
@Service
@RequiredArgsConstructor
public class BuscarProductosUseCase {

    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private final CatalogoRepository catalogoRepository;
    private final Clock clock;

    public Pagina<Producto> ejecutar(FiltroProducto filtro, int pagina, int tamano) {
        if (pagina < 0) {
            throw new ReglaDominioException("La página no puede ser negativa");
        }
        if (tamano <= 0 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new ReglaDominioException(
                    "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
        return catalogoRepository.buscar(filtro, pagina, tamano, LocalDate.now(clock));
    }
}
