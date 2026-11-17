package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CambiarContrasenaRequest;
import com.uniquindio.backend.application.dto.request.EditarPerfilRequest;
import com.uniquindio.backend.application.dto.request.RegistrarVendedorRequest;
import com.uniquindio.backend.application.dto.response.RegistrarVendedorResponse;
import com.uniquindio.backend.application.usecase.CambiarContrasenaVendedorUseCase;
import com.uniquindio.backend.application.usecase.DarDeBajaVendedorUseCase;
import com.uniquindio.backend.application.usecase.EditarPerfilVendedorUseCase;
import com.uniquindio.backend.application.usecase.ObtenerPerfilVendedorUseCase;
import com.uniquindio.backend.application.usecase.RegistrarVendedorUseCase;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.infrastructure.rest.mapper.VendedorMapper;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * El registro es público. La consulta/edición del perfil, el cambio de contraseña y la baja usan
 * el {@code vendedorId} del token JWT (Authentication): cada vendedor solo opera sobre su propia cuenta.
 */
@RestController
@RequestMapping("/api/vendedores")
public class VendedorController {

    private final RegistrarVendedorUseCase registrarVendedorUseCase;
    private final DarDeBajaVendedorUseCase darDeBajaVendedorUseCase;
    private final ObtenerPerfilVendedorUseCase obtenerPerfilVendedorUseCase;
    private final EditarPerfilVendedorUseCase editarPerfilVendedorUseCase;
    private final CambiarContrasenaVendedorUseCase cambiarContrasenaVendedorUseCase;
    private final VendedorMapper mapper;

    public VendedorController(RegistrarVendedorUseCase registrarVendedorUseCase,
                              DarDeBajaVendedorUseCase darDeBajaVendedorUseCase,
                              ObtenerPerfilVendedorUseCase obtenerPerfilVendedorUseCase,
                              EditarPerfilVendedorUseCase editarPerfilVendedorUseCase,
                              CambiarContrasenaVendedorUseCase cambiarContrasenaVendedorUseCase,
                              VendedorMapper mapper) {
        this.registrarVendedorUseCase = registrarVendedorUseCase;
        this.darDeBajaVendedorUseCase = darDeBajaVendedorUseCase;
        this.obtenerPerfilVendedorUseCase = obtenerPerfilVendedorUseCase;
        this.editarPerfilVendedorUseCase = editarPerfilVendedorUseCase;
        this.cambiarContrasenaVendedorUseCase = cambiarContrasenaVendedorUseCase;
        this.mapper = mapper;
    }

    // CU-11 Registrar Vendedor
    @SecurityRequirements // público
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

    // CU-26 Consultar Perfil del vendedor autenticado (para precargar el formulario)
    @PreAuthorize("hasRole('VENDEDOR')")
    @GetMapping("/me")
    public ResponseEntity<RegistrarVendedorResponse> obtenerPerfil(@AuthenticationPrincipal UUID vendedorId) {
        return ResponseEntity.ok(mapper.toRegistrarResponse(obtenerPerfilVendedorUseCase.ejecutar(vendedorId))); // 200 OK
    }

    // CU-26 Editar Perfil (nombre y correo) del vendedor autenticado; tipo y documento no se editan
    @PreAuthorize("hasRole('VENDEDOR')")
    @PutMapping("/me/perfil")
    public ResponseEntity<RegistrarVendedorResponse> editarPerfil(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody EditarPerfilRequest request) {
        Vendedor vendedor = editarPerfilVendedorUseCase.ejecutar(vendedorId, request.nombre(), request.correo());
        return ResponseEntity.ok(mapper.toRegistrarResponse(vendedor)); // 200 OK
    }

    // CU-26 Cambiar Contraseña del vendedor autenticado (exige la actual; la nueva debe ser segura y distinta)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PutMapping("/me/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody CambiarContrasenaRequest request) {
        cambiarContrasenaVendedorUseCase.ejecutar(vendedorId, request.contrasenaActual(), request.contrasenaNueva());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // CU-25 Dar de Baja al vendedor autenticado (baja lógica)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PutMapping("/me/baja")
    public ResponseEntity<RegistrarVendedorResponse> darDeBaja(@AuthenticationPrincipal UUID vendedorId) {
        Vendedor vendedor = darDeBajaVendedorUseCase.ejecutar(vendedorId);
        return ResponseEntity.ok(mapper.toRegistrarResponse(vendedor));
    }
}
