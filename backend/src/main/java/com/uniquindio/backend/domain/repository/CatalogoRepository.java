package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Pagina;

import java.time.LocalDate;

public interface CatalogoRepository {

    Pagina<Producto> buscar(FiltroProducto filtro, int pagina, int tamano, LocalDate hoy);
}
