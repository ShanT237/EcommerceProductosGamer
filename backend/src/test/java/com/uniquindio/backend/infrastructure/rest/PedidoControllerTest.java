package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CancelarPedidoUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarPedidoUseCase;
import com.uniquindio.backend.application.usecase.CrearPedidoUseCase;
import com.uniquindio.backend.application.usecase.ItemPedido;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class PedidoControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // "Postman interno", con Spring Security aplicado

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    @MockitoBean
    private CrearPedidoUseCase crearPedidoUseCase;

    @MockitoBean
    private ConfirmarPedidoUseCase confirmarPedidoUseCase;

    @MockitoBean
    private CancelarPedidoUseCase cancelarPedidoUseCase;

    // SecurityConfig necesita el EmisorToken para armar el filtro JWT; las sesiones se simulan con SesionTest.
    @MockitoBean
    private EmisorToken emisorToken;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID pedidoId = UUID.randomUUID();
    private final UUID teclado = UUID.randomUUID();
    private final UUID mouse = UUID.randomUUID();

    private List<Compra> lineasSimuladas() {
        return List.of(
                Compra.realizar(UUID.randomUUID(), usuarioId, teclado, 2, LocalDateTime.now(),
                        new Precio(100000), pedidoId),
                Compra.realizar(UUID.randomUUID(), usuarioId, mouse, 1, LocalDateTime.now(),
                        new Precio(50000), pedidoId));
    }

    private String pedidoJson() {
        return """
                { "items": [
                    { "productoId": "%s", "cantidad": 2 },
                    { "productoId": "%s", "cantidad": 1 }
                ] }
                """.formatted(teclado, mouse);
    }

    @Test
    void crearUsaElUsuarioDelToken() throws Exception {
        when(crearPedidoUseCase.ejecutar(any(), any())).thenReturn(lineasSimuladas());

        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson()))
                .andExpect(status().isCreated())                         // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.compras.length()").value(2))
                .andExpect(jsonPath("$.compras[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.compras[0].pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.total").value(250000.0));

        Mockito.verify(crearPedidoUseCase).ejecutar(usuarioId,
                List.of(new ItemPedido(teclado, 2), new ItemPedido(mouse, 1)));
    }

    @Test
    void crearSinItemsRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [] }"))
                .andExpect(status().isBadRequest());                     // 400

        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(crearPedidoUseCase);
    }

    @Test
    void crearConCantidadCeroRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"productoId\": \"%s\", \"cantidad\": 0 } ] }".formatted(teclado)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(crearPedidoUseCase);
    }

    @Test
    void crearConUnaLineaSinProductoRetorna400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [ { \"cantidad\": 1 } ] }"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(crearPedidoUseCase);
    }

    @Test
    void crearConMasDeCincuentaLineasRetorna400() throws Exception {
        String lineas = IntStream.range(0, 51)
                .mapToObj(i -> "{ \"productoId\": \"%s\", \"cantidad\": 1 }".formatted(UUID.randomUUID()))
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [" + lineas + "] }"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(crearPedidoUseCase);
    }

    @Test
    void unaReglaDeDominioVioladaRetorna422() throws Exception {
        when(crearPedidoUseCase.ejecutar(any(), any()))
                .thenThrow(new ReglaDominioException("No hay suficiente stock disponible"));

        mockMvc.perform(post("/api/pedidos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson()))
                .andExpect(status().isUnprocessableContent())            // 422
                .andExpect(jsonPath("$.mensaje").value("No hay suficiente stock disponible"));
    }

    @Test
    void confirmarUsaElUsuarioDelTokenYDevuelve200() throws Exception {
        when(confirmarPedidoUseCase.ejecutar(any(), any())).thenReturn(lineasSimuladas());

        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk())                              // 200
                .andExpect(jsonPath("$.pedidoId").value(pedidoId.toString()))
                .andExpect(jsonPath("$.compras.length()").value(2));

        Mockito.verify(confirmarPedidoUseCase).ejecutar(pedidoId, usuarioId);
    }

    @Test
    void cancelarUsaElUsuarioDelTokenYDevuelve200() throws Exception {
        when(cancelarPedidoUseCase.ejecutar(any(), any())).thenReturn(lineasSimuladas());

        mockMvc.perform(put("/api/pedidos/{id}/cancelar", pedidoId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk())                              // 200
                .andExpect(jsonPath("$.total").value(250000.0));

        Mockito.verify(cancelarPedidoUseCase).ejecutar(pedidoId, usuarioId);
    }

    @Test
    void confirmarUnPedidoInexistenteRetorna404() throws Exception {
        when(confirmarPedidoUseCase.ejecutar(any(), any()))
                .thenThrow(new NoSuchElementException("El pedido no existe"));

        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isNotFound());                       // 404
    }

    @Test
    void cancelarUnPedidoAjenoRetorna422() throws Exception {
        when(cancelarPedidoUseCase.ejecutar(any(), any()))
                .thenThrow(new ReglaDominioException("La compra no pertenece a este usuario"));

        mockMvc.perform(put("/api/pedidos/{id}/cancelar", pedidoId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isUnprocessableContent());           // 422
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson()))
                .andExpect(status().isUnauthorized())                    // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId))
                .andExpect(status().isUnauthorized());

        Mockito.verifyNoInteractions(crearPedidoUseCase, confirmarPedidoUseCase, cancelarPedidoUseCase);
    }

    @Test
    void unVendedorNoPuedeOperarPedidosRetorna403EnJson() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(vendedor(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoJson()))
                .andExpect(status().isForbidden())                       // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId).with(vendedor(usuarioId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/pedidos/{id}/cancelar", pedidoId).with(vendedor(usuarioId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(crearPedidoUseCase, confirmarPedidoUseCase, cancelarPedidoUseCase);
    }
}
