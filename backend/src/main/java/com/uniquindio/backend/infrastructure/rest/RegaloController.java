package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CrearRegaloRequest;
import com.uniquindio.backend.application.dto.response.ProgresoRegalosResponse;
import com.uniquindio.backend.application.dto.response.RegaloResponse;
import com.uniquindio.backend.application.usecase.AsignarRegaloPorMetaUseCase;
import com.uniquindio.backend.application.usecase.CancelarRegaloUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarEntregaRegaloUseCase;
import com.uniquindio.backend.application.usecase.ConsultarMisRegalosUseCase;
import com.uniquindio.backend.application.usecase.ConsultarProgresoRegalosUseCase;
import com.uniquindio.backend.application.usecase.ConsultarRegalosDisponiblesUseCase;
import com.uniquindio.backend.application.usecase.ConsultarRegalosPorEnviarUseCase;
import com.uniquindio.backend.application.usecase.CrearRegaloUseCase;
import com.uniquindio.backend.application.usecase.EnviarRegaloUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.rest.mapper.RegaloMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * El {@code usuarioId} sale del token JWT (Authentication): el regalo se asigna al usuario
 * autenticado y solo él puede confirmar su entrega.
 */
@PreAuthorize("hasRole('COMPRADOR')")
@RestController
@RequestMapping("/api/regalos")
public class RegaloController {

    private final AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase;
    private final EnviarRegaloUseCase enviarRegaloUseCase;
    private final CancelarRegaloUseCase cancelarRegaloUseCase;
    private final ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase;
    private final CrearRegaloUseCase crearRegaloUseCase;
    private final ConsultarRegalosDisponiblesUseCase consultarRegalosDisponiblesUseCase;
    private final ConsultarMisRegalosUseCase consultarMisRegalosUseCase;
    private final ConsultarRegalosPorEnviarUseCase consultarRegalosPorEnviarUseCase;
    private final ConsultarProgresoRegalosUseCase consultarProgresoRegalosUseCase;
    private final RegaloMapper mapper;

    public RegaloController(AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase,
                            EnviarRegaloUseCase enviarRegaloUseCase,
                            CancelarRegaloUseCase cancelarRegaloUseCase,
                            ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase,
                            CrearRegaloUseCase crearRegaloUseCase,
                            ConsultarRegalosDisponiblesUseCase consultarRegalosDisponiblesUseCase,
                            ConsultarMisRegalosUseCase consultarMisRegalosUseCase,
                            ConsultarRegalosPorEnviarUseCase consultarRegalosPorEnviarUseCase,
                            ConsultarProgresoRegalosUseCase consultarProgresoRegalosUseCase,
                            RegaloMapper mapper) {
        this.asignarRegaloPorMetaUseCase = asignarRegaloPorMetaUseCase;
        this.enviarRegaloUseCase = enviarRegaloUseCase;
        this.cancelarRegaloUseCase = cancelarRegaloUseCase;
        this.confirmarEntregaRegaloUseCase = confirmarEntregaRegaloUseCase;
        this.crearRegaloUseCase = crearRegaloUseCase;
        this.consultarRegalosDisponiblesUseCase = consultarRegalosDisponiblesUseCase;
        this.consultarMisRegalosUseCase = consultarMisRegalosUseCase;
        this.consultarRegalosPorEnviarUseCase = consultarRegalosPorEnviarUseCase;
        this.consultarProgresoRegalosUseCase = consultarProgresoRegalosUseCase;
        this.mapper = mapper;
    }

    // Crear un regalo de un producto o combo propio (solo vendedor; el id sale del token)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PostMapping
    public ResponseEntity<RegaloResponse> crear(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody CrearRegaloRequest request) {
        Regalo regalo = crearRegaloUseCase.ejecutar(
                UUID.randomUUID(), vendedorId, request.productoId(), request.comboId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(regalo.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(regalo)); // 201 = created
    }

    // Regalos que el comprador puede elegir (estado DISPONIBLE)
    @GetMapping("/disponibles")
    public ResponseEntity<List<RegaloResponse>> disponibles() {
        List<RegaloResponse> respuesta = consultarRegalosDisponiblesUseCase.ejecutar().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }

    // Regalos del comprador autenticado
    @GetMapping("/mios")
    public ResponseEntity<List<RegaloResponse>> mios(@AuthenticationPrincipal UUID usuarioId) {
        List<RegaloResponse> respuesta = consultarMisRegalosUseCase.ejecutar(usuarioId).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }

    // Cuántas compras lleva el comprador y cuántos regalos puede elegir
    @GetMapping("/progreso")
    public ResponseEntity<ProgresoRegalosResponse> progreso(@AuthenticationPrincipal UUID usuarioId) {
        return ResponseEntity.ok(consultarProgresoRegalosUseCase.ejecutar(usuarioId)); // 200 OK
    }

    // Regalos elegidos que el vendedor autenticado debe enviar
    @PreAuthorize("hasRole('VENDEDOR')")
    @GetMapping("/por-enviar")
    public ResponseEntity<List<RegaloResponse>> porEnviar(@AuthenticationPrincipal UUID vendedorId) {
        List<RegaloResponse> respuesta = consultarRegalosPorEnviarUseCase.ejecutar(vendedorId).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }

    // CU-10 Asignar Regalo por Meta al usuario autenticado (5 compras COMPLETADAS por regalo)
    @PutMapping("/{id}/asignar")
    public ResponseEntity<RegaloResponse> asignar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        Regalo regalo = asignarRegaloPorMetaUseCase.ejecutar(usuarioId, id);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }

    // CU-24 Enviar Regalo (solo el vendedor dueño del producto o combo; el id sale del token)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PutMapping("/{id}/enviar")
    public ResponseEntity<RegaloResponse> enviar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId) {
        Regalo regalo = enviarRegaloUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }

    // Cancelar Regalo (solo el vendedor dueño del producto o combo; el id sale del token)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<RegaloResponse> cancelar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId) {
        Regalo regalo = cancelarRegaloUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }

    // CU-24 Confirmar Entrega (solo el destinatario autenticado, y solo si fue enviado)
    @PutMapping("/{id}/entrega")
    public ResponseEntity<RegaloResponse> confirmarEntrega(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        Regalo regalo = confirmarEntregaRegaloUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }
}
