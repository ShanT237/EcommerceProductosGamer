package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.IniciarSesionRequest;
import com.uniquindio.backend.application.dto.response.IniciarSesionResponse;
import com.uniquindio.backend.application.usecase.IniciarSesionUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirements // público: sin candado en Swagger
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesionUseCase;

    public AuthController(IniciarSesionUseCase iniciarSesionUseCase) {
        this.iniciarSesionUseCase = iniciarSesionUseCase;
    }

    // CU-27 Iniciar Sesión
    @PostMapping("/login")
    public ResponseEntity<IniciarSesionResponse> iniciarSesion(@Valid @RequestBody IniciarSesionRequest request) {
        String token = iniciarSesionUseCase.ejecutar(request.correo(), request.contrasena());
        return ResponseEntity.ok(new IniciarSesionResponse(token));
    }
}
