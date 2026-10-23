package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.ComprarComboUseCase;
import com.uniquindio.backend.application.usecase.CrearComboUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductosComboUseCase;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ComboMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComboController.class)
@Import({SecurityConfig.class, ComboMapper.class})
class ComboControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private CrearComboUseCase crearComboUseCase;

    @MockitoBean
    private ModificarProductosComboUseCase modificarProductosComboUseCase;

    @MockitoBean
    private ComprarComboUseCase comprarComboUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID comboId = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    private Combo combo() {
        return Combo.crear(comboId, vendedorId, "Combo Gamer", "Teclado + Mouse", 15.0, List.of(p1, p2));
    }

    private String cuerpoCrear(String extra) {
        return """
            { %s "nombre": "Combo Gamer", "descripcion": "Teclado + Mouse", "descuento": 15,
              "productoIds": ["%s", "%s"] }
            """.formatted(extra, p1, p2);
    }

    @Test
    void crearUsaElVendedorDelToken() throws Exception {
        when(crearComboUseCase.ejecutar(any(), eq(vendedorId), eq("Combo Gamer"), eq("Teclado + Mouse"),
                eq(15.0), eq(List.of(p1, p2)))).thenReturn(combo());

        mockMvc.perform(post("/api/combos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear("")))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()));

        Mockito.verify(crearComboUseCase).ejecutar(any(), eq(vendedorId), eq("Combo Gamer"),
                eq("Teclado + Mouse"), eq(15.0), eq(List.of(p1, p2)));
    }

    @Test
    void crearIgnoraUnVendedorIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(crearComboUseCase.ejecutar(any(), eq(vendedorId), any(), any(),
                Mockito.anyDouble(), any())).thenReturn(combo());

        mockMvc.perform(post("/api/combos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear("\"vendedorId\": \"%s\",".formatted(ajeno))))
                .andExpect(status().isCreated());

        Mockito.verify(crearComboUseCase, Mockito.never()).ejecutar(any(), eq(ajeno), any(), any(),
                Mockito.anyDouble(), any());
    }

    @Test
    void crearConUnSoloProductoRetorna400() throws Exception {
        String json = """
            { "nombre": "Combo", "descripcion": "Desc", "descuento": 10, "productoIds": ["%s"] }
            """.formatted(p1);

        mockMvc.perform(post("/api/combos")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void agregarProductoUsaElVendedorDelToken() throws Exception {
        when(modificarProductosComboUseCase.agregar(comboId, vendedorId, p1)).thenReturn(combo());

        mockMvc.perform(put("/api/combos/{id}/productos/{productoId}", comboId, p1)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(modificarProductosComboUseCase).agregar(comboId, vendedorId, p1);
    }

    @Test
    void quitarProductoUsaElVendedorDelToken() throws Exception {
        when(modificarProductosComboUseCase.quitar(comboId, vendedorId, p1)).thenReturn(combo());

        mockMvc.perform(delete("/api/combos/{id}/productos/{productoId}", comboId, p1)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(modificarProductosComboUseCase).quitar(comboId, vendedorId, p1);
    }

    @Test
    void comprarUsaElUsuarioDelToken() throws Exception {
        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, p1, 2,
                LocalDateTime.now(), new Precio(85));
        when(comprarComboUseCase.ejecutar(usuarioId, comboId, 2)).thenReturn(List.of(compra));

        mockMvc.perform(post("/api/combos/{id}/comprar", comboId)
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"cantidad\": 2 }"))
                .andExpect(status().isCreated())                  // 201
                .andExpect(jsonPath("$.comboId").value(comboId.toString()));

        Mockito.verify(comprarComboUseCase).ejecutar(usuarioId, comboId, 2);
    }

    @Test
    void comprarIgnoraUnUsuarioIdEnElBody() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(comprarComboUseCase.ejecutar(usuarioId, comboId, 1)).thenReturn(List.of());

        mockMvc.perform(post("/api/combos/{id}/comprar", comboId)
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"usuarioId\": \"%s\", \"cantidad\": 1 }".formatted(ajeno)))
                .andExpect(status().isCreated());

        Mockito.verify(comprarComboUseCase, Mockito.never()).ejecutar(eq(ajeno), any(), Mockito.anyInt());
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(post("/api/combos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear("")))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}