package com.uniquindio.backend.infrastructure.config;

import com.uniquindio.backend.application.dto.response.ReporteGlobalResponse;
import com.uniquindio.backend.application.usecase.AlertasStockBajoUseCase;
import com.uniquindio.backend.application.usecase.MetricasResenasUseCase;
import com.uniquindio.backend.application.usecase.ReporteGlobalUseCase;
import com.uniquindio.backend.application.usecase.ReporteMasVendidosUseCase;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.valueobject.Rol;
import com.uniquindio.backend.infrastructure.rest.ReporteController;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
@Import({SecurityConfig.class, ProductoMapper.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisorToken emisorToken;

    @MockitoBean
    private VerificadorCuentaActiva verificadorCuentaActiva;

    @MockitoBean
    private ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;
    @MockitoBean
    private ReporteMasVendidosUseCase reporteMasVendidosUseCase;
    @MockitoBean
    private AlertasStockBajoUseCase alertasStockBajoUseCase;
    @MockitoBean
    private MetricasResenasUseCase metricasResenasUseCase;
    @MockitoBean
    private ReporteGlobalUseCase reporteGlobalUseCase;

    @Test
    void sinTokenResponde401ConCuerpoJson() throws Exception {
        mockMvc.perform(get("/api/reportes/global"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void conTokenInvalidoResponde401() throws Exception {
        when(emisorToken.validar("token-malo")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reportes/global").header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void conTokenValidoPermiteElAcceso() throws Exception {
        SesionToken sesion = new SesionToken(UUID.randomUUID(), Rol.COMPRADOR);
        when(emisorToken.validar("token-bueno")).thenReturn(Optional.of(sesion));
        when(verificadorCuentaActiva.estaActiva(sesion)).thenReturn(true);
        when(reporteGlobalUseCase.ejecutar()).thenReturn(new ReporteGlobalResponse(0, 0.0, 0, 0));

        mockMvc.perform(get("/api/reportes/global").header("Authorization", "Bearer token-bueno"))
                .andExpect(status().isOk());
    }

    @Test
    void conTokenValidoDeUnaCuentaDadaDeBajaResponde401ConCodigo() throws Exception {
        // El token no ha expirado, pero la cuenta ya no está activa: debe cortarse de inmediato.
        SesionToken sesion = new SesionToken(UUID.randomUUID(), Rol.VENDEDOR);
        when(emisorToken.validar("token-de-baja")).thenReturn(Optional.of(sesion));
        when(verificadorCuentaActiva.estaActiva(sesion)).thenReturn(false);

        mockMvc.perform(get("/api/reportes/global").header("Authorization", "Bearer token-de-baja"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void elTokenInvalidoNoLlevaCodigoDeCuentaInactiva() throws Exception {
        when(emisorToken.validar("token-malo")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reportes/global").header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void elRegistroYElLoginNoPidenToken() throws Exception {
        // Estos controllers no están en el slice (responden 404), pero la seguridad no debe bloquearlos.
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
        mockMvc.perform(post("/api/vendedores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void elPreflightDeCorsDelFrontendNoPideToken() throws Exception {
        mockMvc.perform(options("/api/reportes/global")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void unOrigenNoPermitidoNoRecibeCors() throws Exception {
        mockMvc.perform(options("/api/reportes/global")
                        .header("Origin", "http://sitio-malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}