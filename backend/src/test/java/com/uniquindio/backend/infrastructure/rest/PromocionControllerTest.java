package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConfigurarDescuentoPromocionalUseCase;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.PromocionMapper;
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

import java.time.LocalDate;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromocionController.class)
@Import({SecurityConfig.class, PromocionMapper.class})
class PromocionControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ConfigurarDescuentoPromocionalUseCase configurarUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private String cuerpo(String extra, double porcentaje) {
        return """
            { %s "productoId": "%s", "porcentaje": %s, "desde": "2026-12-01", "hasta": "2026-12-31" }
            """.formatted(extra, productoId, porcentaje);
    }

    @Test
    void configurarUsaElVendedorDelToken() throws Exception {
        when(configurarUseCase.ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA))
                .thenReturn(Promocion.programar(UUID.randomUUID(), productoId, 20, DESDE, HASTA));

        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.productoId").value(productoId.toString()))
                .andExpect(jsonPath("$.porcentaje").value(20.0));

        Mockito.verify(configurarUseCase).ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA);
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(configurarUseCase.ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA))
                .thenReturn(Promocion.programar(UUID.randomUUID(), productoId, 20, DESDE, HASTA));

        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("\"vendedorId\": \"%s\",".formatted(ajeno), 20)))
                .andExpect(status().isCreated());

        Mockito.verify(configurarUseCase, Mockito.never())
                .ejecutar(any(), eq(ajeno), Mockito.anyDouble(), any(), any());
    }

    @Test
    void porcentajeNoPositivoRetorna400() throws Exception {
        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 0)))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}