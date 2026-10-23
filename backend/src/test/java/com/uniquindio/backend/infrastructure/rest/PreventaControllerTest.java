package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ProgramarPreventaUseCase;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.PreventaMapper;
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

@WebMvcTest(PreventaController.class)
@Import({SecurityConfig.class, PreventaMapper.class})
class PreventaControllerTest {

    private static final LocalDate LANZAMIENTO = LocalDate.of(2026, 12, 1);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ProgramarPreventaUseCase programarPreventaUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private String cuerpo(String extra, String descripcion) {
        return """
            { %s "productoId": "%s", "fechaLanzamiento": "2026-12-01", "descripcion": "%s" }
            """.formatted(extra, productoId, descripcion);
    }

    @Test
    void programarUsaElVendedorDelToken() throws Exception {
        when(programarPreventaUseCase.ejecutar(vendedorId, productoId, LANZAMIENTO, "Edición coleccionista"))
                .thenReturn(Preventa.programar(UUID.randomUUID(), productoId, LANZAMIENTO, "Edición coleccionista"));

        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "Edición coleccionista")))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.productoId").value(productoId.toString()))
                .andExpect(jsonPath("$.fechaLanzamiento").value("2026-12-01"));

        Mockito.verify(programarPreventaUseCase)
                .ejecutar(vendedorId, productoId, LANZAMIENTO, "Edición coleccionista");
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(programarPreventaUseCase.ejecutar(vendedorId, productoId, LANZAMIENTO, "Lanzamiento"))
                .thenReturn(Preventa.programar(UUID.randomUUID(), productoId, LANZAMIENTO, "Lanzamiento"));

        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("\"vendedorId\": \"%s\",".formatted(ajeno), "Lanzamiento")))
                .andExpect(status().isCreated());

        Mockito.verify(programarPreventaUseCase, Mockito.never())
                .ejecutar(eq(ajeno), any(), any(), any());
    }

    @Test
    void descripcionVaciaRetorna400() throws Exception {
        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "")))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/preventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "Lanzamiento")))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}