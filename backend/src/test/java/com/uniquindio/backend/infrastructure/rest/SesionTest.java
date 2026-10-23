package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.domain.valueobject.Rol;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Simula una sesión JWT: principal = UUID, autoridad = ROLE_<rol>. */
public final class SesionTest {

    private SesionTest() {
    }

    public static RequestPostProcessor como(UUID id, Rol rol) {
        return authentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    public static RequestPostProcessor comprador(UUID id) {
        return como(id, Rol.COMPRADOR);
    }

    public static RequestPostProcessor vendedor(UUID id) {
        return como(id, Rol.VENDEDOR);
    }

    /** MockMvc con la cadena de Spring Security real aplicada (filtro JWT, 401/403 y @AuthenticationPrincipal). */
    public static MockMvc mockMvcConSeguridad(WebApplicationContext contexto) {
        return MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .build();
    }
}