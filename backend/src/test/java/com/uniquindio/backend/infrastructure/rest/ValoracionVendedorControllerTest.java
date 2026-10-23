package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ValorarVendedorUseCase;
import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ValoracionVendedorMapper;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ValoracionVendedorController.class)
@Import({SecurityConfig.class, ValoracionVendedorMapper.class})
class ValoracionVendedorControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ValorarVendedorUseCase valorarVendedorUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID compraId = UUID.randomUUID();
    private final UUID vendedorId = UUID.randomUUID();

    private ValoracionVendedor valoracion(int estrellas, String comentario) {
        return ValoracionVendedor.crear(UUID.randomUUID(), compraId, usuarioId, vendedorId, true,
                new Calificacion(estrellas), comentario, LocalDateTime.of(2026, 10, 22, 10, 0));
    }

    @Test
    void valorarUsaElUsuarioDelToken() throws Exception {
        when(valorarVendedorUseCase.ejecutar(compraId, usuarioId, 5, "Excelente atención"))
                .thenReturn(valoracion(5, "Excelente atención"));

        String json = """
            { "compraId": "%s", "estrellas": 5, "comentario": "Excelente atención" }
            """.formatted(compraId);

        mockMvc.perform(post("/api/valoraciones-vendedor")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()))
                .andExpect(jsonPath("$.estrellas").value(5));

        Mockito.verify(valorarVendedorUseCase).ejecutar(compraId, usuarioId, 5, "Excelente atención");
    }

    @Test
    void ignoraUnUsuarioIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(valorarVendedorUseCase.ejecutar(compraId, usuarioId, 4, null))
                .thenReturn(valoracion(4, null));

        String json = """
            { "compraId": "%s", "usuarioId": "%s", "estrellas": 4 }
            """.formatted(compraId, ajeno);

        mockMvc.perform(post("/api/valoraciones-vendedor")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        Mockito.verify(valorarVendedorUseCase).ejecutar(compraId, usuarioId, 4, null);
        Mockito.verify(valorarVendedorUseCase, Mockito.never()).ejecutar(compraId, ajeno, 4, null);
    }

    @Test
    void estrellasFueraDeRangoRetorna400() throws Exception {
        String json = """
            { "compraId": "%s", "estrellas": 0 }
            """.formatted(compraId);

        mockMvc.perform(post("/api/valoraciones-vendedor")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        String json = """
            { "compraId": "%s", "estrellas": 5 }
            """.formatted(compraId);

        mockMvc.perform(post("/api/valoraciones-vendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}