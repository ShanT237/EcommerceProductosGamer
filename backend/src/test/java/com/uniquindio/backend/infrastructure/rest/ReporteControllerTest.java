package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.MetricasResenasResponse;
import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.AlertasStockBajoUseCase;
import com.uniquindio.backend.application.usecase.MetricasResenasUseCase;
import com.uniquindio.backend.application.usecase.ReporteMasVendidosUseCase;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
@Import(ProductoMapper.class)
class ReporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;

    @MockitoBean
    private ReporteMasVendidosUseCase reporteMasVendidosUseCase;

    @MockitoBean
    private AlertasStockBajoUseCase alertasStockBajoUseCase;

    @MockitoBean
    private MetricasResenasUseCase metricasResenasUseCase;

    // /api/** exige JWT; aquí el interceptor se simula como "token válido".
    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @BeforeEach
    void permitirPeticiones() throws Exception {
        when(jwtAuthInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private final UUID vendedorId = UUID.randomUUID();

    private Producto producto(String nombre, int stock) {
        return Producto.publicar(UUID.randomUUID(), vendedorId, nombre, "Desc", 100, stock,
                Gama.MEDIA, Exclusividad.NORMAL);
    }

    // ===================== CU-29 =====================

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

    // ===================== CU-30 =====================

    @Test
    void masVendidosSinVendedorUsaTopPorDefectoDiez() throws Exception {
        UUID productoId = UUID.randomUUID();
        when(reporteMasVendidosUseCase.ejecutar(isNull(), eq(10)))
                .thenReturn(List.of(new ProductoVendidoResponse(productoId, "Mouse", 5, 4.5)));

        mockMvc.perform(get("/api/reportes/mas-vendidos"))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$[0].nombre").value("Mouse"))
                .andExpect(jsonPath("$[0].unidades").value(5))
                .andExpect(jsonPath("$[0].calificacionPromedio").value(4.5));
    }

    @Test
    void masVendidosFiltraPorVendedorYTop() throws Exception {
        when(reporteMasVendidosUseCase.ejecutar(vendedorId, 3)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/mas-vendidos")
                        .param("vendedorId", vendedorId.toString())
                        .param("top", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void masVendidosConTopNoPositivoRetorna400() throws Exception {
        when(reporteMasVendidosUseCase.ejecutar(isNull(), eq(0)))
                .thenThrow(new ReglaDominioException("El tamaño del escalafón debe ser positivo"));

        mockMvc.perform(get("/api/reportes/mas-vendidos").param("top", "0"))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== CU-34 =====================

    @Test
    void stockBajoListaProductosYMarcaLosAgotados() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 5))
                .thenReturn(List.of(producto("Agotado", 0), producto("Bajo", 3)));

        mockMvc.perform(get("/api/reportes/stock-bajo").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isOk())                       // 200, umbral por defecto 5
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Agotado"))
                .andExpect(jsonPath("$[0].agotado").value(true))
                .andExpect(jsonPath("$[1].stock").value(3))
                .andExpect(jsonPath("$[1].agotado").value(false));
    }

    @Test
    void stockBajoUsaElUmbralRecibido() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/stock-bajo")
                        .param("vendedorId", vendedorId.toString())
                        .param("umbral", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void stockBajoConUmbralNoPositivoRetorna400() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 0))
                .thenThrow(new ReglaDominioException("El umbral de stock debe ser positivo"));

        mockMvc.perform(get("/api/reportes/stock-bajo")
                        .param("vendedorId", vendedorId.toString())
                        .param("umbral", "0"))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void stockBajoConVendedorInexistenteRetorna404() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 5))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/stock-bajo").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void stockBajoSinVendedorIdRetorna400() throws Exception {
        mockMvc.perform(get("/api/reportes/stock-bajo"))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== CU-35 =====================

    @Test
    void metricasResenasDevuelvePromedioDistribucionYConteos() throws Exception {
        when(metricasResenasUseCase.ejecutar(vendedorId)).thenReturn(new MetricasResenasResponse(
                3.0, 5, Map.of(1, 1L, 2, 1L, 3, 1L, 4, 1L, 5, 1L), 2, 2));

        mockMvc.perform(get("/api/reportes/metricas-resenas").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.promedio").value(3.0))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.distribucion['5']").value(1))
                .andExpect(jsonPath("$.positivas").value(2))
                .andExpect(jsonPath("$.negativas").value(2));
    }

    @Test
    void metricasResenasConVendedorInexistenteRetorna404() throws Exception {
        when(metricasResenasUseCase.ejecutar(vendedorId))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/metricas-resenas").param("vendedorId", vendedorId.toString()))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void metricasResenasSinVendedorIdRetorna400() throws Exception {
        mockMvc.perform(get("/api/reportes/metricas-resenas"))
                .andExpect(status().isBadRequest());              // 400
    }
}