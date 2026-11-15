package com.uniquindio.backend.application.dto.request;

import java.util.UUID;

// Mapea a: CrearRegaloUseCase.ejecutar(id, vendedorId, productoId, comboId)
// Debe venir exactamente uno de los dos; lo valida Regalo.crear (422 si vienen ambos o ninguno).
// El vendedorId sale del token (Authentication), no del body.
public record CrearRegaloRequest(
        UUID productoId,
        UUID comboId
) {}
