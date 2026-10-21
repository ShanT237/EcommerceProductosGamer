package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;

    // /api/** exige JWT; aquí el interceptor se simula como "token válido".
    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @BeforeEach
    void permitirPeticiones() throws Exception {
        when(jwtAuthInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private final UUID vendedorId = UUID.randomUUID();

    @Test
    void deberiaDevolverElReporteDeVentasDelVendedor() throws Exception {
        when(reporteVentasVendedorUseCase.ejecutar(vendedorId))
                .thenReturn(new ReporteVentasResponse(250.0, 3, 12.5));

        mockMvc.perform(get("/api/reportes/ventas").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.ingresos").value(250.0))
                .andExpect(jsonPath("$.unidades").value(3))
                .andExpect(jsonPath("$.comision").value(12.5));
    }

    @Test
    void deberiaRetornar404SiElVendedorNoExiste() throws Exception {
        when(reporteVentasVendedorUseCase.ejecutar(vendedorId))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/ventas").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void deberiaRetornar400SiFaltaVendedorId() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas"))
                .andExpect(status().isBadRequest());              // 400
    }
}