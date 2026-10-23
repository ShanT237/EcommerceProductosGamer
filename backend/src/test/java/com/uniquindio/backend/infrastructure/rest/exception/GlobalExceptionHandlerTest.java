package com.uniquindio.backend.infrastructure.rest.exception;

import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void reglaDeDominioResponde422() {
        var r = handler.manejarReglaDominio(new ReglaDominioException("Stock insuficiente"));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, r.getStatusCode());
        assert r.getBody() != null;
        assertEquals(422, r.getBody().get("status"));
        assertEquals("Stock insuficiente", r.getBody().get("mensaje"));
    }

    @Test
    void conflictoResponde409() {
        var r = handler.manejarConflicto(new ConflictoException("Ya existe"));

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals(409, r.getBody().get("status"));
    }
}