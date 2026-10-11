package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Resena;

import java.time.LocalDateTime;

public record SubirResenaResponse(
        String id,
        String idCompra,
        String idProducto,
        String idUsuario,
        int calificacion,
        String comentario,
        LocalDateTime fechaCreacion
) {
    public static SubirResenaResponse desde(Resena resena) {
        return new SubirResenaResponse(
                resena.getId(),
                resena.getIdCompra(),
                resena.getIdProducto(),
                resena.getIdUsuario(),
                resena.getCalificacion().estrellas(),
                resena.getComentario(),
                resena.getFechaCreacion()
        );
    }
}