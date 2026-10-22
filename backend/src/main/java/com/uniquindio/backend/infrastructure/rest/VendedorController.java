package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RegistrarVendedorRequest;
import com.uniquindio.backend.application.dto.response.RegistrarVendedorResponse;
import com.uniquindio.backend.application.usecase.DarDeBajaVendedorUseCase;
import com.uniquindio.backend.application.usecase.RegistrarVendedorUseCase;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.infrastructure.rest.mapper.VendedorMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Limitación: no hay autenticación ni autorización. Cualquiera podría dar de baja
 * a un vendedor por id (CU-27 pendiente).
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

    // CU-25 Dar de Baja Vendedor
    @PutMapping("/{id}/baja")
    public ResponseEntity<RegistrarVendedorResponse> darDeBaja(@PathVariable UUID id) {
        Vendedor vendedor = darDeBajaVendedorUseCase.ejecutar(id);
        return ResponseEntity.ok(mapper.toRegistrarResponse(vendedor));
    }
}