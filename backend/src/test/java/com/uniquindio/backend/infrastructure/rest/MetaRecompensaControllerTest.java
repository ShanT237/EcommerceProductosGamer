package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.GestionarMetasRecompensaUseCase;
import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.rest.mapper.MetaRecompensaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaRecompensaController.class)
@Import(MetaRecompensaMapper.class)
class MetaRecompensaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestionarMetasRecompensaUseCase gestionarMetasUseCase;

    // /api/** exige JWT; aquí el interceptor se simula como "token válido".
    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @BeforeEach
    void permitirPeticiones() throws Exception {
        when(jwtAuthInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private final UUID productoId = UUID.randomUUID();

    @Test
    void deberiaCrearMetaCuandoDatosValidos() throws Exception {
        MetaRecompensa meta = MetaRecompensa.crear(UUID.randomUUID(), 5, productoId);
        when(gestionarMetasUseCase.crear(any(), eq(5), eq(productoId))).thenReturn(meta);

        String json = """
            { "comprasRequeridas": 5, "productoRegaloId": "%s" }
            """.formatted(productoId);

        mockMvc.perform(post("/api/metas-recompensa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.comprasRequeridas").value(5))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void deberiaRetornar400SiElUmbralNoEsPositivo() throws Exception {
        String json = """
            { "comprasRequeridas": 0, "productoRegaloId": "%s" }
            """.formatted(productoId);

        mockMvc.perform(post("/api/metas-recompensa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void deberiaRetornar404SiElProductoRegaloNoExiste() throws Exception {
        when(gestionarMetasUseCase.crear(any(), eq(5), eq(productoId)))
                .thenThrow(new NoSuchElementException("El producto regalo no existe"));

        String json = """
            { "comprasRequeridas": 5, "productoRegaloId": "%s" }
            """.formatted(productoId);

        mockMvc.perform(post("/api/metas-recompensa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void deberiaActualizarElUmbral() throws Exception {
        UUID id = UUID.randomUUID();
        MetaRecompensa meta = MetaRecompensa.crear(id, 5, productoId);
        meta.actualizarUmbral(8);
        when(gestionarMetasUseCase.actualizarUmbral(id, 8)).thenReturn(meta);

        mockMvc.perform(put("/api/metas-recompensa/{id}/umbral", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nuevoUmbral\": 8 }"))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.comprasRequeridas").value(8));
    }

    @Test
    void deberiaDesactivarLaMeta() throws Exception {
        UUID id = UUID.randomUUID();
        MetaRecompensa meta = MetaRecompensa.crear(id, 5, productoId);
        meta.desactivar();
        when(gestionarMetasUseCase.desactivar(id)).thenReturn(meta);

        mockMvc.perform(put("/api/metas-recompensa/{id}/desactivar", id))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    void desactivarUnaMetaYaDesactivadaRetorna400() throws Exception {
        UUID id = UUID.randomUUID();
        when(gestionarMetasUseCase.desactivar(id))
                .thenThrow(new ReglaDominioException("La meta ya está desactivada"));

        mockMvc.perform(put("/api/metas-recompensa/{id}/desactivar", id))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void metaInexistenteRetorna404() throws Exception {
        UUID id = UUID.randomUUID();
        when(gestionarMetasUseCase.actualizarUmbral(id, 3))
                .thenThrow(new NoSuchElementException("La meta no existe"));

        mockMvc.perform(put("/api/metas-recompensa/{id}/umbral", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nuevoUmbral\": 3 }"))
                .andExpect(status().isNotFound());                // 404
    }
}