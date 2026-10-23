package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RegistrarUsuarioRequest;
import com.uniquindio.backend.application.dto.response.RegistrarUsuarioResponse;
import com.uniquindio.backend.application.usecase.RegistrarUsuarioUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import com.uniquindio.backend.application.dto.request.EditarPerfilRequest;
import com.uniquindio.backend.application.usecase.EditarPerfilUseCase;
import com.uniquindio.backend.application.dto.request.CambiarContrasenaRequest;
import com.uniquindio.backend.application.usecase.CambiarContrasenaUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final EditarPerfilUseCase editarPerfilUseCase;
    private final CambiarContrasenaUseCase cambiarContrasenaUseCase;
    private final UsuarioMapper mapper;

    public UsuarioController(RegistrarUsuarioUseCase registrarUsuarioUseCase,
                             EditarPerfilUseCase editarPerfilUseCase,
                             CambiarContrasenaUseCase cambiarContrasenaUseCase,
                             UsuarioMapper mapper) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.editarPerfilUseCase = editarPerfilUseCase;
        this.cambiarContrasenaUseCase = cambiarContrasenaUseCase;
        this.mapper = mapper;
    }

    // CU-11 Registrar Usuario
    @PostMapping
    public ResponseEntity<RegistrarUsuarioResponse> registrar(
            @Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario = registrarUsuarioUseCase.ejecutar(
                UUID.randomUUID(),
                request.nombre(),
                request.correo(),
                request.contrasena()
        );

        RegistrarUsuarioResponse response = mapper.toRegistrarResponse(usuario);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(usuario.getId())
                .toUri();
        return ResponseEntity.created(location).body(response); // 201 = created
    }

    // CU-26 Editar Perfil (nombre y correo) del usuario autenticado
    @PutMapping("/me/perfil")
    public ResponseEntity<RegistrarUsuarioResponse> editarPerfil(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody EditarPerfilRequest request) {
        Usuario usuario = editarPerfilUseCase.ejecutar(usuarioId, request.nombre(), request.correo());
        return ResponseEntity.ok(mapper.toRegistrarResponse(usuario)); // 200 OK
    }

    // CU-26 Cambiar Contraseña del usuario autenticado (exige la actual; la nueva debe ser segura y distinta)
    @PutMapping("/me/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody CambiarContrasenaRequest request) {
        cambiarContrasenaUseCase.ejecutar(usuarioId, request.contrasenaActual(), request.contrasenaNueva());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}