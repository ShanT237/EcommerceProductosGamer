package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RegistrarVendedorRequest;
import com.uniquindio.backend.application.dto.response.RegistrarVendedorResponse;
import com.uniquindio.backend.application.usecase.DarDeBajaVendedorUseCase;
import com.uniquindio.backend.application.usecase.RegistrarVendedorUseCase;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.infrastructure.rest.mapper.VendedorMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * El registro es público. La baja usa el {@code vendedorId} del token JWT (Authentication):
 * cada vendedor solo puede darse de baja a sí mismo.
 */
@RestController
@RequestMapping("/api/vendedores")
public class VendedorController {

    private final RegistrarVendedorUseCase registrarVendedorUseCase;
    private final DarDeBajaVendedorUseCase darDeBajaVendedorUseCase;
    private final VendedorMapper mapper;

    public VendedorController(RegistrarVendedorUseCase registrarVendedorUseCase,
                              DarDeBajaVendedorUseCase darDeBajaVendedorUseCase,
                              VendedorMapper mapper) {
        this.registrarVendedorUseCase = registrarVendedorUseCase;
        this.darDeBajaVendedorUseCase = darDeBajaVendedorUseCase;
        this.mapper = mapper;
    }

    // CU-11 Registrar Vendedor
    @PostMapping
    public ResponseEntity<RegistrarVendedorResponse> registrar(
            @Valid @RequestBody RegistrarVendedorRequest request) {
        Vendedor vendedor = registrarVendedorUseCase.ejecutar(
                UUID.randomUUID(),
                request.nombre(),
                request.correo(),
                request.tipo(),
                request.documento(),
                request.contrasena()
        );

        RegistrarVendedorResponse response = mapper.toRegistrarResponse(vendedor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vendedor.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // CU-25 Dar de Baja al vendedor autenticado (baja lógica)
    @PutMapping("/me/baja")
    public ResponseEntity<RegistrarVendedorResponse> darDeBaja(@AuthenticationPrincipal UUID vendedorId) {
        Vendedor vendedor = darDeBajaVendedorUseCase.ejecutar(vendedorId);
        return ResponseEntity.ok(mapper.toRegistrarResponse(vendedor));
    }
}