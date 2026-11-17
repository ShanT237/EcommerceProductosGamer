package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CancelarPreventaUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPreventasUseCase;
import com.uniquindio.backend.application.usecase.ModificarPreventaUseCase;
import com.uniquindio.backend.application.usecase.ProgramarPreventaUseCase;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.PreventaMapper;
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

import java.time.LocalDate;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreventaController.class)
@Import({SecurityConfig.class, PreventaMapper.class})
class PreventaControllerTest {

    private static final LocalDate LANZAMIENTO = LocalDate.of(2026, 12, 1);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ProgramarPreventaUseCase programarPreventaUseCase;

    @MockitoBean
    private ConsultarPreventasUseCase consultarPreventasUseCase;

    @MockitoBean
    private ModificarPreventaUseCase modificarPreventaUseCase;

    @MockitoBean
    private CancelarPreventaUseCase cancelarPreventaUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @MockitoBean
    private VerificadorCuentaActiva verificadorCuentaActiva;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    @Test
    void listarDevuelve200ParaCompradorYVendedor() throws Exception {
        when(consultarPreventasUseCase.ejecutar()).thenReturn(java.util.List.of(
                Preventa.reconstruir(UUID.randomUUID(), productoId, LANZAMIENTO, "Edición coleccionista")));

        mockMvc.perform(get("/api/preventas").with(comprador(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(productoId.toString()))
                .andExpect(jsonPath("$[0].fechaLanzamiento").value("2026-12-01"));

        mockMvc.perform(get("/api/preventas").with(vendedor(vendedorId)))
                .andExpect(status().isOk());
    }

    @Test
    void listarSinTokenEsPublico() throws Exception {
        when(consultarPreventasUseCase.ejecutar()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/preventas"))
                .andExpect(status().isOk());                      // 200: el catálogo lo necesita
    }

    private String cuerpo(String extra, String descripcion) {
        return """
            { %s "productoId": "%s", "fechaLanzamiento": "2026-12-01", "descripcion": "%s" }
            """.formatted(extra, productoId, descripcion);
    }

    @Test
    void programarUsaElVendedorDelToken() throws Exception {
        when(programarPreventaUseCase.ejecutar(vendedorId, productoId, LANZAMIENTO, "Edición coleccionista"))
                .thenReturn(Preventa.programar(UUID.randomUUID(), productoId, LANZAMIENTO, "Edición coleccionista"));

        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "Edición coleccionista")))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.productoId").value(productoId.toString()))
                .andExpect(jsonPath("$.fechaLanzamiento").value("2026-12-01"));

        Mockito.verify(programarPreventaUseCase)
                .ejecutar(vendedorId, productoId, LANZAMIENTO, "Edición coleccionista");
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(programarPreventaUseCase.ejecutar(vendedorId, productoId, LANZAMIENTO, "Lanzamiento"))
                .thenReturn(Preventa.programar(UUID.randomUUID(), productoId, LANZAMIENTO, "Lanzamiento"));

        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("\"vendedorId\": \"%s\",".formatted(ajeno), "Lanzamiento")))
                .andExpect(status().isCreated());

        Mockito.verify(programarPreventaUseCase, Mockito.never())
                .ejecutar(eq(ajeno), any(), any(), any());
    }

    @Test
    void descripcionVaciaRetorna400() throws Exception {
        mockMvc.perform(post("/api/preventas")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "")))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/preventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "Lanzamiento")))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------------------------------------------------------------- modificar

    private String cuerpoModificar(String descripcion) {
        return """
            { "fechaLanzamiento": "2026-12-01", "descripcion": "%s" }
            """.formatted(descripcion);
    }

    @Test
    void modificarUsaElVendedorDelTokenYElIdDeLaUrl() throws Exception {
        UUID preventaId = UUID.randomUUID();
        when(modificarPreventaUseCase.ejecutar(preventaId, vendedorId, LANZAMIENTO, "Nueva descripción"))
                .thenReturn(Preventa.reconstruir(preventaId, productoId, LANZAMIENTO, "Nueva descripción"));

        mockMvc.perform(put("/api/preventas/{id}", preventaId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("Nueva descripción")))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(preventaId.toString()))
                .andExpect(jsonPath("$.fechaLanzamiento").value("2026-12-01"))
                .andExpect(jsonPath("$.descripcion").value("Nueva descripción"));

        Mockito.verify(modificarPreventaUseCase)
                .ejecutar(preventaId, vendedorId, LANZAMIENTO, "Nueva descripción");
    }

    @Test
    void modificarConDescripcionVaciaRetorna400() throws Exception {
        mockMvc.perform(put("/api/preventas/{id}", UUID.randomUUID())
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("")))
                .andExpect(status().isBadRequest());              // 400

        Mockito.verifyNoInteractions(modificarPreventaUseCase);
    }

    @Test
    void modificarUnaPreventaYaLanzadaRetorna422() throws Exception {
        UUID preventaId = UUID.randomUUID();
        when(modificarPreventaUseCase.ejecutar(preventaId, vendedorId, LANZAMIENTO, "Tarde"))
                .thenThrow(new ReglaDominioException("La preventa ya fue lanzada y no se puede modificar"));

        mockMvc.perform(put("/api/preventas/{id}", preventaId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("Tarde")))
                .andExpect(status().isUnprocessableContent())     // 422
                .andExpect(jsonPath("$.mensaje").value("La preventa ya fue lanzada y no se puede modificar"));
    }

    @Test
    void modificarSinTokenRetorna401YComoCompradorRetorna403() throws Exception {
        mockMvc.perform(put("/api/preventas/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("Sin token")))
                .andExpect(status().isUnauthorized());            // 401

        mockMvc.perform(put("/api/preventas/{id}", UUID.randomUUID())
                        .with(comprador(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("Comprador")))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(modificarPreventaUseCase);
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    void cancelarUsaElVendedorDelTokenYDevuelve204() throws Exception {
        UUID preventaId = UUID.randomUUID();

        mockMvc.perform(delete("/api/preventas/{id}", preventaId).with(vendedor(vendedorId)))
                .andExpect(status().isNoContent());               // 204

        Mockito.verify(cancelarPreventaUseCase).ejecutar(preventaId, vendedorId);
    }

    @Test
    void cancelarUnaPreventaYaLanzadaRetorna422() throws Exception {
        UUID preventaId = UUID.randomUUID();
        doThrow(new ReglaDominioException("La preventa ya fue lanzada y no se puede cancelar"))
                .when(cancelarPreventaUseCase).ejecutar(preventaId, vendedorId);

        mockMvc.perform(delete("/api/preventas/{id}", preventaId).with(vendedor(vendedorId)))
                .andExpect(status().isUnprocessableContent())     // 422
                .andExpect(jsonPath("$.mensaje").value("La preventa ya fue lanzada y no se puede cancelar"));
    }

    @Test
    void cancelarUnaPreventaInexistenteRetorna404() throws Exception {
        UUID preventaId = UUID.randomUUID();
        doThrow(new java.util.NoSuchElementException("La preventa no existe"))
                .when(cancelarPreventaUseCase).ejecutar(preventaId, vendedorId);

        mockMvc.perform(delete("/api/preventas/{id}", preventaId).with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void cancelarSinTokenRetorna401YComoCompradorRetorna403() throws Exception {
        mockMvc.perform(delete("/api/preventas/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());            // 401

        mockMvc.perform(delete("/api/preventas/{id}", UUID.randomUUID()).with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(cancelarPreventaUseCase);
    }
}