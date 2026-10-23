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
import com.uniquindio.backend.application.dto.request.CambiarVisibilidadProductoRequest;
import com.uniquindio.backend.application.dto.response.CambiarVisibilidadProductoResponse;
import com.uniquindio.backend.application.usecase.CambiarVisibilidadProductoUseCase;
import com.uniquindio.backend.application.dto.request.BuscarProductosRequest;
import com.uniquindio.backend.application.dto.response.BuscarProductoResponse;
import com.uniquindio.backend.application.usecase.BuscarProductosUseCase;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.net.URI;
import java.util.UUID;

@PreAuthorize("hasRole('VENDEDOR')")
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final PublicarProductoUseCase publicarProductoUseCase;
    private final EliminarProductoUseCase eliminarProductoUseCase;
    private final ModificarProductoUseCase modificarProductoUseCase;
    private final EditarInformacionProductoUseCase editarInformacionProductoUseCase;
    private final CambiarVisibilidadProductoUseCase cambiarVisibilidadProductoUseCase;
    private final BuscarProductosUseCase buscarProductosUseCase;
    private final ProductoMapper mapper;

    public ProductoController(PublicarProductoUseCase publicarProductoUseCase,
                              EliminarProductoUseCase eliminarProductoUseCase,
                              ModificarProductoUseCase modificarProductoUseCase,
                              EditarInformacionProductoUseCase editarInformacionProductoUseCase,
                              CambiarVisibilidadProductoUseCase cambiarVisibilidadProductoUseCase,
                              BuscarProductosUseCase buscarProductosUseCase,
                              ProductoMapper mapper) {
        this.publicarProductoUseCase = publicarProductoUseCase;
        this.eliminarProductoUseCase = eliminarProductoUseCase;
        this.modificarProductoUseCase = modificarProductoUseCase;
        this.editarInformacionProductoUseCase = editarInformacionProductoUseCase;
        this.cambiarVisibilidadProductoUseCase = cambiarVisibilidadProductoUseCase;
        this.buscarProductosUseCase = buscarProductosUseCase;
        this.mapper = mapper;
    }

    // CU-04 Publicar Producto
    @PostMapping
    public ResponseEntity<PublicarProductoResponse> publicar(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody PublicarProductoRequest request) {
        Producto producto = publicarProductoUseCase.ejecutar(
                UUID.randomUUID(),
                vendedorId,
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
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ModificarProductoRequest request) {
        Producto producto = modificarProductoUseCase.ejecutar(id, vendedorId, request.nuevoPrecio());
        return ResponseEntity.ok(mapper.toModificarResponse(producto)); // 200 OK
    }

    // CU-06 Eliminar Producto (eliminación lógica, Regla 3) del vendedor autenticado
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId) {
        eliminarProductoUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.noContent().build(); // -> 204 No Content
    }
    // CU-19 Editar Información General (nombre y descripción)
    @PutMapping("/{id}/informacion")
    public ResponseEntity<EditarInformacionProductoResponse> editarInformacion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody EditarInformacionProductoRequest request) {
        Producto producto = editarInformacionProductoUseCase.ejecutar(
                id, vendedorId, request.nombre(), request.descripcion());
        return ResponseEntity.ok(mapper.toEditarInformacionResponse(producto)); // 200 OK
    }

    // CU-20 Filtrar y Buscar Productos (solo visibles: no eliminados ni pausados)
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<BuscarProductoResponse> buscar(BuscarProductosRequest request) {
        return buscarProductosUseCase.ejecutar(request.aFiltro()).stream()
                .map(mapper::toBuscarResponse)
                .toList();
    }

    // CU-21 Activar / Pausar Publicación
    @PutMapping("/{id}/visibilidad")
    public ResponseEntity<CambiarVisibilidadProductoResponse> cambiarVisibilidad(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody CambiarVisibilidadProductoRequest request) {
        Producto producto = cambiarVisibilidadProductoUseCase.ejecutar(
                id, vendedorId, request.activo());
        return ResponseEntity.ok(mapper.toVisibilidadResponse(producto)); // 200 OK
    }
}