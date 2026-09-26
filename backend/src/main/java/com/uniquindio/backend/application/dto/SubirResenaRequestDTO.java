package com.uniquindio.backend.application.dto;

import java.util.UUID;

public record SubirResenaRequestDTO(
        String idResena,
        UUID idCompra,
        UUID idUsuario,
        int valorCalificacion,
        String comentario
) {
}