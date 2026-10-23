package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CancelarCompraUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarCompraUseCase;
import com.uniquindio.backend.application.usecase.RealizarCompraUseCase;
import com.uniquindio.backend.application.usecase.SolicitarReembolsoUseCase;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.rest.mapper.CompraMapper;
import com.uniquindio.backend.application.usecase.AplicarCuponUseCase;

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

@WebMvcTest(CompraController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompraControllerTest {

    @Autowired // "Postman interno"
    private MockMvc mockMvc;

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

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Compra compraSimulada() {
        return Compra.realizar(UUID.randomUUID(), usuarioId, productoId, 2,
                LocalDateTime.now(), new Precio(250000));
    }

    @Test
    void deberiaCrearCompraCuandoDatosValidos() throws Exception {
        // Arrange
        String requestJson = """
            {
                "usuarioId": "%s",
                "productoId": "%s",
                "cantidad": 2
            }
            """.formatted(usuarioId, productoId);

        when(realizarCompraUseCase.ejecutar(any(), any(), anyInt()))
                .thenReturn(compraSimulada());

        // Act & Assert
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())            // 201
                .andExpect(header().exists("Location"));
    }

    @Test
    void deberiaRetornar400CuandoFaltaUsuarioId() throws Exception {
        // Arrange: falta usuarioId
        String requestJson = """
            {
                "productoId": "%s",
                "cantidad": 2
            }
            """.formatted(productoId);

        // Act & Assert
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());        // 400
    }

    @Test
    void deberiaConfirmarCompraCuandoDatosValidos() throws Exception {
        // Arrange
        Compra compra = compraSimulada();
        when(confirmarCompraUseCase.ejecutar(any(), any())).thenReturn(compra);

        // Act & Assert
        mockMvc.perform(put("/api/compras/{id}/confirmar", compra.getId())
                        .param("usuarioId", usuarioId.toString()))
                .andExpect(status().isOk());                // 200
    }
}