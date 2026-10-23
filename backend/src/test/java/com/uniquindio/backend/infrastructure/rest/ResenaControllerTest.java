package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.SubirResenaUseCase;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ResenaMapper;
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

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResenaController.class)
@Import({SecurityConfig.class, ResenaMapper.class})
class ResenaControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private SubirResenaUseCase subirResenaUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID compraId = UUID.randomUUID();

    private String cuerpo(int estrellas, String comentario) {
        return """
            { "idCompra": "%s", "valorCalificacion": %d, "comentario": "%s" }
            """.formatted(compraId, estrellas, comentario);
    }

    @Test
    void crearUsaElUsuarioDelToken() throws Exception {
        Resena resena = Resena.crear("resena-1", compraId.toString(), UUID.randomUUID().toString(),
                usuarioId.toString(), true, new Calificacion(5), "Excelente");
        when(subirResenaUseCase.ejecutar(compraId, usuarioId, 5, "Excelente")).thenReturn(resena);

        mockMvc.perform(post("/api/resenas")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(5, "Excelente")))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idUsuario").value(usuarioId.toString()))
                .andExpect(jsonPath("$.calificacion").value(5));

        Mockito.verify(subirResenaUseCase).ejecutar(compraId, usuarioId, 5, "Excelente");
    }

    @Test
    void ignoraUnIdUsuarioEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        Resena resena = Resena.crear("resena-1", compraId.toString(), UUID.randomUUID().toString(),
                usuarioId.toString(), true, new Calificacion(4), "Bien");
        when(subirResenaUseCase.ejecutar(compraId, usuarioId, 4, "Bien")).thenReturn(resena);

        String conIdAjeno = """
            { "idCompra": "%s", "idUsuario": "%s", "valorCalificacion": 4, "comentario": "Bien" }
            """.formatted(compraId, ajeno);

        mockMvc.perform(post("/api/resenas")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conIdAjeno))
                .andExpect(status().isCreated());

        Mockito.verify(subirResenaUseCase).ejecutar(compraId, usuarioId, 4, "Bien");
        Mockito.verify(subirResenaUseCase, Mockito.never()).ejecutar(compraId, ajeno, 4, "Bien");
    }

    @Test
    void calificacionFueraDeRangoRetorna400() throws Exception {
        mockMvc.perform(post("/api/resenas")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(6, "Demasiado")))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(5, "Excelente")))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}