package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;

import java.util.List;

/** Un combo junto con sus productos ya resueltos (en el orden del combo). */
public record ComboConProductos(Combo combo, List<Producto> productos) {
}
