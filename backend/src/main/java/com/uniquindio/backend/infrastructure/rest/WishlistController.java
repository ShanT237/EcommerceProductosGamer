package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AnadirWishlistRequest;
import com.uniquindio.backend.application.dto.response.AnadirWishlistResponse;
import com.uniquindio.backend.application.dto.response.BuscarProductoResponse;
import com.uniquindio.backend.application.usecase.AnadirAWishlistUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPromocionesVigentesUseCase;
import com.uniquindio.backend.application.usecase.ConsultarWishlistUseCase;
import com.uniquindio.backend.application.usecase.QuitarDeWishlistUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import com.uniquindio.backend.infrastructure.rest.mapper.WishlistMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PreAuthorize("hasRole('COMPRADOR')")
@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final AnadirAWishlistUseCase anadirAWishlistUseCase;
    private final QuitarDeWishlistUseCase quitarDeWishlistUseCase;
    private final ConsultarWishlistUseCase consultarWishlistUseCase;
    private final ConsultarPromocionesVigentesUseCase consultarPromocionesVigentesUseCase;
    private final WishlistMapper mapper;
    private final ProductoMapper productoMapper;

    public WishlistController(AnadirAWishlistUseCase anadirAWishlistUseCase,
                              QuitarDeWishlistUseCase quitarDeWishlistUseCase,
                              ConsultarWishlistUseCase consultarWishlistUseCase,
                              ConsultarPromocionesVigentesUseCase consultarPromocionesVigentesUseCase,
                              WishlistMapper mapper,
                              ProductoMapper productoMapper) {
        this.anadirAWishlistUseCase = anadirAWishlistUseCase;
        this.quitarDeWishlistUseCase = quitarDeWishlistUseCase;
        this.consultarWishlistUseCase = consultarWishlistUseCase;
        this.consultarPromocionesVigentesUseCase = consultarPromocionesVigentesUseCase;
        this.mapper = mapper;
        this.productoMapper = productoMapper;
    }

    // Consultar los favoritos del usuario autenticado (lista vacía si aún no tiene wishlist)
    @GetMapping
    public ResponseEntity<List<BuscarProductoResponse>> listar(@AuthenticationPrincipal UUID usuarioId) {
        List<Producto> productos = consultarWishlistUseCase.ejecutar(usuarioId);
        Map<UUID, Promocion> promociones = consultarPromocionesVigentesUseCase.ejecutar(
                productos.stream().map(Producto::getId).toList());
        List<BuscarProductoResponse> respuesta = productos.stream()
                .map(p -> productoMapper.toBuscarResponse(p, promociones.get(p.getId())))
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }

    // CU-03 Añadir a Wishlist del usuario autenticado
    @PostMapping
    public ResponseEntity<AnadirWishlistResponse> anadir(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody AnadirWishlistRequest request) {
        Wishlist wishlist = anadirAWishlistUseCase.ejecutar(usuarioId, request.productoId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wishlist.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(wishlist));
    }

    // CU-15 Quitar de Wishlist del usuario autenticado
    @DeleteMapping("/{productoId}")
    public ResponseEntity<AnadirWishlistResponse> quitar(
            @PathVariable UUID productoId,
            @AuthenticationPrincipal UUID usuarioId) {
        Wishlist wishlist = quitarDeWishlistUseCase.ejecutar(usuarioId, productoId);
        return ResponseEntity.ok(mapper.toResponse(wishlist));
    }
}
