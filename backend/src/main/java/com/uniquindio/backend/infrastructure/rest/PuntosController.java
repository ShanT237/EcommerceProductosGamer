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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * El {@code usuarioId} sale del token JWT (Authentication): cada usuario solo redime
 * y consulta sus propios puntos.
 */
@PreAuthorize("hasRole('COMPRADOR')")
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
    public ResponseEntity<PuntosResponse> redimir(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody RedimirPuntosRequest request) {
        Usuario usuario = redimirPuntosUseCase.ejecutar(usuarioId, request.cantidad());
        return ResponseEntity.ok(usuarioMapper.toPuntosResponse(usuario));
    }

    // CU-32 Consultar Historial de Puntos del usuario autenticado (más reciente primero)
    @GetMapping("/historial")
    public ResponseEntity<List<MovimientoPuntosResponse>> historial(@AuthenticationPrincipal UUID usuarioId) {
        List<MovimientoPuntosResponse> respuesta = consultarHistorialPuntosUseCase.ejecutar(usuarioId).stream()
                .map(movimientoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }
}