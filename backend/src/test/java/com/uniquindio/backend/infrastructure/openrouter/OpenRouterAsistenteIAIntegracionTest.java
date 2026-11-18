package com.uniquindio.backend.infrastructure.openrouter;

import com.uniquindio.backend.application.port.out.MensajeIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.List;

import static com.uniquindio.backend.application.port.out.MensajeIA.Rol.SISTEMA;
import static com.uniquindio.backend.application.port.out.MensajeIA.Rol.USUARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Verifica el adaptador contra el contrato HTTP de OpenRouter (simulado) y, con API key, contra el real. */
class OpenRouterAsistenteIAIntegracionTest {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final String URL_CHAT = BASE_URL + "/chat/completions";

    private MockRestServiceServer servidor;
    private RestTemplate restTemplate;

    @BeforeEach
    void preparar() {
        restTemplate = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
    }

    private OpenRouterAsistenteIA adaptador(String apiKey) {
        return new OpenRouterAsistenteIA(apiKey, BASE_URL, "openrouter/auto", 700,
                "http://localhost:4200", restTemplate);
    }

    private static List<MensajeIA> conversacion() {
        return List.of(new MensajeIA(SISTEMA, "Eres el asistente de GamerZone"),
                new MensajeIA(USUARIO, "Recomiéndame un teclado"));
    }

    @Test
    void respuestaExitosa_devuelveElTextoYEnviaModeloAutoBearerYMensajes() {
        servidor.expect(requestTo(URL_CHAT))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.model").value("openrouter/auto"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"  Te recomiendo el Teclado mecánico RGB.  "}}]}
                        """, MediaType.APPLICATION_JSON));

        String respuesta = adaptador("sk-test").responder(conversacion());

        assertEquals("Te recomiendo el Teclado mecánico RGB.", respuesta);
        servidor.verify();
    }

    @Test
    void rateLimit429_seTraduceA429ConMensajeAmigable() {
        servidor.expect(requestTo(URL_CHAT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(429, error.getStatusCode().value());
        assertEquals(OpenRouterAsistenteIA.MSG_LIMITE, error.getReason());
    }

    @Test
    void claveInvalida401_seTraduceA503SinFiltrarDetalleDelProveedor() {
        servidor.expect(requestTo(URL_CHAT)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"No auth credentials found\"}}"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(503, error.getStatusCode().value());
        assertFalse(error.getReason().contains("credentials"));
    }

    @Test
    void error500_seTraduceA503() {
        servidor.expect(requestTo(URL_CHAT)).andRespond(withServerError());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void timeout_seTraduceA504() {
        servidor.expect(requestTo(URL_CHAT))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(504, error.getStatusCode().value());
    }

    @Test
    void cuerpoSinChoices_seTraduceA502() {
        servidor.expect(requestTo(URL_CHAT))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(502, error.getStatusCode().value());
    }

    @Test
    void contenidoVacio_seTraduceA502() {
        servidor.expect(requestTo(URL_CHAT))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"   \"}}]}",
                        MediaType.APPLICATION_JSON));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("sk-test").responder(conversacion()));

        assertEquals(502, error.getStatusCode().value());
    }

    @Test
    void sinApiKey_responde503SinLlamarAlProveedor() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> adaptador("").responder(conversacion()));

        assertEquals(503, error.getStatusCode().value());
        assertEquals(OpenRouterAsistenteIA.MSG_NO_CONFIGURADO, error.getReason());
        servidor.verify(); // sin expectativas: cualquier llamada habría fallado
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
    void pruebaReal_contraOpenRouter() {
        OpenRouterAsistenteIA real = new OpenRouterAsistenteIA(System.getenv("OPENROUTER_API_KEY"), BASE_URL,
                System.getenv().getOrDefault("OPENROUTER_MODEL", "openrouter/auto"), 700,
                "http://localhost:4200");

        String respuesta = real.responder(List.of(new MensajeIA(USUARIO, "Responde solo con la palabra OK")));

        assertTrue(respuesta != null && !respuesta.isBlank());
    }
}
