package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.infrastructure.security.JwtEmisorToken;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthInterceptorTest {

    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(
            new JwtEmisorToken("secreto-de-prueba-con-mas-de-32-caracteres", 2, Clock.systemUTC()));

    @Test
    void permiteSinTokenSoloLosPostDeRegistro() {
        assertTrue(preHandle("POST", "/api/usuarios"));
        assertTrue(preHandle("POST", "/api/vendedores"));

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/usuarios");
        request.setServletPath("/api/usuarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    @Test
    void rutasDeOtrasColeccionesSiguenExigiendoToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/productos");
        request.setServletPath("/api/productos");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
    }

    private boolean preHandle(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }
}
