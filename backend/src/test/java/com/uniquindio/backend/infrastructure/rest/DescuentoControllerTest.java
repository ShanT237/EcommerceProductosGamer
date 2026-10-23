package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.AplicarDescuentoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
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

import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DescuentoController.class)
@Import(SecurityConfig.class)
class DescuentoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private AplicarDescuentoUseCase aplicarDescuentoUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Producto productoConDescuento() {
        Producto p = Producto.publicar(productoId, vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        p.aplicarDescuento(20);
        return p;
    }

    private String cuerpo(String extra, double porcentaje) {
        return """
            { %s "productoId": "%s", "porcentaje": %s }
            """.formatted(extra, productoId, porcentaje);
    }

    @Test
    void aplicarUsaElVendedorDelToken() throws Exception {
        when(aplicarDescuentoUseCase.ejecutar(productoId, vendedorId, 20.0))
                .thenReturn(productoConDescuento());

        mockMvc.perform(post("/api/descuentos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(productoId.toString()))
                .andExpect(jsonPath("$.nuevoPrecio").value(80.0));

        Mockito.verify(aplicarDescuentoUseCase).ejecutar(productoId, vendedorId, 20.0);
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(aplicarDescuentoUseCase.ejecutar(productoId, vendedorId, 20.0))
                .thenReturn(productoConDescuento());

        mockMvc.perform(post("/api/descuentos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("\"vendedorId\": \"%s\",".formatted(ajeno), 20)))
                .andExpect(status().isOk());

        Mockito.verify(aplicarDescuentoUseCase, Mockito.never())
                .ejecutar(any(), eq(ajeno), Mockito.anyDouble());
    }

    @Test
    void porcentajeNoPositivoRetorna400() throws Exception {
        mockMvc.perform(post("/api/descuentos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 0)))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/descuentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}