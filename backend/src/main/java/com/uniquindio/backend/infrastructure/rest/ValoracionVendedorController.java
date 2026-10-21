package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ValorarVendedorRequest;
import com.uniquindio.backend.application.dto.response.ValoracionVendedorResponse;
import com.uniquindio.backend.application.usecase.ValorarVendedorUseCase;
import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.infrastructure.rest.mapper.ValoracionVendedorMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Limitación: el {@code usuarioId} viaja en el body; el JWT valida la sesión pero
 * no se cruza con el usuario que valora.
 */
@RestController
@RequestMapping("/api/valoraciones-vendedor")
public class ValoracionVendedorController {

    private final ValorarVendedorUseCase valorarVendedorUseCase;
    private final ValoracionVendedorMapper mapper;

    public ValoracionVendedorController(ValorarVendedorUseCase valorarVendedorUseCase,
                                        ValoracionVendedorMapper mapper) {
        this.valorarVendedorUseCase = valorarVendedorUseCase;
        this.mapper = mapper;
    }

    // CU-28 Valorar Vendedor (una valoración por compra COMPLETADA)
    @PostMapping
    public ResponseEntity<ValoracionVendedorResponse> valorar(
            @Valid @RequestBody ValorarVendedorRequest request) {
        ValoracionVendedor valoracion = valorarVendedorUseCase.ejecutar(
                request.compraId(),
                request.usuarioId(),
                request.estrellas(),
                request.comentario()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(valoracion.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(valoracion)); // 201 Created
    }
}