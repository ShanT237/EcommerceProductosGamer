package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.PublicarProductoRequest;
import com.uniquindio.backend.application.dto.response.PublicarProductoResponse;
import com.uniquindio.backend.application.usecase.PublicarProductoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/productos")
public class ProductoController {

    private final PublicarProductoUseCase publicarProductoUseCase;
    private final ProductoMapper mapper;

    public ProductoController(PublicarProductoUseCase publicarProductoUseCase,
                              ProductoMapper mapper) {
        this.publicarProductoUseCase = publicarProductoUseCase;
        this.mapper = mapper;
    }

    // CU-04 Publicar Producto
    @PostMapping
    public ResponseEntity<PublicarProductoResponse> publicar(@Valid @RequestBody PublicarProductoRequest request) {
        Producto producto = publicarProductoUseCase.ejecutar(
                UUID.randomUUID(),
                request.vendedorId(),
                request.nombre(),
                request.descripcion(),
                request.precio(),
                request.stock(),
                request.gama(),
                request.exclusividad()
        );

        PublicarProductoResponse response = mapper.toPublicarResponse(producto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(producto.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
