package com.uniquindio.backend.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @DisplayName("Regla de negocio real: comprar más que el stock responde 422 y no descuenta stock")
    void comprarMasQueElStockRetorna422YNoDescuentaStock() throws Exception {
        // Arrange: un producto con solo 2 unidades y un nombre único para encontrarlo después
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String nombreUnico = "Mouse-stock-" + UUID.randomUUID();

        String productoJson = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "%s",
                                  "descripcion": "Producto con stock limitado para la prueba",
                                  "precio": 80000, "stock": 2,
                                  "gama": "BAJA", "exclusividad": "NORMAL" }
                                """.formatted(nombreUnico)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productoId = JsonPath.read(productoJson, "$.id");

        // Act + Assert: pedir 3 unidades viola la regla del dominio -> 422 con su mensaje
        mockMvc.perform(post("/api/compras")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productoId": "%s", "cantidad": 3 }
                                """.formatted(productoId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No hay suficiente stock disponible"));

        // Assert: una consulta nueva, leída de la base, sigue mostrando las 2 unidades
        mockMvc.perform(get("/api/productos")
                        .param("texto", nombreUnico)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].stock").value(2));
    }

    @Test
    @DisplayName("Seguridad real: comprar sin token responde 401")
    void comprarSinTokenRetorna401() throws Exception {
        // Act + Assert: sin cabecera Authorization el filtro JWT corta la petición
        mockMvc.perform(post("/api/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productoId": "00000000-0000-0000-0000-000000000000", "cantidad": 1 }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Validación real: publicar con datos inválidos responde 400 con un mensaje que nombra el campo")
    void publicarConDatosInvalidosRetorna400ConMensaje() throws Exception {
        // Arrange
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);

        // Act + Assert: nombre vacío y precio negativo no pasan @Valid
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "", "descripcion": "Datos invalidos a proposito",
                                  "precio": -5, "stock": 1,
                                  "gama": "ALTA", "exclusividad": "NORMAL" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("nombre")));
    }

    // ------------------------------------------------------------------ Pedidos (carrito)

    /** Publica un producto con nombre único y devuelve su id. */
    private String publicarProducto(String tokenVendedor, String nombre, int precio, int stock) throws Exception {
        String json = mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "%s",
                                  "descripcion": "Producto creado por la prueba de pedidos",
                                  "precio": %d, "stock": %d,
                                  "gama": "MEDIA", "exclusividad": "NORMAL" }
                                """.formatted(nombre, precio, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    @DisplayName("Flujo real de pedido: 2 productos en un solo POST y se confirman en una sola operación")
    void pedidoDeDosProductosSeConfirmaEnUnaSolaOperacion() throws Exception {
        // Arrange
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String teclado = publicarProducto(tokenVendedor, "Teclado-pedido-" + UUID.randomUUID(), 150000, 10);
        String mouse = publicarProducto(tokenVendedor, "Mouse-pedido-" + UUID.randomUUID(), 80000, 10);

        // Act 1: un solo POST crea una compra PENDIENTE por línea, con el mismo pedidoId
        String pedidoJson = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [
                                    { "productoId": "%s", "cantidad": 2 },
                                    { "productoId": "%s", "cantidad": 1 }
                                ] }
                                """.formatted(teclado, mouse)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.compras.length()").value(2))
                .andExpect(jsonPath("$.compras[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.compras[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(380000.0))
                .andReturn().getResponse().getContentAsString();
        String pedidoId = JsonPath.read(pedidoJson, "$.pedidoId");
        String primeraCompra = JsonPath.read(pedidoJson, "$.compras[0].id");
        String segundaCompra = JsonPath.read(pedidoJson, "$.compras[1].id");

        // Act 2: una sola confirmación completa las dos líneas
        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compras[0].estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.compras[1].estado").value("COMPLETADA"));

        // Assert: el historial, leído de la base, muestra ambas COMPLETADAS con el mismo pedido
        mockMvc.perform(get("/api/compras")
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(primeraCompra)).value("COMPLETADA"))
                .andExpect(jsonPath("$[?(@.id == '%s')].estado".formatted(segundaCompra)).value("COMPLETADA"))
                .andExpect(jsonPath("$[?(@.id == '%s')].pedidoId".formatted(primeraCompra)).value(pedidoId))
                .andExpect(jsonPath("$[?(@.id == '%s')].pedidoId".formatted(segundaCompra)).value(pedidoId));
    }

    @Test
    @DisplayName("Regla real: un pedido con un ítem sin stock responde 422 y no descuenta el stock de ninguno")
    void pedidoConUnItemSinStockRetorna422YNoDescuentaStock() throws Exception {
        // Arrange: el segundo producto solo tiene 1 unidad y el pedido pide 5
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String nombreAbundante = "Teclado-abundante-" + UUID.randomUUID();
        String nombreEscaso = "Mouse-escaso-" + UUID.randomUUID();
        String abundante = publicarProducto(tokenVendedor, nombreAbundante, 150000, 10);
        String escaso = publicarProducto(tokenVendedor, nombreEscaso, 80000, 1);

        // Act + Assert: el dominio rechaza el pedido completo
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [
                                    { "productoId": "%s", "cantidad": 2 },
                                    { "productoId": "%s", "cantidad": 5 }
                                ] }
                                """.formatted(abundante, escaso)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No hay suficiente stock disponible"));

        // Assert: ningún stock cambió (se leen de la base con consultas nuevas)
        mockMvc.perform(get("/api/productos")
                        .param("texto", nombreAbundante)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].stock").value(10));
        mockMvc.perform(get("/api/productos")
                        .param("texto", nombreEscaso)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].stock").value(1));

        // Assert: ninguna compra quedó guardada para esos productos
        mockMvc.perform(get("/api/compras")
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productoId == '%s')]".formatted(abundante)).isEmpty())
                .andExpect(jsonPath("$[?(@.productoId == '%s')]".formatted(escaso)).isEmpty());
    }

    @Test
    @DisplayName("Regla real: cancelar un pedido devuelve el stock y una línea del pedido no se confirma por separado")
    void cancelarPedidoRestituyeStockYLaLineaNoSeOperaSola() throws Exception {
        // Arrange
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String nombre = "Headset-pedido-" + UUID.randomUUID();
        String productoId = publicarProducto(tokenVendedor, nombre, 90000, 10);

        String pedidoJson = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "productoId": "%s", "cantidad": 3 } ] }
                                """.formatted(productoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String pedidoId = JsonPath.read(pedidoJson, "$.pedidoId");
        String compraId = JsonPath.read(pedidoJson, "$.compras[0].id");

        mockMvc.perform(get("/api/productos")
                        .param("texto", nombre)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.contenido[0].stock").value(7));

        // Act 1: la línea del pedido no se confirma ni se cancela sola -> 422
        mockMvc.perform(put("/api/compras/{id}/confirmar", compraId)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/compras/{id}/cancelar", compraId)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isUnprocessableContent());

        // Act 2: cancelar el pedido completo
        mockMvc.perform(put("/api/pedidos/{id}/cancelar", pedidoId)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compras[0].estado").value("CANCELADA"));

        // Assert: el stock volvió a 10
        mockMvc.perform(get("/api/productos")
                        .param("texto", nombre)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(jsonPath("$.contenido[0].stock").value(10));
    }

    @Test
    @DisplayName("Seguridad real: otro comprador no puede confirmar el pedido ajeno (422) y un pedido inexistente da 404")
    void pedidoAjenoRetorna422YElInexistenteRetorna404() throws Exception {
        // Arrange: Jose crea su pedido
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenJose = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String tokenAna = iniciarSesion(ANA_CORREO, ANA_CLAVE);
        String productoId = publicarProducto(tokenVendedor, "Mousepad-pedido-" + UUID.randomUUID(), 30000, 5);
        String pedidoJson = mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenJose)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [ { "productoId": "%s", "cantidad": 1 } ] }
                                """.formatted(productoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String pedidoId = JsonPath.read(pedidoJson, "$.pedidoId");

        // Act + Assert: Ana no puede confirmarlo; un id desconocido es 404
        mockMvc.perform(put("/api/pedidos/{id}/confirmar", pedidoId)
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(put("/api/pedidos/{id}/confirmar", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validación real: un pedido sin ítems responde 400")
    void pedidoSinItemsRetorna400() throws Exception {
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"items\": [] }"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ Cupones

    /** Usos consumidos hasta ahora de un cupón, leídos con la vista previa del comprador. */
    private int usosDelCupon(String tokenComprador, String codigo) throws Exception {
        String json = mockMvc.perform(get("/api/cupones/{codigo}", codigo)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.usos");
    }

    @Test
    @DisplayName("Regla real: un pedido de 2 ítems con VERANO26 consume un solo uso del cupón")
    void pedidoConVERANO26ConsumeUnSoloUso() throws Exception {
        // Arrange: el cupón global del data.sql (15 %) y dos productos del vendedor
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String teclado = publicarProducto(tokenVendedor, "Teclado-cupon-" + UUID.randomUUID(), 150000, 10);
        String mouse = publicarProducto(tokenVendedor, "Mouse-cupon-" + UUID.randomUUID(), 80000, 10);
        // Los usos se leen con otro comprador (Ana): para José la vista previa ya dirá "ya lo usaste"
        String tokenAna = iniciarSesion(ANA_CORREO, ANA_CLAVE);
        int usosAntes = usosDelCupon(tokenAna, "VERANO26");

        // Act: un solo pedido con dos líneas y el cupón
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "codigoCupon": "verano26",
                                  "items": [
                                    { "productoId": "%s", "cantidad": 2 },
                                    { "productoId": "%s", "cantidad": 1 }
                                ] }
                                """.formatted(teclado, mouse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuponAplicado").value(true))
                .andExpect(jsonPath("$.compras[0].precioUnitario").value(127500.0))   // 150000 - 15 %
                .andExpect(jsonPath("$.compras[1].precioUnitario").value(68000.0))    //  80000 - 15 %
                .andExpect(jsonPath("$.total").value(323000.0));

        // Assert: el cupón, leído de la base, subió exactamente un uso (no uno por línea)
        assertEquals(usosAntes + 1, usosDelCupon(tokenAna, "VERANO26"));
    }

    @Test
    @DisplayName("Flujo real: el vendedor crea su cupón, el comprador lo usa una sola vez y un segundo intento responde 422")
    void vendedorCreaCuponYElCompradorLoUsaEnUnPedido() throws Exception {
        // Arrange
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String codigo = "TIENDA" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String cuponJson = """
                { "codigo": "%s", "porcentaje": 20, "vence": "%s", "usosMaximos": 3 }
                """.formatted(codigo.toLowerCase(), LocalDate.now().plusDays(30));

        // Act 1: el vendedor crea el cupón (el código se normaliza a mayúsculas y queda a su nombre)
        mockMvc.perform(post("/api/cupones")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuponJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.usos").value(0))
                .andExpect(jsonPath("$.vendedorId").isNotEmpty());

        // Act 2: el mismo código otra vez responde 409
        mockMvc.perform(post("/api/cupones")
                        .header("Authorization", "Bearer " + tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuponJson))
                .andExpect(status().isConflict());

        // Act 3: el comprador lo previsualiza y lo usa en un pedido de dos líneas
        assertEquals(0, usosDelCupon(tokenComprador, codigo));
        String teclado = publicarProducto(tokenVendedor, "Teclado-propio-" + UUID.randomUUID(), 100000, 10);
        String mouse = publicarProducto(tokenVendedor, "Mouse-propio-" + UUID.randomUUID(), 50000, 10);
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "codigoCupon": "%s",
                                  "items": [
                                    { "productoId": "%s", "cantidad": 1 },
                                    { "productoId": "%s", "cantidad": 1 }
                                ] }
                                """.formatted(codigo, teclado, mouse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuponAplicado").value(true))
                .andExpect(jsonPath("$.total").value(120000.0));   // (100000 + 50000) - 20 %

        // Act 4: el mismo comprador no puede usar el cupón otra vez (ni en vista previa ni en un pedido)
        mockMvc.perform(get("/api/cupones/{codigo}", codigo)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "codigoCupon": "%s",
                                  "items": [ { "productoId": "%s", "cantidad": 1 } ] }
                                """.formatted(codigo, teclado)))
                .andExpect(status().isUnprocessableContent());

        // Assert: el vendedor ve su cupón con un solo uso (el intento rechazado no consumió nada)
        mockMvc.perform(get("/api/cupones")
                        .header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == '%s')].usos".formatted(codigo)).value(1));
    }

    @Test
    @DisplayName("Regla real: un cupón inexistente responde 404 y el stock no cambia")
    void cuponInexistenteRetorna404YNoDescuentaStock() throws Exception {
        String tokenVendedor = iniciarSesion(VENDEDOR_CORREO, VENDEDOR_CLAVE);
        String tokenComprador = iniciarSesion(COMPRADOR_CORREO, COMPRADOR_CLAVE);
        String nombre = "Teclado-sin-cupon-" + UUID.randomUUID();
        String producto = publicarProducto(tokenVendedor, nombre, 150000, 10);

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer " + tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "codigoCupon": "NOEXISTE",
                                  "items": [ { "productoId": "%s", "cantidad": 2 } ] }
                                """.formatted(producto)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/productos")
                        .param("texto", nombre)
                        .header("Authorization", "Bearer " + tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].stock").value(10));
    }

    // ------------------------------------------------------------------ Baja de cuenta y token vigente

    private void registrarComprador(String correo, String contrasena) throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Comprador baja", "correo": "%s", "contrasena": "%s" }
                                """.formatted(correo, contrasena)))
                .andExpect(status().isCreated());
    }

    private void registrarVendedor(String correo, String contrasena, String documento) throws Exception {
        mockMvc.perform(post("/api/vendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Vendedor baja", "correo": "%s", "tipo": "EMPRESA",
                                  "documento": "%s", "contrasena": "%s" }
                                """.formatted(correo, documento, contrasena)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Seguridad real: tras la baja, el JWT vigente del comprador deja de servir al instante (401)")
    void tokenDeCompradorDadoDeBajaRetorna401() throws Exception {
        // Arrange: comprador nuevo con sesión activa
        String correo = "baja-" + UUID.randomUUID() + "@correo.com";
        registrarComprador(correo, "BajaGamer2026");
        String token = iniciarSesion(correo, "BajaGamer2026");
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Act: se da de baja con ese mismo token
        mockMvc.perform(put("/api/usuarios/me/baja").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Assert: el token no ha expirado, pero ya no abre ninguna ruta protegida
        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
        mockMvc.perform(get("/api/compras").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
        mockMvc.perform(put("/api/usuarios/me/baja").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Assert: el catálogo público sigue disponible aunque el token ya no sirva
        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Assert: tampoco puede iniciar sesión de nuevo
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "correo": "%s", "contrasena": "BajaGamer2026" }
                                """.formatted(correo)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Seguridad real: tras la baja, el JWT vigente del vendedor deja de servir al instante (401)")
    void tokenDeVendedorDadoDeBajaRetorna401() throws Exception {
        // Arrange: vendedor nuevo con sesión activa
        String correo = "vendedor-baja-" + UUID.randomUUID() + "@tienda.com";
        String documento = "9" + Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);
        registrarVendedor(correo, "BajaVendedor2026", documento);
        String token = iniciarSesion(correo, "BajaVendedor2026");
        mockMvc.perform(get("/api/vendedores/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Act
        mockMvc.perform(put("/api/vendedores/me/baja").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Assert: el mismo token ya no sirve, ni siquiera para publicar
        mockMvc.perform(get("/api/vendedores/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Mouse-baja-%s", "descripcion": "No debe publicarse nunca",
                                  "precio": 50000, "stock": 1,
                                  "gama": "BAJA", "exclusividad": "NORMAL" }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}