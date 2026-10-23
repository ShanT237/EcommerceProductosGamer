package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CancelarCompraUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarCompraUseCase;
import com.uniquindio.backend.application.usecase.RealizarCompraUseCase;
import com.uniquindio.backend.application.usecase.SolicitarReembolsoUseCase;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.rest.mapper.CompraMapper;
import com.uniquindio.backend.application.usecase.AplicarCuponUseCase;
import org.springframework.web.context.WebApplicationContext;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.mockito.Mockito;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;

@WebMvcTest(CompraController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class CompraControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // "Postman interno", con Spring Security aplicado

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    @MockitoBean
    private RealizarCompraUseCase realizarCompraUseCase;

    @MockitoBean
    private ConfirmarCompraUseCase confirmarCompraUseCase;

    @MockitoBean
    private CancelarCompraUseCase cancelarCompraUseCase;

    @MockitoBean
    private SolicitarReembolsoUseCase solicitarReembolsoUseCase;

    @MockitoBean
    private AplicarCuponUseCase aplicarCuponUseCase;

    @MockitoBean
    private CompraMapper mapper;

    // SecurityConfig necesita el EmisorToken para armar el filtro JWT; las sesiones se simulan con SesionPrueba.
    @MockitoBean
    private EmisorToken emisorToken;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Compra compraSimulada() {
        return Compra.realizar(UUID.randomUUID(), usuarioId, productoId, 2,
                LocalDateTime.now(), new Precio(250000));
    }

    @Test
    void deberiaCrearCompraUsandoElUsuarioDelToken() throws Exception {
        String requestJson = """
            {
                "productoId": "%s",
                "cantidad": 2
            }
            """.formatted(productoId);

        when(realizarCompraUseCase.ejecutar(any(), any(), anyInt()))
                .thenReturn(compraSimulada());

        mockMvc.perform(post("/api/compras")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())            // 201
                .andExpect(header().exists("Location"));

        Mockito.verify(realizarCompraUseCase).ejecutar(usuarioId, productoId, 2);
    }

    @Test
    void deberiaRetornar400CuandoFaltaProductoId() throws Exception {
        String requestJson = """
            {
                "cantidad": 2
            }
            """;

        mockMvc.perform(post("/api/compras")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());        // 400
    }

    @Test
    void deberiaConfirmarCompraUsandoElUsuarioDelToken() throws Exception {
        Compra compra = compraSimulada();
        when(confirmarCompraUseCase.ejecutar(any(), any())).thenReturn(compra);

        mockMvc.perform(put("/api/compras/{id}/confirmar", compra.getId())
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk());                // 200

        Mockito.verify(confirmarCompraUseCase).ejecutar(compra.getId(), usuarioId);
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\": \"%s\", \"cantidad\": 1}".formatted(productoId)))
                .andExpect(status().isUnauthorized())               // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}