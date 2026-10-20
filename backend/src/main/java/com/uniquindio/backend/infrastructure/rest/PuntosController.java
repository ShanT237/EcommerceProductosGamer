package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RedimirPuntosRequest;
import com.uniquindio.backend.application.dto.response.RegistrarUsuarioResponse;
import com.uniquindio.backend.application.usecase.RedimirPuntosUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<RegistrarUsuarioResponse> redimir(@Valid @RequestBody RedimirPuntosRequest request) {
        Usuario usuario = redimirPuntosUseCase.ejecutar(request.usuarioId(), request.cantidad());
        return ResponseEntity.ok(usuarioMapper.toRegistrarResponse(usuario));
    }
}