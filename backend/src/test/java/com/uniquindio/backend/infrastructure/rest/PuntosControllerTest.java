package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConsultarHistorialPuntosUseCase;
import com.uniquindio.backend.application.usecase.RedimirPuntosUseCase;
import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.MovimientoPuntosMapper;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
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
import java.util.List;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(PuntosController.class)
@Import({SecurityConfig.class, MovimientoPuntosMapper.class})
class PuntosControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private RedimirPuntosUseCase redimirPuntosUseCase;

    @MockitoBean
    private ConsultarHistorialPuntosUseCase consultarHistorialPuntosUseCase;

    @MockitoBean
    private UsuarioMapper usuarioMapper;

    // SecurityConfig necesita el EmisorToken para armar el filtro JWT; las sesiones se simulan con SesionPrueba.
    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deberiaListarElHistorialDelUsuarioDelToken() throws Exception {
        when(consultarHistorialPuntosUseCase.ejecutar(usuarioId)).thenReturn(List.of(
                MovimientoPuntos.redimido(usuarioId, 5, LocalDateTime.of(2026, 10, 1, 10, 0)),
                MovimientoPuntos.ganado(usuarioId, 10, LocalDateTime.of(2026, 9, 1, 10, 0))));

        mockMvc.perform(get("/api/puntos/historial").with(comprador(usuarioId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("REDIMIDO"))
                .andExpect(jsonPath("$[0].cantidad").value(5))
                .andExpect(jsonPath("$[1].tipo").value("GANADO"))
                .andExpect(jsonPath("$[1].cantidad").value(10));
    }

    @Test
    void usuarioSinMovimientosDevuelveListaVacia() throws Exception {
        when(consultarHistorialPuntosUseCase.ejecutar(usuarioId)).thenReturn(List.of());

        mockMvc.perform(get("/api/puntos/historial").with(comprador(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void historialIgnoraUnUsuarioIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(consultarHistorialPuntosUseCase.ejecutar(usuarioId)).thenReturn(List.of());

        mockMvc.perform(get("/api/puntos/historial")
                        .param("usuarioId", ajeno.toString())
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk());

        Mockito.verify(consultarHistorialPuntosUseCase).ejecutar(usuarioId);
        Mockito.verify(consultarHistorialPuntosUseCase, Mockito.never()).ejecutar(ajeno);
    }

    @Test
    void redimirUsaElUsuarioDelToken() throws Exception {
        mockMvc.perform(post("/api/puntos/redimir")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cantidad\": 30 }"))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(redimirPuntosUseCase).ejecutar(usuarioId, 30);
    }

    @Test
    void redimirConCantidadNoPositivaRetorna400() throws Exception {
        mockMvc.perform(post("/api/puntos/redimir")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cantidad\": 0 }"))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(get("/api/puntos/historial"))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}