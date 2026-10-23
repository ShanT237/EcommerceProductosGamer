package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.MetricasResenasResponse;
import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.dto.response.ReporteGlobalResponse;
import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.AlertasStockBajoUseCase;
import com.uniquindio.backend.application.usecase.MetricasResenasUseCase;
import com.uniquindio.backend.application.usecase.ReporteGlobalUseCase;
import com.uniquindio.backend.application.usecase.ReporteMasVendidosUseCase;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
@Import({SecurityConfig.class, ProductoMapper.class})
class ReporteControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

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

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();

    private Producto producto(String nombre, int stock) {
        return Producto.publicar(UUID.randomUUID(), vendedorId, nombre, "Desc", 100, stock,
                Gama.MEDIA, Exclusividad.NORMAL);
    }

    // ===================== CU-29 =====================

    @Test
    void ventasUsaElVendedorDelToken() throws Exception {
        when(reporteVentasVendedorUseCase.ejecutar(vendedorId))
                .thenReturn(new ReporteVentasResponse(250.0, 3, 12.5));

        mockMvc.perform(get("/api/reportes/ventas").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.ingresos").value(250.0))
                .andExpect(jsonPath("$.unidades").value(3))
                .andExpect(jsonPath("$.comision").value(12.5));

        Mockito.verify(reporteVentasVendedorUseCase).ejecutar(vendedorId);
    }

    @Test
    void ventasIgnoraUnVendedorIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(reporteVentasVendedorUseCase.ejecutar(vendedorId))
                .thenReturn(new ReporteVentasResponse(0, 0, 0));

        mockMvc.perform(get("/api/reportes/ventas")
                        .param("vendedorId", ajeno.toString())
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk());

        Mockito.verify(reporteVentasVendedorUseCase, Mockito.never()).ejecutar(ajeno);
    }

    @Test
    void ventasConVendedorInexistenteRetorna404() throws Exception {
        when(reporteVentasVendedorUseCase.ejecutar(vendedorId))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/ventas").with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404
    }

    // ===================== CU-30 =====================

    @Test
    void masVendidosSinVendedorUsaTopPorDefectoDiez() throws Exception {
        UUID productoId = UUID.randomUUID();
        when(reporteMasVendidosUseCase.ejecutar(isNull(), eq(10)))
                .thenReturn(List.of(new ProductoVendidoResponse(productoId, "Mouse", 5, 4.5)));

        mockMvc.perform(get("/api/reportes/mas-vendidos").with(vendedor(vendedorId)))
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
                        .param("top", "3")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void masVendidosConTopNoPositivoRetorna422() throws Exception {
        when(reporteMasVendidosUseCase.ejecutar(isNull(), eq(0)))
                .thenThrow(new ReglaDominioException("El tamaño del escalafón debe ser positivo"));

        mockMvc.perform(get("/api/reportes/mas-vendidos")
                        .param("top", "0")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isUnprocessableContent());              // 422
    }

    // ===================== CU-34 =====================

    @Test
    void stockBajoUsaElVendedorDelTokenYMarcaLosAgotados() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 5))
                .thenReturn(List.of(producto("Agotado", 0), producto("Bajo", 3)));

        mockMvc.perform(get("/api/reportes/stock-bajo").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200, umbral por defecto 5
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Agotado"))
                .andExpect(jsonPath("$[0].agotado").value(true))
                .andExpect(jsonPath("$[1].stock").value(3))
                .andExpect(jsonPath("$[1].agotado").value(false));

        Mockito.verify(alertasStockBajoUseCase).ejecutar(vendedorId, 5);
    }

    @Test
    void stockBajoUsaElUmbralRecibido() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/stock-bajo")
                        .param("umbral", "10")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void stockBajoIgnoraUnVendedorIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/reportes/stock-bajo")
                        .param("vendedorId", ajeno.toString())
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk());

        Mockito.verify(alertasStockBajoUseCase, Mockito.never()).ejecutar(ajeno, 5);
    }

    @Test
    void stockBajoConUmbralNoPositivoRetorna422() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 0))
                .thenThrow(new ReglaDominioException("El umbral de stock debe ser positivo"));

        mockMvc.perform(get("/api/reportes/stock-bajo")
                        .param("umbral", "0")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isUnprocessableContent());              // 422
    }

    @Test
    void stockBajoConVendedorInexistenteRetorna404() throws Exception {
        when(alertasStockBajoUseCase.ejecutar(vendedorId, 5))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/stock-bajo").with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404
    }

    // ===================== CU-35 =====================

    @Test
    void metricasResenasUsaElVendedorDelToken() throws Exception {
        when(metricasResenasUseCase.ejecutar(vendedorId)).thenReturn(new MetricasResenasResponse(
                3.0, 5, Map.of(1, 1L, 2, 1L, 3, 1L, 4, 1L, 5, 1L), 2, 2));

        mockMvc.perform(get("/api/reportes/metricas-resenas").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.promedio").value(3.0))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.distribucion['5']").value(1))
                .andExpect(jsonPath("$.positivas").value(2))
                .andExpect(jsonPath("$.negativas").value(2));

        Mockito.verify(metricasResenasUseCase).ejecutar(vendedorId);
    }

    @Test
    void metricasResenasIgnoraUnVendedorIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(metricasResenasUseCase.ejecutar(vendedorId))
                .thenReturn(new MetricasResenasResponse(0, 0, Map.of(), 0, 0));

        mockMvc.perform(get("/api/reportes/metricas-resenas")
                        .param("vendedorId", ajeno.toString())
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk());

        Mockito.verify(metricasResenasUseCase, Mockito.never()).ejecutar(ajeno);
    }

    @Test
    void metricasResenasConVendedorInexistenteRetorna404() throws Exception {
        when(metricasResenasUseCase.ejecutar(vendedorId))
                .thenThrow(new NoSuchElementException("El vendedor no existe"));

        mockMvc.perform(get("/api/reportes/metricas-resenas").with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404
    }

    // ===================== CU-36 =====================

    @Test
    void reporteGlobalDevuelveElConsolidadoDelMarketplace() throws Exception {
        when(reporteGlobalUseCase.ejecutar())
                .thenReturn(new ReporteGlobalResponse(12, 4500.5, 30, 4));

        mockMvc.perform(get("/api/reportes/global").with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.comprasCompletadas").value(12))
                .andExpect(jsonPath("$.ingresos").value(4500.5))
                .andExpect(jsonPath("$.productosActivos").value(30))
                .andExpect(jsonPath("$.vendedoresActivos").value(4));
    }

    @Test
    void reporteGlobalConMarketplaceVacioDevuelveCeros() throws Exception {
        when(reporteGlobalUseCase.ejecutar()).thenReturn(new ReporteGlobalResponse(0, 0.0, 0, 0));

        mockMvc.perform(get("/api/reportes/global").with(vendedor(vendedorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprasCompletadas").value(0))
                .andExpect(jsonPath("$.ingresos").value(0.0));
    }

    // ===================== sin token =====================

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas"))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
    @Test
    void unCompradorNoPuedeVerReportesDeVendedorRetorna403() throws Exception {
        UUID compradorId = UUID.randomUUID();

        mockMvc.perform(get("/api/reportes/ventas").with(comprador(compradorId)))
                .andExpect(status().isForbidden())                // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/reportes/stock-bajo").with(comprador(compradorId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reportes/metricas-resenas").with(comprador(compradorId)))
                .andExpect(status().isForbidden());

        Mockito.verifyNoInteractions(reporteVentasVendedorUseCase, alertasStockBajoUseCase, metricasResenasUseCase);
    }

    @Test
    void elReporteGlobalSigueAbiertoACualquierSesion() throws Exception {
        when(reporteGlobalUseCase.ejecutar()).thenReturn(new ReporteGlobalResponse(0, 0.0, 0, 0));

        mockMvc.perform(get("/api/reportes/global").with(comprador(UUID.randomUUID())))
                .andExpect(status().isOk());
    }
}