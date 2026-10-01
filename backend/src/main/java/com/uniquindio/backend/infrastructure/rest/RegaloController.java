package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ElegirRegaloRequest;
import com.uniquindio.backend.application.dto.response.ElegirRegaloResponse;
import com.uniquindio.backend.application.usecase.ElegirRegaloUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.rest.mapper.RegaloMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/regalos")
public class RegaloController {

    private final ElegirRegaloUseCase elegirRegaloUseCase;
    private final RegaloMapper mapper;

    public RegaloController(ElegirRegaloUseCase elegirRegaloUseCase, RegaloMapper mapper) {
        this.elegirRegaloUseCase = elegirRegaloUseCase;
        this.mapper = mapper;
    }

    // Elegir Regalo
    @PostMapping("/elegir")
    public ResponseEntity<ElegirRegaloResponse> elegir(@Valid @RequestBody ElegirRegaloRequest request) {
        Regalo regalo = elegirRegaloUseCase.ejecutar(request.regaloId(), request.usuarioId());
        return ResponseEntity.ok(mapper.toElegirResponse(regalo)); // -> 200 OK
    }
}