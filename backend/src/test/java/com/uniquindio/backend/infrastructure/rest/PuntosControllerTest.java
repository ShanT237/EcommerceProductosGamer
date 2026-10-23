package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConsultarHistorialPuntosUseCase;
import com.uniquindio.backend.application.usecase.RedimirPuntosUseCase;
import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.infrastructure.rest.mapper.MovimientoPuntosMapper;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PuntosController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MovimientoPuntosMapper.class)
class PuntosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedimirPuntosUseCase redimirPuntosUseCase;

    @MockitoBean
    private ConsultarHistorialPuntosUseCase consultarHistorialPuntosUseCase;

    @MockitoBean
    private UsuarioMapper usuarioMapper;

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deberiaListarElHistorialDePuntosDelUsuario() throws Exception {
        when(consultarHistorialPuntosUseCase.ejecutar(usuarioId)).thenReturn(List.of(
                MovimientoPuntos.redimido(usuarioId, 5, LocalDateTime.of(2026, 10, 1, 10, 0)),
                MovimientoPuntos.ganado(usuarioId, 10, LocalDateTime.of(2026, 9, 1, 10, 0))));

        mockMvc.perform(get("/api/puntos/historial").param("usuarioId", usuarioId.toString()))
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

        mockMvc.perform(get("/api/puntos/historial").param("usuarioId", usuarioId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deberiaRetornar400SiFaltaUsuarioId() throws Exception {
        mockMvc.perform(get("/api/puntos/historial"))
                .andExpect(status().isBadRequest());              // 400
    }
}