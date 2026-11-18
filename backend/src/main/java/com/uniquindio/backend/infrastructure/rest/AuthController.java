package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.IniciarSesionRequest;
import com.uniquindio.backend.application.dto.request.RestablecerContrasenaRequest;
import com.uniquindio.backend.application.dto.request.SolicitarRecuperacionRequest;
import com.uniquindio.backend.application.dto.response.IniciarSesionResponse;
import com.uniquindio.backend.application.dto.response.MensajeResponse;
import com.uniquindio.backend.application.usecase.IniciarSesionUseCase;
import com.uniquindio.backend.application.usecase.RestablecerContrasenaUseCase;
import com.uniquindio.backend.application.usecase.SolicitarRecuperacionContrasenaUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirements // público: sin candado en Swagger
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IniciarSesionUseCase iniciarSesionUseCase;
    private final SolicitarRecuperacionContrasenaUseCase solicitarRecuperacionUseCase;
    private final RestablecerContrasenaUseCase restablecerContrasenaUseCase;

    public AuthController(IniciarSesionUseCase iniciarSesionUseCase,
                          SolicitarRecuperacionContrasenaUseCase solicitarRecuperacionUseCase,
                          RestablecerContrasenaUseCase restablecerContrasenaUseCase) {
        this.iniciarSesionUseCase = iniciarSesionUseCase;
        this.solicitarRecuperacionUseCase = solicitarRecuperacionUseCase;
        this.restablecerContrasenaUseCase = restablecerContrasenaUseCase;
    }

    // CU-27 Iniciar Sesión
    @PostMapping("/login")
    public ResponseEntity<IniciarSesionResponse> iniciarSesion(@Valid @RequestBody IniciarSesionRequest request) {
        String token = iniciarSesionUseCase.ejecutar(request.correo(), request.contrasena());
        return ResponseEntity.ok(new IniciarSesionResponse(token));
    }

    // Recuperación de contraseña, paso 1: pedir el enlace por correo.
    // Responde siempre lo mismo, exista o no el correo, para no revelar qué cuentas hay.
    @PostMapping("/olvide-contrasena")
    public ResponseEntity<MensajeResponse> olvideContrasena(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        solicitarRecuperacionUseCase.ejecutar(request.correo());
        return ResponseEntity.ok(new MensajeResponse(
                "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña."));
    }

    // Recuperación de contraseña, paso 2: elegir la contraseña nueva con el token del enlace.
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<MensajeResponse> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaRequest request) {
        restablecerContrasenaUseCase.ejecutar(request.token(), request.contrasenaNueva());
        return ResponseEntity.ok(new MensajeResponse("Tu contraseña se actualizó. Ya puedes ingresar."));
    }
}
