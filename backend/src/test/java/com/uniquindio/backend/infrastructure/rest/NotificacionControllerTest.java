package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConsultarNotificacionesUseCase;
import com.uniquindio.backend.application.usecase.MarcarNotificacionLeidaUseCase;
import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.NotificacionMapper;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionController.class)
@Import({SecurityConfig.class, NotificacionMapper.class})
class NotificacionControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ConsultarNotificacionesUseCase consultarNotificacionesUseCase;

    @MockitoBean
    private MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID notificacionId = UUID.randomUUID();

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private Notificacion notificacion(UUID id, boolean leida) {
        return Notificacion.reconstruir(id, usuarioId, UUID.randomUUID(), TipoNotificacion.DESCUENTO,
                "El producto 'Teclado RGB' ha bajado de precio", LocalDateTime.now(), leida);
    }

    @Test
    void listarUsaElUsuarioDelToken() throws Exception {
        when(consultarNotificacionesUseCase.ejecutar(usuarioId))
                .thenReturn(List.of(notificacion(UUID.randomUUID(), false), notificacion(UUID.randomUUID(), true)));

        mockMvc.perform(get("/api/notificaciones").with(comprador(usuarioId)))
                .andExpect(status().isOk())                         // 200
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("DESCUENTO"))
                .andExpect(jsonPath("$[0].leida").value(false));

        Mockito.verify(consultarNotificacionesUseCase).ejecutar(usuarioId);
    }

    @Test
    void marcarLeidaDevuelve200ConLeidaTrue() throws Exception {
        when(marcarNotificacionLeidaUseCase.ejecutar(notificacionId, usuarioId))
                .thenReturn(notificacion(notificacionId, true));

        mockMvc.perform(put("/api/notificaciones/{id}/leida", notificacionId).with(comprador(usuarioId)))
                .andExpect(status().isOk())                         // 200
                .andExpect(jsonPath("$.id").value(notificacionId.toString()))
                .andExpect(jsonPath("$.leida").value(true));

        Mockito.verify(marcarNotificacionLeidaUseCase).ejecutar(notificacionId, usuarioId);
    }

    @Test
    void ajenaRetorna422() throws Exception {
        when(marcarNotificacionLeidaUseCase.ejecutar(notificacionId, usuarioId))
                .thenThrow(new ReglaDominioException("La notificación no pertenece al usuario"));

        mockMvc.perform(put("/api/notificaciones/{id}/leida", notificacionId).with(comprador(usuarioId)))
                .andExpect(status().isUnprocessableContent());      // 422
    }

    @Test
    void inexistenteRetorna404() throws Exception {
        when(marcarNotificacionLeidaUseCase.ejecutar(notificacionId, usuarioId))
                .thenThrow(new NoSuchElementException("La notificación no existe"));

        mockMvc.perform(put("/api/notificaciones/{id}/leida", notificacionId).with(comprador(usuarioId)))
                .andExpect(status().isNotFound());                  // 404
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isUnauthorized())               // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void vendedorPuedeListarYMarcarSusNotificaciones() throws Exception {
        mockMvc.perform(get("/api/notificaciones").with(vendedor(usuarioId)))
                .andExpect(status().isOk());                        // 200

        // El caso de uso (mock) no encuentra la notificación: 404 prueba que el rol sí pasó la seguridad
        when(marcarNotificacionLeidaUseCase.ejecutar(notificacionId, usuarioId))
                .thenThrow(new NoSuchElementException("La notificación no existe"));
        mockMvc.perform(put("/api/notificaciones/{id}/leida", notificacionId).with(vendedor(usuarioId)))
                .andExpect(status().isNotFound());                  // 404
    }
}
