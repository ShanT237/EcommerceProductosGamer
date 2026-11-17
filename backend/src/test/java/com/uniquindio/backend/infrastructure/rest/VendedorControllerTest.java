package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CambiarContrasenaVendedorUseCase;
import com.uniquindio.backend.application.usecase.DarDeBajaVendedorUseCase;
import com.uniquindio.backend.application.usecase.EditarPerfilVendedorUseCase;
import com.uniquindio.backend.application.usecase.ObtenerPerfilVendedorUseCase;
import com.uniquindio.backend.application.usecase.RegistrarVendedorUseCase;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.VendedorMapper;
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

import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendedorController.class)
@Import({SecurityConfig.class, VendedorMapper.class})
class VendedorControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private RegistrarVendedorUseCase registrarVendedorUseCase;

    @MockitoBean
    private DarDeBajaVendedorUseCase darDeBajaVendedorUseCase;

    @MockitoBean
    private ObtenerPerfilVendedorUseCase obtenerPerfilVendedorUseCase;

    @MockitoBean
    private EditarPerfilVendedorUseCase editarPerfilVendedorUseCase;

    @MockitoBean
    private CambiarContrasenaVendedorUseCase cambiarContrasenaVendedorUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID vendedorId = UUID.randomUUID();

    private Vendedor vendedorRegistrado() {
        return Vendedor.registrar(vendedorId, "GamerZone", "ventas@gamerzone.com",
                TipoVendedor.EMPRESA, "900123");
    }

    // ===================== registrar (público) =====================

    @Test
    void registrarNoPideToken() throws Exception {
        when(registrarVendedorUseCase.ejecutar(any(), eq("GamerZone"), eq("ventas@gamerzone.com"),
                eq(TipoVendedor.EMPRESA), eq("900123"), eq("Vendedor2026"))).thenReturn(vendedorRegistrado());

        String json = """
            { "nombre": "GamerZone", "correo": "ventas@gamerzone.com", "tipo": "EMPRESA",
              "documento": "900123", "contrasena": "Vendedor2026" }
            """;

        mockMvc.perform(post("/api/vendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nombre").value("GamerZone"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void registrarConCorreoInvalidoRetorna400() throws Exception {
        String json = """
            { "nombre": "GamerZone", "correo": "no-es-correo", "tipo": "EMPRESA",
              "documento": "900123", "contrasena": "Vendedor2026" }
            """;

        mockMvc.perform(post("/api/vendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());              // 400
    }

    // ===================== perfil (CU-26) =====================

    @Test
    void obtenerPerfilUsaElVendedorDelToken() throws Exception {
        when(obtenerPerfilVendedorUseCase.ejecutar(vendedorId)).thenReturn(vendedorRegistrado());

        mockMvc.perform(get("/api/vendedores/me")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(vendedorId.toString()))
                .andExpect(jsonPath("$.nombre").value("GamerZone"))
                .andExpect(jsonPath("$.correo").value("ventas@gamerzone.com"))
                .andExpect(jsonPath("$.tipo").value("EMPRESA"))
                .andExpect(jsonPath("$.documento").value("900123"));
    }

    @Test
    void obtenerPerfilSinTokenRetorna401YComoCompradorRetorna403() throws Exception {
        mockMvc.perform(get("/api/vendedores/me"))
                .andExpect(status().isUnauthorized());            // 401

        mockMvc.perform(get("/api/vendedores/me")
                        .with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(obtenerPerfilVendedorUseCase);
    }

    @Test
    void editarPerfilUsaElVendedorDelToken() throws Exception {
        Vendedor editado = vendedorRegistrado();
        editado.actualizarPerfil("GamerZone Pro", "pro@gamerzone.com");
        when(editarPerfilVendedorUseCase.ejecutar(vendedorId, "GamerZone Pro", "pro@gamerzone.com"))
                .thenReturn(editado);

        mockMvc.perform(put("/api/vendedores/me/perfil")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"GamerZone Pro\", \"correo\": \"pro@gamerzone.com\" }"))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.nombre").value("GamerZone Pro"))
                .andExpect(jsonPath("$.correo").value("pro@gamerzone.com"));

        Mockito.verify(editarPerfilVendedorUseCase).ejecutar(vendedorId, "GamerZone Pro", "pro@gamerzone.com");
    }

    @Test
    void editarPerfilConDatosInvalidosRetorna400() throws Exception {
        mockMvc.perform(put("/api/vendedores/me/perfil")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"\", \"correo\": \"no-es-correo\" }"))
                .andExpect(status().isBadRequest());              // 400

        Mockito.verifyNoInteractions(editarPerfilVendedorUseCase);
    }

    @Test
    void editarPerfilConCorreoDeOtraCuentaRetorna409() throws Exception {
        when(editarPerfilVendedorUseCase.ejecutar(any(), any(), any()))
                .thenThrow(new ConflictoException("Ya existe una cuenta con ese correo"));

        mockMvc.perform(put("/api/vendedores/me/perfil")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"GamerZone\", \"correo\": \"otro@tienda.com\" }"))
                .andExpect(status().isConflict())                 // 409
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void editarPerfilSinTokenRetorna401YComoCompradorRetorna403() throws Exception {
        String json = "{ \"nombre\": \"GamerZone\", \"correo\": \"ventas@gamerzone.com\" }";

        mockMvc.perform(put("/api/vendedores/me/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());            // 401

        mockMvc.perform(put("/api/vendedores/me/perfil")
                        .with(comprador(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(editarPerfilVendedorUseCase);
    }

    // ===================== contraseña (CU-26) =====================

    @Test
    void cambiarContrasenaUsaElVendedorDelToken() throws Exception {
        mockMvc.perform(put("/api/vendedores/me/contrasena")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contrasenaActual\": \"Vendedor2026\", \"contrasenaNueva\": \"Nueva2027x\" }"))
                .andExpect(status().isNoContent());               // 204

        Mockito.verify(cambiarContrasenaVendedorUseCase).ejecutar(vendedorId, "Vendedor2026", "Nueva2027x");
    }

    @Test
    void cambiarContrasenaConContrasenaNuevaCortaRetorna400() throws Exception {
        mockMvc.perform(put("/api/vendedores/me/contrasena")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contrasenaActual\": \"Vendedor2026\", \"contrasenaNueva\": \"corta\" }"))
                .andExpect(status().isBadRequest());              // 400

        Mockito.verifyNoInteractions(cambiarContrasenaVendedorUseCase);
    }

    @Test
    void cambiarContrasenaConActualIncorrectaRetorna422() throws Exception {
        Mockito.doThrow(new ReglaDominioException("La contraseña actual es incorrecta"))
                .when(cambiarContrasenaVendedorUseCase).ejecutar(any(), any(), any());

        mockMvc.perform(put("/api/vendedores/me/contrasena")
                        .with(vendedor(vendedorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contrasenaActual\": \"Equivocada1\", \"contrasenaNueva\": \"Nueva2027x\" }"))
                .andExpect(status().isUnprocessableContent());    // 422
    }

    @Test
    void cambiarContrasenaSinTokenRetorna401YComoCompradorRetorna403() throws Exception {
        String json = "{ \"contrasenaActual\": \"Vendedor2026\", \"contrasenaNueva\": \"Nueva2027x\" }";

        mockMvc.perform(put("/api/vendedores/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());            // 401

        mockMvc.perform(put("/api/vendedores/me/contrasena")
                        .with(comprador(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());               // 403

        Mockito.verifyNoInteractions(cambiarContrasenaVendedorUseCase);
    }

    // ===================== baja =====================

    @Test
    void darDeBajaUsaElVendedorDelToken() throws Exception {
        Vendedor baja = vendedorRegistrado();
        baja.darDeBaja();
        when(darDeBajaVendedorUseCase.ejecutar(vendedorId)).thenReturn(baja);

        mockMvc.perform(put("/api/vendedores/me/baja")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(vendedorId.toString()))
                .andExpect(jsonPath("$.activo").value(false));

        Mockito.verify(darDeBajaVendedorUseCase).ejecutar(vendedorId);
    }

    @Test
    void yaNoExisteLaBajaPorIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();

        mockMvc.perform(put("/api/vendedores/{id}/baja", ajeno)
                        .with(vendedor(vendedorId)))
                .andExpect(status().isNotFound());                // 404

        Mockito.verifyNoInteractions(darDeBajaVendedorUseCase);
    }

    @Test
    void darDeBajaDosVecesRetorna422() throws Exception {
        when(darDeBajaVendedorUseCase.ejecutar(vendedorId))
                .thenThrow(new ReglaDominioException("El vendedor ya está dado de baja"));

        mockMvc.perform(put("/api/vendedores/me/baja")
                        .with(vendedor(vendedorId)))
                .andExpect(status().isUnprocessableContent());     // 422
    }

    @Test
    void darDeBajaSinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(put("/api/vendedores/me/baja"))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(darDeBajaVendedorUseCase);
    }

    @Test
    void unCompradorNoPuedeDarDeBajaRetorna403() throws Exception {
        mockMvc.perform(put("/api/vendedores/me/baja")
                        .with(comprador(UUID.randomUUID())))
                .andExpect(status().isForbidden())                // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(darDeBajaVendedorUseCase);
    }

    @Test
    void registrarConDocumentoDuplicadoRetorna409() throws Exception {
        when(registrarVendedorUseCase.ejecutar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ConflictoException("Ya existe un vendedor con ese documento"));

        String json = """
        { "nombre": "GamerZone", "correo": "ventas@gamerzone.com", "tipo": "EMPRESA",
          "documento": "900123", "contrasena": "Vendedor2026" }
        """;

        mockMvc.perform(post("/api/vendedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())                 // 409
                .andExpect(jsonPath("$.status").value(409));
    }
}
