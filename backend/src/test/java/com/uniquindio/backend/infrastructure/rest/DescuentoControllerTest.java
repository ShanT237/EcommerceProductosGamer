package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.AplicarDescuentoUseCase;
import com.uniquindio.backend.application.usecase.QuitarDescuentoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
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

import java.util.NoSuchElementException;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private QuitarDescuentoUseCase quitarDescuentoUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @MockitoBean
    private VerificadorCuentaActiva verificadorCuentaActiva;

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

    @Test
    void aplicarDevuelveElPrecioOriginalYElPorcentaje() throws Exception {
        when(aplicarDescuentoUseCase.ejecutar(productoId, vendedorId, 20.0))
                .thenReturn(productoConDescuento());

        mockMvc.perform(post("/api/descuentos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuevoPrecio").value(80.0))
                .andExpect(jsonPath("$.precioOriginal").value(100.0))
                .andExpect(jsonPath("$.porcentajeDescuento").value(20.0));
    }

    @Test
    void quitarUsaElVendedorDelTokenYDevuelveElPrecioRestaurado() throws Exception {
        Producto sinDescuento = productoConDescuento();
        sinDescuento.quitarDescuento();
        when(quitarDescuentoUseCase.ejecutar(productoId, vendedorId)).thenReturn(sinDescuento);

        mockMvc.perform(delete("/api/descuentos/{productoId}", productoId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(productoId.toString()))
                .andExpect(jsonPath("$.nuevoPrecio").value(100.0))
                .andExpect(jsonPath("$.precioOriginal").doesNotExist())
                .andExpect(jsonPath("$.porcentajeDescuento").doesNotExist());

        Mockito.verify(quitarDescuentoUseCase).ejecutar(productoId, vendedorId);
    }

    @Test
    void quitarSinDescuentoActivoRetorna422() throws Exception {
        when(quitarDescuentoUseCase.ejecutar(productoId, vendedorId))
                .thenThrow(new ReglaDominioException("El producto no tiene un descuento activo"));

        mockMvc.perform(delete("/api/descuentos/{productoId}", productoId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isUnprocessableContent());    // 422
    }

    @Test
    void quitarDeProductoInexistenteRetorna404() throws Exception {
        when(quitarDescuentoUseCase.ejecutar(productoId, vendedorId))
                .thenThrow(new NoSuchElementException("El producto no existe"));

        mockMvc.perform(delete("/api/descuentos/{productoId}", productoId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void quitarSinTokenRetorna401() throws Exception {
        mockMvc.perform(delete("/api/descuentos/{productoId}", productoId))
                .andExpect(status().isUnauthorized());            // 401
    }

    @Test
    void unCompradorNoPuedeQuitarRetorna403() throws Exception {
        mockMvc.perform(delete("/api/descuentos/{productoId}", productoId)
                        .with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden());               // 403
    }
}