package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.AsignarRegaloPorMetaUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarEntregaRegaloUseCase;
import com.uniquindio.backend.application.usecase.EnviarRegaloUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
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

import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

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
    void asignarSinAlcanzarLaMetaRetorna400() throws Exception {
        when(asignarRegaloPorMetaUseCase.ejecutar(usuarioId, regaloId))
                .thenThrow(new ReglaDominioException("El usuario aún no alcanza la meta"));

        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== enviar =====================

    @Test
    void enviarDevuelveElRegaloEnviado() throws Exception {
        Regalo enviado = regaloElegido();
        enviado.enviar();
        when(enviarRegaloUseCase.ejecutar(regaloId)).thenReturn(enviado);

        mockMvc.perform(put("/api/regalos/{id}/enviar", regaloId)
                        .with(vendedor(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
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
                .andExpect(status().isBadRequest());              // 400

        Mockito.verify(confirmarEntregaRegaloUseCase, Mockito.never()).ejecutar(regaloId, destinatario);
    }

    // ===================== sin token =====================

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(put("/api/regalos/{id}/asignar", regaloId))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}