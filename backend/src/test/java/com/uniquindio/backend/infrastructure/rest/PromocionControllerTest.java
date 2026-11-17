package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConfigurarDescuentoPromocionalUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPromocionesDelVendedorUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPromocionesDelVendedorUseCase.PromocionDeProducto;
import com.uniquindio.backend.application.usecase.ModificarPromocionUseCase;
import com.uniquindio.backend.application.usecase.QuitarPromocionUseCase;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.PromocionMapper;
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
import java.util.List;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromocionController.class)
@Import({SecurityConfig.class, PromocionMapper.class})
class PromocionControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private ConfigurarDescuentoPromocionalUseCase configurarUseCase;

    @MockitoBean
    private ModificarPromocionUseCase modificarUseCase;

    @MockitoBean
    private QuitarPromocionUseCase quitarUseCase;

    @MockitoBean
    private ConsultarPromocionesDelVendedorUseCase consultarUseCase;

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
    private final UUID promocionId = UUID.randomUUID();

    private String cuerpo(String extra, double porcentaje) {
        return """
            { %s "productoId": "%s", "porcentaje": %s, "desde": "2026-12-01", "hasta": "2026-12-31" }
            """.formatted(extra, productoId, porcentaje);
    }

    private String cuerpoModificar(String extra, double porcentaje) {
        return """
            { %s "porcentaje": %s, "desde": "2026-12-01", "hasta": "2026-12-31" }
            """.formatted(extra, porcentaje);
    }

    // ---------- POST (configurar) ----------

    @Test
    void configurarUsaElVendedorDelToken() throws Exception {
        when(configurarUseCase.ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA))
                .thenReturn(Promocion.programar(UUID.randomUUID(), productoId, 20, DESDE, HASTA));

        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.productoId").value(productoId.toString()))
                .andExpect(jsonPath("$.porcentaje").value(20.0));

        Mockito.verify(configurarUseCase).ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA);
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(configurarUseCase.ejecutar(productoId, vendedorId, 20.0, DESDE, HASTA))
                .thenReturn(Promocion.programar(UUID.randomUUID(), productoId, 20, DESDE, HASTA));

        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("\"vendedorId\": \"%s\",".formatted(ajeno), 20)))
                .andExpect(status().isCreated());

        Mockito.verify(configurarUseCase, Mockito.never())
                .ejecutar(any(), eq(ajeno), Mockito.anyDouble(), any(), any());
    }

    @Test
    void porcentajeNoPositivoRetorna400() throws Exception {
        mockMvc.perform(post("/api/promociones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 0)))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", 20)))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ---------- GET (listar las del vendedor) ----------

    @Test
    void listarUsaElVendedorDelTokenYDevuelveElNombreDelProducto() throws Exception {
        Promocion promocion = Promocion.reconstruir(promocionId, productoId, 20, DESDE, HASTA);
        when(consultarUseCase.ejecutar(vendedorId))
                .thenReturn(List.of(new PromocionDeProducto(promocion, "Mouse")));

        mockMvc.perform(get("/api/promociones").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(promocionId.toString()))
                .andExpect(jsonPath("$[0].productoId").value(productoId.toString()))
                .andExpect(jsonPath("$[0].productoNombre").value("Mouse"))
                .andExpect(jsonPath("$[0].porcentaje").value(20.0))
                .andExpect(jsonPath("$[0].desde").value("2026-12-01"))
                .andExpect(jsonPath("$[0].hasta").value("2026-12-31"));

        Mockito.verify(consultarUseCase).ejecutar(vendedorId);
    }

    @Test
    void listarSinPromocionesDevuelveListaVacia() throws Exception {
        when(consultarUseCase.ejecutar(vendedorId)).thenReturn(List.of());

        mockMvc.perform(get("/api/promociones").with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/promociones"))
                .andExpect(status().isUnauthorized());            // 401
    }

    @Test
    void unCompradorNoPuedeListarRetorna403() throws Exception {
        mockMvc.perform(get("/api/promociones").with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden());               // 403
    }

    // ---------- PUT (modificar) ----------

    @Test
    void modificarUsaElVendedorDelTokenYLaPromocionDeLaUrl() throws Exception {
        when(modificarUseCase.ejecutar(promocionId, vendedorId, 25.0, DESDE, HASTA))
                .thenReturn(Promocion.reconstruir(promocionId, productoId, 25, DESDE, HASTA));

        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("", 25)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(promocionId.toString()))
                .andExpect(jsonPath("$.productoId").value(productoId.toString()))
                .andExpect(jsonPath("$.porcentaje").value(25.0));

        Mockito.verify(modificarUseCase).ejecutar(promocionId, vendedorId, 25.0, DESDE, HASTA);
    }

    @Test
    void modificarIgnoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(modificarUseCase.ejecutar(promocionId, vendedorId, 25.0, DESDE, HASTA))
                .thenReturn(Promocion.reconstruir(promocionId, productoId, 25, DESDE, HASTA));

        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("\"vendedorId\": \"%s\",".formatted(ajeno), 25)))
                .andExpect(status().isOk());

        Mockito.verify(modificarUseCase, Mockito.never())
                .ejecutar(any(), eq(ajeno), Mockito.anyDouble(), any(), any());
    }

    @Test
    void modificarConPorcentajeNoPositivoRetorna400() throws Exception {
        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("", 0)))
                .andExpect(status().isBadRequest());              // 400

        Mockito.verifyNoInteractions(modificarUseCase);
    }

    @Test
    void modificarSinFechasRetorna400() throws Exception {
        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"porcentaje\": 25 }"))
                .andExpect(status().isBadRequest());              // 400

        Mockito.verifyNoInteractions(modificarUseCase);
    }

    @Test
    void modificarSinTokenRetorna401() throws Exception {
        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("", 25)))
                .andExpect(status().isUnauthorized());            // 401
    }

    @Test
    void unCompradorNoPuedeModificarRetorna403() throws Exception {
        mockMvc.perform(put("/api/promociones/{id}", promocionId)
                        .with(comprador(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoModificar("", 25)))
                .andExpect(status().isForbidden());               // 403
    }

    // ---------- DELETE (quitar) ----------

    @Test
    void quitarUsaElVendedorDelTokenYRetorna204() throws Exception {
        mockMvc.perform(delete("/api/promociones/{id}", promocionId).with(vendedor(vendedorId)))
                .andExpect(status().isNoContent());               // 204

        Mockito.verify(quitarUseCase).ejecutar(promocionId, vendedorId);
    }

    @Test
    void quitarSinTokenRetorna401() throws Exception {
        mockMvc.perform(delete("/api/promociones/{id}", promocionId))
                .andExpect(status().isUnauthorized());            // 401

        Mockito.verifyNoInteractions(quitarUseCase);
    }

    @Test
    void unCompradorNoPuedeQuitarRetorna403() throws Exception {
        mockMvc.perform(delete("/api/promociones/{id}", promocionId).with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(quitarUseCase);
    }
}