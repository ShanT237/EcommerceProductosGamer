package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RegistrarUsuarioRequest;
import com.uniquindio.backend.application.dto.response.RegistrarUsuarioResponse;
import com.uniquindio.backend.application.usecase.RegistrarUsuarioUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import com.uniquindio.backend.application.dto.request.EditarPerfilRequest;
import com.uniquindio.backend.application.usecase.EditarPerfilUseCase;

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
    private final UsuarioMapper mapper;

    public UsuarioController(RegistrarUsuarioUseCase registrarUsuarioUseCase,
                             EditarPerfilUseCase editarPerfilUseCase,
                             UsuarioMapper mapper) {
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.editarPerfilUseCase = editarPerfilUseCase;
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

    // CU-26 Editar Perfil (nombre y correo)
    @PutMapping("/{id}/perfil")
    public ResponseEntity<RegistrarUsuarioResponse> editarPerfil(
            @PathVariable UUID id,
            @Valid @RequestBody EditarPerfilRequest request) {
        Usuario usuario = editarPerfilUseCase.ejecutar(id, request.nombre(), request.correo());
        return ResponseEntity.ok(mapper.toRegistrarResponse(usuario)); // 200 OK
    }
}