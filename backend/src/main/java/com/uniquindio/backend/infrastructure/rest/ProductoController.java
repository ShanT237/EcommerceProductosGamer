package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.PublicarProductoRequest;
import com.uniquindio.backend.application.dto.response.PublicarProductoResponse;
import com.uniquindio.backend.application.usecase.EliminarProductoUseCase;
import com.uniquindio.backend.application.usecase.PublicarProductoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Pagina;
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
import com.uniquindio.backend.application.dto.response.PaginaResponse;
import com.uniquindio.backend.application.usecase.BuscarProductosUseCase;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.net.URI;
import java.util.UUID;

@RestController    // Maneja peticiones HTTP -> JSON
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

    // CU-20 Filtrar y Buscar Productos (solo visibles: no eliminados ni pausados)
    @GetMapping
    public PaginaResponse<BuscarProductoResponse> buscar(
            @ModelAttribute BuscarProductosRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pagina<Producto> pagina = buscarProductosUseCase.ejecutar(request.aFiltro(), page, size);
        List<BuscarProductoResponse> contenido = pagina.contenido().stream()
                .map(mapper::toBuscarResponse)
                .toList();
        return new PaginaResponse<>(
                contenido,
                pagina.pagina(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }

    // CU-21 Activar / Pausar Publicación
    @PutMapping("/{id}/visibilidad")
    public ResponseEntity<CambiarVisibilidadProductoResponse> cambiarVisibilidad(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarVisibilidadProductoRequest request) {
        Producto producto = cambiarVisibilidadProductoUseCase.ejecutar(
                id, request.vendedorId(), request.activo());
        return ResponseEntity.ok(mapper.toVisibilidadResponse(producto)); // 200 OK
    }
}