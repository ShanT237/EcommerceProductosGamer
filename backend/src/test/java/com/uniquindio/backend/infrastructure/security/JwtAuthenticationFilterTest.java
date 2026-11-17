package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.domain.valueobject.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private EmisorToken emisorToken;
    private VerificadorCuentaActiva verificador;
    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain cadena;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        emisorToken = mock(EmisorToken.class);
        verificador = mock(VerificadorCuentaActiva.class);
        filtro = new JwtAuthenticationFilter(emisorToken, verificador);
        request = new MockHttpServletRequest("GET", "/api/compras");
        response = new MockHttpServletResponse();
        cadena = new MockFilterChain();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conCuentaActivaAutenticaConElIdYElRol() throws Exception {
        UUID id = UUID.randomUUID();
        SesionToken sesion = new SesionToken(id, Rol.COMPRADOR);
        request.addHeader("Authorization", "Bearer token-bueno");
        when(emisorToken.validar("token-bueno")).thenReturn(Optional.of(sesion));
        when(verificador.estaActiva(sesion)).thenReturn(true);

        filtro.doFilter(request, response, cadena);

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacion);
        assertEquals(id, autenticacion.getPrincipal());
        assertTrue(autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPRADOR")));
        assertNull(request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_CUENTA_INACTIVA));
        assertNotNull(cadena.getRequest()); // la petición siguió su curso
    }

    @Test
    void conCuentaInactivaNoAutenticaYMarcaLaPeticion() throws Exception {
        SesionToken sesion = new SesionToken(UUID.randomUUID(), Rol.VENDEDOR);
        request.addHeader("Authorization", "Bearer token-de-cuenta-dada-de-baja");
        when(emisorToken.validar("token-de-cuenta-dada-de-baja")).thenReturn(Optional.of(sesion));
        when(verificador.estaActiva(sesion)).thenReturn(false);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(true, request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_CUENTA_INACTIVA));
        // El filtro no responde: deja pasar para que las rutas públicas sigan funcionando
        // y el entry point devuelva el 401 en las protegidas.
        assertNotNull(cadena.getRequest());
    }

    @Test
    void conTokenInvalidoNoConsultaLaCuenta() throws Exception {
        request.addHeader("Authorization", "Bearer token-malo");
        when(emisorToken.validar("token-malo")).thenReturn(Optional.empty());

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_CUENTA_INACTIVA));
        verifyNoInteractions(verificador);
    }

    @Test
    void sinCabeceraNoValidaNiConsultaLaCuenta() throws Exception {
        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(emisorToken, verificador);
    }
}
