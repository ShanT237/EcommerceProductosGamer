package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.PublicarProductoRequest;
import com.uniquindio.backend.application.dto.response.PublicarProductoResponse;
import com.uniquindio.backend.application.usecase.EliminarProductoUseCase;
import com.uniquindio.backend.application.usecase.PublicarProductoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import com.uniquindio.backend.application.dto.request.ModificarProductoRequest;
import com.uniquindio.backend.application.dto.response.ModificarProductoResponse;
import com.uniquindio.backend.application.usecase.ModificarProductoUseCase;
import com.uniquindio.backend.application.dto.request.EditarInformacionProductoRequest;
import com.uniquindio.backend.application.dto.response.EditarInformacionProductoResponse;
import com.uniquindio.backend.application.usecase.EditarInformacionProductoUseCase;

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
    private final EliminarProductoUseCase eliminarProductoUseCase;
    private final ModificarProductoUseCase modificarProductoUseCase;
    private final EditarInformacionProductoUseCase editarInformacionProductoUseCase;
    private final ProductoMapper mapper;

    public ProductoController(PublicarProductoUseCase publicarProductoUseCase,
                              EliminarProductoUseCase eliminarProductoUseCase,
                              ModificarProductoUseCase modificarProductoUseCase,
                              EditarInformacionProductoUseCase editarInformacionProductoUseCase,
                              ProductoMapper mapper) {
        this.publicarProductoUseCase = publicarProductoUseCase;
        this.eliminarProductoUseCase = eliminarProductoUseCase;
        this.modificarProductoUseCase = modificarProductoUseCase;
        this.editarInformacionProductoUseCase = editarInformacionProductoUseCase;
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
        return ResponseEntity.created(location).body(response); // -> 201 Created
    }


    // CU-05 Modificar Producto (precio)
    @PutMapping("/{id}/precio")
    public ResponseEntity<ModificarProductoResponse> modificarPrecio(
            @PathVariable UUID id,
            @Valid @RequestBody ModificarProductoRequest request) {
        Producto producto = modificarProductoUseCase.ejecutar(id, request.vendedorId(), request.nuevoPrecio());
        return ResponseEntity.ok(mapper.toModificarResponse(producto)); // 200 OK
    }

    // CU-06 Eliminar Producto (eliminación lógica, Regla 3)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        eliminarProductoUseCase.ejecutar(id);
        return ResponseEntity.noContent().build(); // -> 204 No Content
    }

    // CU-19 Editar Información General (nombre y descripción)
    @PutMapping("/{id}/informacion")
    public ResponseEntity<EditarInformacionProductoResponse> editarInformacion(
            @PathVariable UUID id,
            @Valid @RequestBody EditarInformacionProductoRequest request) {
        Producto producto = editarInformacionProductoUseCase.ejecutar(
                id, request.vendedorId(), request.nombre(), request.descripcion());
        return ResponseEntity.ok(mapper.toEditarInformacionResponse(producto)); // 200 OK
    }
}