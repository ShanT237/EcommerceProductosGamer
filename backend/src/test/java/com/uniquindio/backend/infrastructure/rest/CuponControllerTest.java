package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ConsultarCuponUseCase;
import com.uniquindio.backend.application.usecase.ConsultarCuponesVendedorUseCase;
import com.uniquindio.backend.application.usecase.CrearCuponUseCase;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.CuponMapper;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuponController.class)
@Import({SecurityConfig.class, CuponMapper.class})
class CuponControllerTest {

    private static final LocalDate VENCE = LocalDate.of(2026, 12, 31);

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    @MockitoBean
    private CrearCuponUseCase crearCuponUseCase;

    @MockitoBean
    private ConsultarCuponesVendedorUseCase consultarCuponesVendedorUseCase;

    @MockitoBean
    private ConsultarCuponUseCase consultarCuponUseCase;

    // SecurityConfig necesita el EmisorToken para armar el filtro JWT; las sesiones se simulan con SesionTest.
    @MockitoBean
    private EmisorToken emisorToken;

    @MockitoBean
    private VerificadorCuentaActiva verificadorCuentaActiva;

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID compradorId = UUID.randomUUID();

    private String cuerpo(String codigo, double porcentaje, int usosMaximos) {
        return """
                { "codigo": "%s", "porcentaje": %s, "vence": "2026-12-31", "usosMaximos": %s }
                """.formatted(codigo, porcentaje, usosMaximos);
    }

    private Cupon cuponDelVendedor(String codigo) {
        return Cupon.crear(UUID.randomUUID(), vendedorId, codigo, 10, VENCE, 5);
    }

    // ---------- POST ----------

