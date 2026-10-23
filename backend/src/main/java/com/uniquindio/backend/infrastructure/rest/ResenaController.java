package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.SubirResenaRequest;
import com.uniquindio.backend.application.dto.response.SubirResenaResponse;
import com.uniquindio.backend.application.usecase.SubirResenaUseCase;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.infrastructure.rest.mapper.ResenaMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;

import java.net.URI;

@PreAuthorize("hasRole('COMPRADOR')")
@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final SubirResenaUseCase subirResenaUseCase;
    private final ResenaMapper mapper;

    public ResenaController(SubirResenaUseCase subirResenaUseCase, ResenaMapper mapper) {
        this.subirResenaUseCase = subirResenaUseCase;
        this.mapper = mapper;
    }

    // CU-02 Subir Reseña y Asignar Puntos al usuario autenticado
    @PostMapping
    public ResponseEntity<SubirResenaResponse> crear(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody SubirResenaRequest request) {
        Resena resena = subirResenaUseCase.ejecutar(
                request.idCompra(),
                usuarioId,
                request.valorCalificacion(),
                request.comentario()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resena.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(resena));
    }
}