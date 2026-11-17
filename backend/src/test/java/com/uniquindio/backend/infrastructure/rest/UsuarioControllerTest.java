package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.CambiarContrasenaUseCase;
import com.uniquindio.backend.application.usecase.DarDeBajaUsuarioUseCase;
import com.uniquindio.backend.application.usecase.EditarPerfilUseCase;
import com.uniquindio.backend.application.usecase.ObtenerPerfilUseCase;
import com.uniquindio.backend.application.usecase.RegistrarUsuarioUseCase;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.UsuarioMapper;
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

import java.util.NoSuchElementException;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, UsuarioMapper.class})
class UsuarioControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @MockitoBean
    private EditarPerfilUseCase editarPerfilUseCase;

    @MockitoBean
    private ObtenerPerfilUseCase obtenerPerfilUseCase;

    @MockitoBean
    private CambiarContrasenaUseCase cambiarContrasenaUseCase;

    @MockitoBean
    private DarDeBajaUsuarioUseCase darDeBajaUsuarioUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();

    private Usuario usuarioDadoDeBaja() {
        Usuario usuario = Usuario.registrar(usuarioId, "Gamer", "gamer@test.com");
        usuario.darDeBaja();
        return usuario;
    }

    // ===================== baja (CU-25) =====================

    @Test
    void darDeBajaUsaElCompradorDelToken() throws Exception {
        when(darDeBajaUsuarioUseCase.ejecutar(usuarioId)).thenReturn(usuarioDadoDeBaja());

        mockMvc.perform(put("/api/usuarios/me/baja")
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.id").value(usuarioId.toString()))
                .andExpect(jsonPath("$.activo").value(false));

        Mockito.verify(darDeBajaUsuarioUseCase).ejecutar(usuarioId);
    }

    @Test
    void darDeBajaDosVecesRetorna422() throws Exception {
        when(darDeBajaUsuarioUseCase.ejecutar(usuarioId))
                .thenThrow(new ReglaDominioException("El usuario ya está dado de baja"));

        mockMvc.perform(put("/api/usuarios/me/baja")
                        .with(comprador(usuarioId)))
                .andExpect(status().isUnprocessableContent());    // 422
    }

    @Test
    void darDeBajaDeUsuarioInexistenteRetorna404() throws Exception {
        when(darDeBajaUsuarioUseCase.ejecutar(usuarioId))
                .thenThrow(new NoSuchElementException("El usuario no existe"));

        mockMvc.perform(put("/api/usuarios/me/baja")
                        .with(comprador(usuarioId)))
                .andExpect(status().isNotFound());                // 404
    }

    @Test
    void darDeBajaSinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/baja"))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(darDeBajaUsuarioUseCase);
    }

    @Test
    void unVendedorNoPuedeUsarLaBajaDeCompradorRetorna403() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/baja")
                        .with(vendedor(UUID.randomUUID())))
                .andExpect(status().isForbidden())                // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(darDeBajaUsuarioUseCase);
    }
}
