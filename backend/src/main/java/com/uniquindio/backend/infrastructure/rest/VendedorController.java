package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RegistrarVendedorRequest;
import com.uniquindio.backend.application.dto.response.RegistrarVendedorResponse;
import com.uniquindio.backend.application.usecase.RegistrarVendedorUseCase;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.infrastructure.rest.mapper.VendedorMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/vendedores")
public class VendedorController {

    private final RegistrarVendedorUseCase registrarVendedorUseCase;
    private final VendedorMapper mapper;

    public VendedorController(RegistrarVendedorUseCase registrarVendedorUseCase,
                              VendedorMapper mapper) {
        this.registrarVendedorUseCase = registrarVendedorUseCase;
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
                request.documento()
        );

        RegistrarVendedorResponse response = mapper.toRegistrarResponse(vendedor);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vendedor.getId())
                .toUri();
        return ResponseEntity.created(location).body(response); // 201 = created
    }
}