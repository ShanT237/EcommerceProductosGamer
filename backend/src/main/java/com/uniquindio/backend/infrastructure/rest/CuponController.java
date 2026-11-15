package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CrearCuponRequest;
import com.uniquindio.backend.application.dto.response.CuponResponse;
import com.uniquindio.backend.application.usecase.ConsultarCuponUseCase;
import com.uniquindio.backend.application.usecase.ConsultarCuponesVendedorUseCase;
import com.uniquindio.backend.application.usecase.CrearCuponUseCase;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.infrastructure.rest.mapper.CuponMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Cupones: el vendedor los crea y consulta los propios; el comprador solo puede ver el detalle
 * de un código antes de pagar (vista previa). El cupón se consume al crear el pedido
 * (POST /api/pedidos), un uso por pedido. El vendedor sale siempre del token JWT.
 * Cada método declara su rol porque la clase mezcla VENDEDOR y COMPRADOR.
 */
@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private final CrearCuponUseCase crearCuponUseCase;
    private final ConsultarCuponesVendedorUseCase consultarCuponesVendedorUseCase;
    private final ConsultarCuponUseCase consultarCuponUseCase;
    private final CuponMapper mapper;

    public CuponController(CrearCuponUseCase crearCuponUseCase,
                           ConsultarCuponesVendedorUseCase consultarCuponesVendedorUseCase,
                           ConsultarCuponUseCase consultarCuponUseCase,
                           CuponMapper mapper) {
        this.crearCuponUseCase = crearCuponUseCase;
        this.consultarCuponesVendedorUseCase = consultarCuponesVendedorUseCase;
        this.consultarCuponUseCase = consultarCuponUseCase;
        this.mapper = mapper;
    }

    // Crear un cupón propio (solo aplica a los productos del vendedor)
    @PreAuthorize("hasRole('VENDEDOR')")
    @PostMapping
    public ResponseEntity<CuponResponse> crear(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody CrearCuponRequest request) {
        Cupon cupon = crearCuponUseCase.ejecutar(UUID.randomUUID(), vendedorId, request.codigo(),
                request.porcentaje(), request.vence(), request.usosMaximos());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{codigo}")
                .buildAndExpand(cupon.getCodigo())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(cupon)); // 201
    }

    // Cupones propios del vendedor
    @PreAuthorize("hasRole('VENDEDOR')")
    @GetMapping
    public ResponseEntity<List<CuponResponse>> listar(@AuthenticationPrincipal UUID vendedorId) {
        return ResponseEntity.ok(consultarCuponesVendedorUseCase.ejecutar(vendedorId).stream()
                .map(mapper::toResponse)
                .toList()); // 200
    }

    // Vista previa para el comprador: 404 si no existe; 422 si está vencido, agotado o ya lo usó
    @PreAuthorize("hasRole('COMPRADOR')")
    @GetMapping("/{codigo}")
    public ResponseEntity<CuponResponse> consultar(
            @PathVariable String codigo,
            @AuthenticationPrincipal UUID usuarioId) {
        return ResponseEntity.ok(mapper.toResponse(consultarCuponUseCase.ejecutar(codigo, usuarioId))); // 200
    }
}
