package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AnadirWishlistRequest;
import com.uniquindio.backend.application.dto.response.AnadirWishlistResponse;
import com.uniquindio.backend.application.usecase.AnadirAWishlistUseCase;
import com.uniquindio.backend.application.usecase.QuitarDeWishlistUseCase;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.infrastructure.rest.mapper.WishlistMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final AnadirAWishlistUseCase anadirAWishlistUseCase;
    private final QuitarDeWishlistUseCase quitarDeWishlistUseCase;
    private final WishlistMapper mapper;

    public WishlistController(AnadirAWishlistUseCase anadirAWishlistUseCase,
                              QuitarDeWishlistUseCase quitarDeWishlistUseCase,
                              WishlistMapper mapper) {
        this.anadirAWishlistUseCase = anadirAWishlistUseCase;
        this.quitarDeWishlistUseCase = quitarDeWishlistUseCase;
        this.mapper = mapper;
    }

    // CU-03 Añadir a Wishlist
    @PostMapping
    public ResponseEntity<AnadirWishlistResponse> anadir(@Valid @RequestBody AnadirWishlistRequest request) {
        Wishlist wishlist = anadirAWishlistUseCase.ejecutar(request.usuarioId(), request.productoId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(wishlist.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(wishlist));
    }

    // CU-15 Quitar de Wishlist
    @DeleteMapping("/{productoId}")
    public ResponseEntity<AnadirWishlistResponse> quitar(
            @PathVariable UUID productoId,
            @RequestParam UUID usuarioId) {
        Wishlist wishlist = quitarDeWishlistUseCase.ejecutar(usuarioId, productoId);
        return ResponseEntity.ok(mapper.toResponse(wishlist));
    }
}