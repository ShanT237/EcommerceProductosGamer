package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ConfigurarDescuentoPromocionalRequest;
import com.uniquindio.backend.application.dto.request.ModificarPromocionRequest;
import com.uniquindio.backend.application.dto.response.PromocionResponse;
import com.uniquindio.backend.application.dto.response.PromocionVendedorResponse;
import com.uniquindio.backend.application.usecase.ConfigurarDescuentoPromocionalUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPromocionesDelVendedorUseCase;
import com.uniquindio.backend.application.usecase.ModificarPromocionUseCase;
import com.uniquindio.backend.application.usecase.QuitarPromocionUseCase;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.infrastructure.rest.mapper.PromocionMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * El {@code vendedorId} sale del token JWT (Authentication): el caso de uso verifica
 * que el producto pertenezca a ese vendedor.
 */
@PreAuthorize("hasRole('VENDEDOR')")
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final ConfigurarDescuentoPromocionalUseCase configurarUseCase;
    private final ModificarPromocionUseCase modificarUseCase;
    private final QuitarPromocionUseCase quitarUseCase;
    private final ConsultarPromocionesDelVendedorUseCase consultarUseCase;
    private final PromocionMapper mapper;

    public PromocionController(ConfigurarDescuentoPromocionalUseCase configurarUseCase,
                               ModificarPromocionUseCase modificarUseCase,
                               QuitarPromocionUseCase quitarUseCase,
                               ConsultarPromocionesDelVendedorUseCase consultarUseCase,
                               PromocionMapper mapper) {
        this.configurarUseCase = configurarUseCase;
        this.modificarUseCase = modificarUseCase;
        this.quitarUseCase = quitarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.mapper = mapper;
    }

    // CU-22 Configurar Descuento Promocional (con vigencia; no cambia el precio base)
    @PostMapping
    public ResponseEntity<PromocionResponse> configurar(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ConfigurarDescuentoPromocionalRequest request) {
        Promocion promocion = configurarUseCase.ejecutar(
                request.productoId(), vendedorId, request.porcentaje(), request.desde(), request.hasta());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(promocion.getId())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(promocion)); // 201
    }

    // Promociones de temporada de los productos del vendedor (pasadas, vigentes y futuras)
    @GetMapping
    public ResponseEntity<List<PromocionVendedorResponse>> listar(@AuthenticationPrincipal UUID vendedorId) {
        return ResponseEntity.ok(consultarUseCase.ejecutar(vendedorId).stream()
                .map(mapper::toVendedorResponse)
                .toList()); // 200
    }

    // Modificar porcentaje y vigencia
    @PutMapping("/{id}")
    public ResponseEntity<PromocionResponse> modificar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ModificarPromocionRequest request) {
        Promocion promocion = modificarUseCase.ejecutar(
                id, vendedorId, request.porcentaje(), request.desde(), request.hasta());
        return ResponseEntity.ok(mapper.toResponse(promocion)); // 200
    }

    // Quitar la promoción
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> quitar(@PathVariable UUID id, @AuthenticationPrincipal UUID vendedorId) {
        quitarUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.noContent().build(); // 204
    }
}