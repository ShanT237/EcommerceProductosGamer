package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RedimirPuntosRequest;
import com.uniquindio.backend.application.dto.response.PuntosResponse;
import com.uniquindio.backend.application.usecase.RedimirPuntosUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Limitación: no hay autenticación ni autorización. El {@code usuarioId} viaja
 * en el body; cualquiera podría redimir puntos de otro usuario (CU-27 pendiente).
 */
@RestController
@RequestMapping("/api/puntos")
public class PuntosController {

    private final RedimirPuntosUseCase redimirPuntosUseCase;
    private final UsuarioMapper usuarioMapper;

    public PuntosController(RedimirPuntosUseCase redimirPuntosUseCase, UsuarioMapper usuarioMapper) {
        this.redimirPuntosUseCase = redimirPuntosUseCase;
        this.usuarioMapper = usuarioMapper;
    }

    // CU-08 Redimir Puntos por Beneficios
    @PostMapping("/redimir")
    public ResponseEntity<PuntosResponse> redimir(@Valid @RequestBody RedimirPuntosRequest request) {
        Usuario usuario = redimirPuntosUseCase.ejecutar(request.usuarioId(), request.cantidad());
        return ResponseEntity.ok(usuarioMapper.toPuntosResponse(usuario));
    }
}