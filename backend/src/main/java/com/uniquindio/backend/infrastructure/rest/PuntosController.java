package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RedimirPuntosRequest;
import com.uniquindio.backend.application.dto.response.MovimientoPuntosResponse;
import com.uniquindio.backend.application.dto.response.PuntosResponse;
import com.uniquindio.backend.application.usecase.ConsultarHistorialPuntosUseCase;
import com.uniquindio.backend.application.usecase.RedimirPuntosUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.rest.mapper.MovimientoPuntosMapper;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Limitación: el {@code usuarioId} viaja en el body o como parámetro; el JWT valida la
 * sesión pero no se cruza con el usuario consultado (cualquiera podría redimir o ver
 * los puntos de otro usuario).
 */
@RestController
@RequestMapping("/api/puntos")
public class PuntosController {

    private final RedimirPuntosUseCase redimirPuntosUseCase;
    private final ConsultarHistorialPuntosUseCase consultarHistorialPuntosUseCase;
    private final UsuarioMapper usuarioMapper;
    private final MovimientoPuntosMapper movimientoMapper;

    public PuntosController(RedimirPuntosUseCase redimirPuntosUseCase,
                            ConsultarHistorialPuntosUseCase consultarHistorialPuntosUseCase,
                            UsuarioMapper usuarioMapper,
                            MovimientoPuntosMapper movimientoMapper) {
        this.redimirPuntosUseCase = redimirPuntosUseCase;
        this.consultarHistorialPuntosUseCase = consultarHistorialPuntosUseCase;
        this.usuarioMapper = usuarioMapper;
        this.movimientoMapper = movimientoMapper;
    }

    // CU-08 Redimir Puntos por Beneficios
    @PostMapping("/redimir")
    public ResponseEntity<PuntosResponse> redimir(@Valid @RequestBody RedimirPuntosRequest request) {
        Usuario usuario = redimirPuntosUseCase.ejecutar(request.usuarioId(), request.cantidad());
        return ResponseEntity.ok(usuarioMapper.toPuntosResponse(usuario));
    }

    // CU-32 Consultar Historial de Puntos (más reciente primero)
    @GetMapping("/historial")
    public ResponseEntity<List<MovimientoPuntosResponse>> historial(@RequestParam UUID usuarioId) {
        List<MovimientoPuntosResponse> respuesta = consultarHistorialPuntosUseCase.ejecutar(usuarioId).stream()
                .map(movimientoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }
}