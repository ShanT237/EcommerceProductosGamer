package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.ProgresoRegalosResponse;
import com.uniquindio.backend.application.usecase.AsignarRegaloPorMetaUseCase;
import com.uniquindio.backend.application.usecase.CancelarRegaloUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarEntregaRegaloUseCase;
import com.uniquindio.backend.application.usecase.ConsultarMisRegalosUseCase;
import com.uniquindio.backend.application.usecase.ConsultarProgresoRegalosUseCase;
import com.uniquindio.backend.application.usecase.ConsultarRegalosDisponiblesUseCase;
import com.uniquindio.backend.application.usecase.ConsultarRegalosPorEnviarUseCase;
import com.uniquindio.backend.application.usecase.CrearRegaloUseCase;
import com.uniquindio.backend.application.usecase.EnviarRegaloUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.RegaloMapper;
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

import java.util.List;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegaloController.class)
@Import({SecurityConfig.class, RegaloMapper.class})
class RegaloControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase;

    @MockitoBean
    private EnviarRegaloUseCase enviarRegaloUseCase;

    @MockitoBean
    private CancelarRegaloUseCase cancelarRegaloUseCase;

    @MockitoBean
    private ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase;

    @MockitoBean
    private CrearRegaloUseCase crearRegaloUseCase;

    @MockitoBean
    private ConsultarRegalosDisponiblesUseCase consultarRegalosDisponiblesUseCase;

    @MockitoBean
    private ConsultarMisRegalosUseCase consultarMisRegalosUseCase;

    @MockitoBean
    private ConsultarRegalosPorEnviarUseCase consultarRegalosPorEnviarUseCase;

    @MockitoBean
    private ConsultarProgresoRegalosUseCase consultarProgresoRegalosUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @MockitoBean
    private VerificadorCuentaActiva verificadorCuentaActiva;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID regaloId = UUID.randomUUID();

    private Regalo regaloElegido() {
        Regalo regalo = Regalo.crear(regaloId, UUID.randomUUID(), null);
        regalo.elegir(usuarioId, false);
        return regalo;
    }

    private Regalo regaloEntregado() {
        Regalo regalo = regaloElegido();
        regalo.enviar();
        regalo.confirmarEntrega(usuarioId);
        return regalo;
    }

    // ===================== asignar =====================

    @Test
    void asignarUsaElUsuarioDelToken() throws Exception {
        when(asignarRegaloPorMetaUseCase.ejecutar(usuarioId, regaloId)).thenReturn(regaloElegido());

        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.estado").value("ELEGIDO"))
                .andExpect(jsonPath("$.idUsuario").value(usuarioId.toString()));

        Mockito.verify(asignarRegaloPorMetaUseCase).ejecutar(usuarioId, regaloId);
    }

    @Test
    void asignarIgnoraUnUsuarioIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(asignarRegaloPorMetaUseCase.ejecutar(usuarioId, regaloId)).thenReturn(regaloElegido());

        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId)
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"usuarioId\": \"%s\" }".formatted(ajeno)))
                .andExpect(status().isOk());

        Mockito.verify(asignarRegaloPorMetaUseCase, Mockito.never()).ejecutar(ajeno, regaloId);
    }

    @Test
    void asignarSinAlcanzarLaMetaRetorna422() throws Exception {
        when(asignarRegaloPorMetaUseCase.ejecutar(usuarioId, regaloId))
                .thenThrow(new ReglaDominioException("El usuario aún no alcanza la meta"));

        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isUnprocessableContent());              // 422
    }

    // ===================== enviar =====================

    @Test
    void enviarDevuelveElRegaloEnviado() throws Exception {
        Regalo enviado = regaloElegido();
        enviado.enviar();
        UUID vendedorId = UUID.randomUUID();
        when(enviarRegaloUseCase.ejecutar(regaloId, vendedorId)).thenReturn(enviado);

        mockMvc.perform(put("/api/regalos/{id}/enviar", regaloId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));

        Mockito.verify(enviarRegaloUseCase).ejecutar(regaloId, vendedorId);
    }

    @Test
    void unCompradorNoPuedeEnviarRegalosRetorna403() throws Exception {
        mockMvc.perform(put("/api/regalos/{id}/enviar", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(enviarRegaloUseCase);
    }
    // ===================== entrega =====================

    @Test
    void confirmarEntregaUsaElUsuarioDelToken() throws Exception {
        when(confirmarEntregaRegaloUseCase.ejecutar(regaloId, usuarioId)).thenReturn(regaloEntregado());

        mockMvc.perform(put("/api/regalos/{id}/entrega", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));

        Mockito.verify(confirmarEntregaRegaloUseCase).ejecutar(regaloId, usuarioId);
    }

    @Test
    void confirmarEntregaIgnoraUnUsuarioIdEnElBody() throws Exception {
        UUID destinatario = UUID.randomUUID();
        when(confirmarEntregaRegaloUseCase.ejecutar(regaloId, usuarioId))
                .thenThrow(new ReglaDominioException("Solo el usuario destinatario puede confirmar la recepción"));

        mockMvc.perform(put("/api/regalos/{id}/entrega", regaloId)
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"usuarioId\": \"%s\" }".formatted(destinatario)))
                .andExpect(status().isUnprocessableContent());              // 422

        Mockito.verify(confirmarEntregaRegaloUseCase, Mockito.never()).ejecutar(regaloId, destinatario);
    }

    // ===================== crear =====================

    @Test
    void crearUsaElVendedorDelTokenYDevuelve201() throws Exception {
        UUID vendedorId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        when(crearRegaloUseCase.ejecutar(Mockito.any(UUID.class), Mockito.eq(vendedorId),
                Mockito.eq(productoId), Mockito.isNull()))
                .thenReturn(Regalo.crear(regaloId, productoId, null));

        mockMvc.perform(post("/api/regalos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"productoId\": \"%s\" }".formatted(productoId)))
                .andExpect(status().isCreated())                    // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.idProducto").value(productoId.toString()));
    }

    @Test
    void crearConProductoYComboRetorna422() throws Exception {
        when(crearRegaloUseCase.ejecutar(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new ReglaDominioException(
                        "Un regalo debe estar asociado a un producto o a un combo, pero no a ambos"));

        mockMvc.perform(post("/api/regalos")
                        .with(vendedor(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"productoId\": \"%s\", \"comboId\": \"%s\" }"
                                .formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isUnprocessableContent());      // 422
    }

    @Test
    void compradorNoPuedeCrearRetorna403() throws Exception {
        mockMvc.perform(post("/api/regalos")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"productoId\": \"%s\" }".formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());                 // 403

        Mockito.verifyNoInteractions(crearRegaloUseCase);
    }

    // ===================== consultas =====================

    @Test
    void disponiblesDevuelveLaListaParaElComprador() throws Exception {
        when(consultarRegalosDisponiblesUseCase.ejecutar())
                .thenReturn(List.of(Regalo.crear(regaloId, UUID.randomUUID(), null)));

        mockMvc.perform(get("/api/regalos/disponibles").with(comprador(usuarioId)))
                .andExpect(status().isOk())                         // 200
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"));
    }

    @Test
    void miosUsaElUsuarioDelToken() throws Exception {
        when(consultarMisRegalosUseCase.ejecutar(usuarioId)).thenReturn(List.of(regaloElegido()));

        mockMvc.perform(get("/api/regalos/mios").with(comprador(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(usuarioId.toString()));

        Mockito.verify(consultarMisRegalosUseCase).ejecutar(usuarioId);
    }

    @Test
    void progresoUsaElUsuarioDelToken() throws Exception {
        when(consultarProgresoRegalosUseCase.ejecutar(usuarioId))
                .thenReturn(new ProgresoRegalosResponse(7, 5, 0, 1, 3));

        mockMvc.perform(get("/api/regalos/progreso").with(comprador(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprasCompletadas").value(7))
                .andExpect(jsonPath("$.regalosDisponibles").value(1))
                .andExpect(jsonPath("$.comprasParaSiguiente").value(3));
    }

    @Test
    void porEnviarUsaElVendedorDelToken() throws Exception {
        UUID vendedorId = UUID.randomUUID();
        when(consultarRegalosPorEnviarUseCase.ejecutar(vendedorId)).thenReturn(List.of(regaloElegido()));

        mockMvc.perform(get("/api/regalos/por-enviar").with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ELEGIDO"));

        Mockito.verify(consultarRegalosPorEnviarUseCase).ejecutar(vendedorId);
    }

    @Test
    void compradorNoPuedeVerLosRegalosPorEnviarRetorna403() throws Exception {
        mockMvc.perform(get("/api/regalos/por-enviar").with(comprador(usuarioId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(consultarRegalosPorEnviarUseCase);
    }

    @Test
    void vendedorNoPuedeVerLosRegalosDisponiblesRetorna403() throws Exception {
        mockMvc.perform(get("/api/regalos/disponibles").with(vendedor(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(consultarRegalosDisponiblesUseCase);
    }

    // ===================== sin token =====================

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ===================== cancelar =====================

    @Test
    void cancelarUsaElVendedorDelToken() throws Exception {
        UUID vendedorId = UUID.randomUUID();
        Regalo cancelado = regaloElegido();
        cancelado.cancelarRegalo();
        when(cancelarRegaloUseCase.ejecutar(regaloId, vendedorId)).thenReturn(cancelado);

        mockMvc.perform(put("/api/regalos/{id}/cancelar", regaloId)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        Mockito.verify(cancelarRegaloUseCase).ejecutar(regaloId, vendedorId);
    }

    @Test
    void unCompradorNoPuedeCancelarRetorna403() throws Exception {
        mockMvc.perform(put("/api/regalos/{id}/cancelar", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(cancelarRegaloUseCase);
    }

    @Test
    void cancelarSinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(put("/api/regalos/{id}/cancelar", regaloId))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(cancelarRegaloUseCase);
    }
}