package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.BuscarProductosUseCase;
import com.uniquindio.backend.application.usecase.CambiarVisibilidadProductoUseCase;
import com.uniquindio.backend.application.usecase.EditarInformacionProductoUseCase;
import com.uniquindio.backend.application.usecase.EliminarProductoUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductoUseCase;
import com.uniquindio.backend.application.usecase.PublicarProductoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller + caso de uso REAL + repositorios simulados.
 * Así el 404 y el 422 nacen de verdad en el caso de uso y en el dominio,
 * y podemos comprobar que guardar() nunca se llamó cuando algo falla.
 */
@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, ProductoMapper.class, EliminarProductoUseCase.class})
class EliminarProductoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    // Repositorios simulados: los usa el caso de uso real
    @MockitoBean
    private ProductoRepository productoRepository;

    @MockitoBean
    private CompraRepository compraRepository;

    // Resto de dependencias del controller (no se usan en estas pruebas)
    @MockitoBean
    private PublicarProductoUseCase publicarProductoUseCase;

    @MockitoBean
    private ModificarProductoUseCase modificarProductoUseCase;

    @MockitoBean
    private EditarInformacionProductoUseCase editarInformacionProductoUseCase;

    @MockitoBean
    private CambiarVisibilidadProductoUseCase cambiarVisibilidadProductoUseCase;

    @MockitoBean
    private BuscarProductosUseCase buscarProductosUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private Producto producto() {
        return Producto.publicar(productoId, vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    @DisplayName("Eliminar un producto que no existe responde 404 y nunca guarda")
    void eliminarProductoInexistenteRetorna404YNoGuarda() throws Exception {
        // Arrange: el repositorio no encuentra el producto
        when(productoRepository.obtenerPorId(productoId)).thenReturn(Optional.empty());

        // Act
        var resultado = mockMvc.perform(delete("/api/productos/{id}", productoId)
                .with(vendedor(vendedorId)));

        // Assert
        resultado.andExpect(status().isNotFound())                 // 404
                .andExpect(jsonPath("$.status").value(404));
        verify(productoRepository, never()).guardar(any(Producto.class));
    }

    @Test
    @DisplayName("Eliminar un producto con compras activas responde 422 y nunca guarda")
    void eliminarProductoConComprasActivasRetorna422YNoGuarda() throws Exception {
        // Arrange: el producto existe, pero tiene compras activas (Regla 3)
        when(productoRepository.obtenerPorId(productoId)).thenReturn(Optional.of(producto()));
        when(compraRepository.existeCompraActivaDeProducto(productoId)).thenReturn(true);

        // Act
        var resultado = mockMvc.perform(delete("/api/productos/{id}", productoId)
                .with(vendedor(vendedorId)));

        // Assert
        resultado.andExpect(status().isUnprocessableContent())     // 422
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede eliminar un producto que tiene compras activas"));
        verify(productoRepository, never()).guardar(any(Producto.class));
    }

    @Test
    @DisplayName("Control: sin compras activas responde 204 y sí guarda")
    void eliminarProductoSinComprasActivasRetorna204YGuarda() throws Exception {
        // Arrange
        when(productoRepository.obtenerPorId(productoId)).thenReturn(Optional.of(producto()));
        when(compraRepository.existeCompraActivaDeProducto(productoId)).thenReturn(false);

        // Act
        var resultado = mockMvc.perform(delete("/api/productos/{id}", productoId)
                .with(vendedor(vendedorId)));

        // Assert
        resultado.andExpect(status().isNoContent());               // 204
        verify(productoRepository).guardar(any(Producto.class));
    }
}