    @Test
    void crearUsaElVendedorDelToken() throws Exception {
        when(crearCuponUseCase.ejecutar(any(), eq(vendedorId), eq("GAMER10"), eq(10.0), eq(VENCE), eq(5)))
                .thenReturn(cuponDelVendedor("GAMER10"));

        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 10, 5)))
                .andExpect(status().isCreated())                         // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value("GAMER10"))
                .andExpect(jsonPath("$.porcentaje").value(10.0))
                .andExpect(jsonPath("$.usosMaximos").value(5))
                .andExpect(jsonPath("$.usos").value(0))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()));

        Mockito.verify(crearCuponUseCase).ejecutar(any(), eq(vendedorId), eq("GAMER10"), eq(10.0), eq(VENCE), eq(5));
    }

    @Test
    void ignoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(crearCuponUseCase.ejecutar(any(), eq(vendedorId), any(), anyDouble(), any(), anyInt()))
                .thenReturn(cuponDelVendedor("GAMER10"));

        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "vendedorId": "%s", "codigo": "GAMER10", "porcentaje": 10,
                                  "vence": "2026-12-31", "usosMaximos": 5 }
                                """.formatted(ajeno)))
                .andExpect(status().isCreated());

        Mockito.verify(crearCuponUseCase, Mockito.never())
                .ejecutar(any(), eq(ajeno), any(), anyDouble(), any(), anyInt());
    }

    @Test
    void crearConPorcentajeCienRetorna400() throws Exception {
        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 100, 5)))
                .andExpect(status().isBadRequest());                     // 400

        Mockito.verifyNoInteractions(crearCuponUseCase);
    }

    @Test
    void crearConDatosInvalidosRetorna400() throws Exception {
        // código en blanco, porcentaje 0 y usos 0
        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(" ", 0, 0)))
                .andExpect(status().isBadRequest());

        // sin fecha de vencimiento
        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"codigo\": \"GAMER10\", \"porcentaje\": 10, \"usosMaximos\": 5 }"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(crearCuponUseCase);
    }

    @Test
    void codigoDuplicadoRetorna409() throws Exception {
        when(crearCuponUseCase.ejecutar(any(), any(), any(), anyDouble(), any(), anyInt()))
                .thenThrow(new ConflictoException("Ya existe un cupón con el código GAMER10"));

        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 10, 5)))
                .andExpect(status().isConflict())                        // 409
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cupón con el código GAMER10"));
    }

    @Test
    void vencimientoPasadoRetorna422() throws Exception {
        when(crearCuponUseCase.ejecutar(any(), any(), any(), anyDouble(), any(), anyInt()))
                .thenThrow(new ReglaDominioException("La fecha de vencimiento del cupón no puede estar en el pasado"));

        mockMvc.perform(post("/api/cupones")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 10, 5)))
                .andExpect(status().isUnprocessableContent());           // 422
    }

    // ---------- GET (propios) ----------

    @Test
    void listarUsaElVendedorDelToken() throws Exception {
        when(consultarCuponesVendedorUseCase.ejecutar(vendedorId))
                .thenReturn(List.of(cuponDelVendedor("ALFA10"), cuponDelVendedor("ZETA10")));

        mockMvc.perform(get("/api/cupones").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                              // 200
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("ALFA10"))
                .andExpect(jsonPath("$[1].codigo").value("ZETA10"));

        Mockito.verify(consultarCuponesVendedorUseCase).ejecutar(vendedorId);
    }

    @Test
    void listarSinCuponesDevuelveListaVacia() throws Exception {
        when(consultarCuponesVendedorUseCase.ejecutar(vendedorId)).thenReturn(List.of());

        mockMvc.perform(get("/api/cupones").with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- GET /{codigo} (vista previa) ----------

    @Test
    void vistaPreviaDevuelve200ParaComprador() throws Exception {
        Cupon global = Cupon.crear(UUID.randomUUID(), "VERANO26", 15, VENCE, 100);
        when(consultarCuponUseCase.ejecutar("VERANO26", compradorId)).thenReturn(global);

        mockMvc.perform(get("/api/cupones/{codigo}", "VERANO26").with(comprador(compradorId)))
                .andExpect(status().isOk())                              // 200
                .andExpect(jsonPath("$.codigo").value("VERANO26"))
                .andExpect(jsonPath("$.porcentaje").value(15.0))
                .andExpect(jsonPath("$.vendedorId").doesNotExist());     // global: sin dueño (null)

        Mockito.verify(consultarCuponUseCase).ejecutar("VERANO26", compradorId);
    }

    @Test
    void vistaPreviaDeUnCuponInexistenteRetorna404() throws Exception {
        when(consultarCuponUseCase.ejecutar("NOEXISTE", compradorId)).thenThrow(new NoSuchElementException("El cupón no existe"));

        mockMvc.perform(get("/api/cupones/{codigo}", "NOEXISTE").with(comprador(compradorId)))
                .andExpect(status().isNotFound());                       // 404
    }

    @Test
    void vistaPreviaDeUnCuponVencidoRetorna422() throws Exception {
        when(consultarCuponUseCase.ejecutar("VIEJO10", compradorId)).thenThrow(new ReglaDominioException("El cupón está vencido"));

        mockMvc.perform(get("/api/cupones/{codigo}", "VIEJO10").with(comprador(compradorId)))
                .andExpect(status().isUnprocessableContent())            // 422
                .andExpect(jsonPath("$.mensaje").value("El cupón está vencido"));
    }

    @Test
    void vistaPreviaDeUnCuponYaUsadoPorElCompradorRetorna422() throws Exception {
        when(consultarCuponUseCase.ejecutar("GAMER10", compradorId))
                .thenThrow(new ReglaDominioException("Ya usaste este cupón: solo se puede usar una vez por comprador"));

        mockMvc.perform(get("/api/cupones/{codigo}", "GAMER10").with(comprador(compradorId)))
                .andExpect(status().isUnprocessableContent())            // 422
                .andExpect(jsonPath("$.mensaje").value("Ya usaste este cupón: solo se puede usar una vez por comprador"));
    }

    // ---------- Seguridad ----------

    @Test
    void compradorNoPuedeCrearNiListarRetorna403() throws Exception {
        mockMvc.perform(post("/api/cupones")
                        .with(comprador(compradorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 10, 5)))
                .andExpect(status().isForbidden())                       // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/cupones").with(comprador(compradorId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(crearCuponUseCase, consultarCuponesVendedorUseCase);
    }

    @Test
    void vendedorNoPuedeVerLaVistaPreviaRetorna403() throws Exception {
        mockMvc.perform(get("/api/cupones/{codigo}", "VERANO26").with(vendedor(vendedorId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(consultarCuponUseCase);
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/cupones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("GAMER10", 10, 5)))
                .andExpect(status().isUnauthorized())                    // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/cupones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cupones/{codigo}", "VERANO26")).andExpect(status().isUnauthorized());

        Mockito.verifyNoInteractions(crearCuponUseCase, consultarCuponesVendedorUseCase, consultarCuponUseCase);
    }
}