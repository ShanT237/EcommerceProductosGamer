package com.uniquindio.backend.infrastructure.persistence.jpa;

import java.util.UUID;

interface ProductoMasVendidoProjection {

    UUID getProductoId();

    String getNombre();

    Long getUnidades();
}
