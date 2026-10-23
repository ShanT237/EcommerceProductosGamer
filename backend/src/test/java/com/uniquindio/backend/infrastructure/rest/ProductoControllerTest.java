package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.BuscarProductosUseCase;
import com.uniquindio.backend.application.usecase.CambiarVisibilidadProductoUseCase;
import com.uniquindio.backend.application.usecase.EditarInformacionProductoUseCase;
import com.uniquindio.backend.application.usecase.EliminarProductoUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductoUseCase;
import com.uniquindio.backend.application.usecase.PublicarProductoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, ProductoMapper.class})
class ProductoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private PublicarProductoUseCase publicarProductoUseCase;

    @MockitoBean
    private EliminarProductoUseCase eliminarProductoUseCase;

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

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Producto producto() {
        return Producto.publicar(productoId, vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
    }

    // ===================== publicar =====================

    private String cuerpoPublicar(String extra) {
        return """
            { %s "nombre": "Mouse", "descripcion": "Mouse RGB", "precio": 100,
              "stock": 5, "gama": "MEDIA", "exclusividad": "NORMAL" }
            """.formatted(extra);
    }

    @Test
    void publicarUsaElVendedorDelToken() throws Exception {
        when(publicarProductoUseCase.ejecutar(any(), eq(vendedorId), eq("Mouse"), eq("Mouse RGB"),
                eq(100.0), eq(5), eq(Gama.MEDIA), eq(Exclusividad.NORMAL))).thenReturn(producto());

        mockMvc.perform(post("/api/productos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPublicar("")))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()));

        Mockito.verify(publicarProductoUseCase).ejecutar(any(), eq(vendedorId), eq("Mouse"),
                eq("Mouse RGB"), eq(100.0), eq(5), eq(Gama.MEDIA), eq(Exclusividad.NORMAL));
    }

    @Test
    void publicarIgnoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(publicarProductoUseCase.ejecutar(any(), eq(vendedorId), any(), any(),
                Mockito.anyDouble(), Mockito.anyInt(), any(), any())).thenReturn(producto());

        mockMvc.perform(post("/api/productos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPublicar("\"vendedorId\": \"%s\",".formatted(ajeno))))
                .andExpect(status().isCreated());

        Mockito.verify(publicarProductoUseCase, Mockito.never()).ejecutar(any(), eq(ajeno), any(), any(),
                Mockito.anyDouble(), Mockito.anyInt(), any(), any());
    }

    @Test
    void publicarConPrecioNoPositivoRetorna400() throws Exception {
        String json = """
            { "nombre": "Mouse", "descripcion": "Mouse RGB", "precio": 0,
              "stock": 5, "gama": "MEDIA", "exclusividad": "NORMAL" }
            """;

        mockMvc.perform(post("/api/productos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== precio =====================

    @Test
    void modificarPrecioUsaElVendedorDelToken() throws Exception {
        when(modificarProductoUseCase.ejecutar(productoId, vendedorId, 80.0)).thenReturn(producto());

        mockMvc.perform(put("/api/productos/{id}/precio", productoId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nuevoPrecio\": 80 }"))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(modificarProductoUseCase).ejecutar(productoId, vendedorId, 80.0);
    }

    @Test
    void modificarPrecioConValorNoPositivoRetorna400() throws Exception {
        mockMvc.perform(put("/api/productos/{id}/precio", productoId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nuevoPrecio\": 0 }"))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== información =====================

    @Test
    void editarInformacionUsaElVendedorDelToken() throws Exception {
        when(editarInformacionProductoUseCase.ejecutar(productoId, vendedorId, "Mouse Pro", "Inalámbrico"))
                .thenReturn(producto());

        mockMvc.perform(put("/api/productos/{id}/informacion", productoId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Mouse Pro\", \"descripcion\": \"Inalámbrico\" }"))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(editarInformacionProductoUseCase)
                .ejecutar(productoId, vendedorId, "Mouse Pro", "Inalámbrico");
    }

    // ===================== visibilidad =====================

    @Test
    void cambiarVisibilidadUsaElVendedorDelToken() throws Exception {
        Producto pausado = producto();
        pausado.pausar();
        when(cambiarVisibilidadProductoUseCase.ejecutar(productoId, vendedorId, false)).thenReturn(pausado);

        mockMvc.perform(put("/api/productos/{id}/visibilidad", productoId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"activo\": false }"))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.pausado").value(true));

        Mockito.verify(cambiarVisibilidadProductoUseCase).ejecutar(productoId, vendedorId, false);
    }

    @Test
    void cambiarVisibilidadSinActivoRetorna400() throws Exception {
        mockMvc.perform(put("/api/productos/{id}/visibilidad", productoId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== eliminar =====================

    @Test
    void eliminarUsaElVendedorDelToken() throws Exception {
        mockMvc.perform(delete("/api/productos/{id}", productoId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isNoContent());               // 204

        Mockito.verify(eliminarProductoUseCase).ejecutar(productoId, vendedorId);
    }

    @Test
    void eliminarSinTokenRetorna401() throws Exception {
        mockMvc.perform(delete("/api/productos/{id}", productoId))
                .andExpect(status().isUnauthorized());            // 401

        Mockito.verifyNoInteractions(eliminarProductoUseCase);
    }

    // ===================== sin token =====================

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPublicar("")))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void unCompradorNoPuedePublicarRetorna403EnJson() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(comprador(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPublicar("")))
                .andExpect(status().isForbidden())                // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(publicarProductoUseCase);
    }

    @Test
    void cualquierSesionPuedeBuscarProductos() throws Exception {
        mockMvc.perform(get("/api/productos").with(comprador(UUID.randomUUID())))
                .andDo(print())
                .andExpect(status().isOk());                      // 200
    }
}