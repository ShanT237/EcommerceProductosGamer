package com.uniquindio.backend.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración: recorre el sistema REAL, sin simular nada.
 * Filtro de seguridad JWT -> controller -> caso de uso -> dominio -> repositorio JPA -> H2.
 * Usa los usuarios y datos que carga data.sql al arrancar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoCompletoIntegracionTest {

    private static final String VENDEDOR_CORREO = "demo@gamerzone.com";
    private static final String VENDEDOR_CLAVE = "GamerZone2026";
    private static final String COMPRADOR_CORREO = "jose.demo@correo.com";
    private static final String COMPRADOR_CLAVE = "JoseGamer2026";
    private static final String ANA_CORREO = "ana.demo@correo.com";
    private static final String ANA_CLAVE = "AnaGamer2026";

    // Compra de demo de Ana (COMPLETADA, del 19/10/2026): ya superó el plazo de 48 h
    private static final String COMPRA_DE_ANA = "dddddddd-dddd-dddd-dddd-ddddddddddd2";

    @Autowired
    private MockMvc mockMvc;

    private String iniciarSesion(String correo, String contrasena) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "correo": "%s", "contrasena": "%s" }
                                """.formatted(correo, contrasena)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    @Test
    @DisplayName("Flujo real: el vendedor publica, el comprador compra y confirma, y el historial lo refleja")
    void vendedorPublicaYCompradorCompraYConfirma() throws Exception {
        // Arrange: tokens reales de los usuarios sembrados en data.sql
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);

        // Act 1: el vendedor publica un producto
        String productoJson = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Teclado de integracion",
                                  "descripcion": "Producto creado por la prueba de integracion",
                                  "precio": 150000, "stock": 10,
                                  "gama": "MEDIA", "exclusividad": "NORMAL" }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productoId = JsonPath.read(productoJson, "$.id");

        // Act 2: el comprador compra 2 unidades
        String compraJson = mockMvc.perform(post("/api/compras")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productoId": "%s", "cantidad": 2 }
                                """.formatted(productoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.precioUnitario").value(150000.0))
                .andReturn().getResponse().getContentAsString();
        String compraId = JsonPath.read(compraJson, "$.id");

        // Act 3: el comprador confirma la compra
        mockMvc.perform(put("/api/compras/{id}/confirmar", compraId)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        // Assert: una consulta nueva, leída de la base, muestra la compra COMPLETADA
        mockMvc.perform(get("/api/compras")
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(compraId))
                        .value("COMPLETADA"));
    }

    @Test
    @DisplayName("Regla de negocio real: reembolsar fuera de plazo responde 422 y la compra no cambia")
    void reembolsoFueraDePlazoRetorna422YNoCambiaLaCompra() throws Exception {
        // Arrange
        String tokenAna = iniciarSesion(ANA_CORREO, ANA_CLAVE);

        // Act + Assert: el dominio rechaza el reembolso y el handler lo traduce a 422
        mockMvc.perform(put("/api/compras/{id}/reembolso", COMPRA_DE_ANA)
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        // Assert: en la base la compra sigue COMPLETADA (no se guardó ningún cambio)
        mockMvc.perform(get("/api/compras")
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(COMPRA_DE_ANA))
                        .value("COMPLETADA"));
    }

    @Test
    @DisplayName("Seguridad real: un comprador no puede publicar productos (403)")
    void compradorNoPuedePublicarRetorna403() throws Exception {
        // Arrange
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);

        // Act + Assert
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "No deberia crearse", "descripcion": "Solo vende el vendedor",
                                  "precio": 1000, "stock": 1,
                                  "gama": "BAJA", "exclusividad": "NORMAL" }
                                """))
                .andExpect(status().isForbidden());
    }
}