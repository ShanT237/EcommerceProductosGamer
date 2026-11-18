package com.uniquindio.backend.infrastructure.openrouter;

import com.uniquindio.backend.application.port.out.AsistenteIAPort;
import com.uniquindio.backend.application.port.out.MensajeIA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Habla con OpenRouter (API compatible con OpenAI: {@code POST {baseUrl}/chat/completions}).
 *
 * Variables de entorno: OPENROUTER_API_KEY y, opcionalmente, OPENROUTER_MODEL (por defecto
 * {@code openrouter/auto}) y OPENROUTER_BASE_URL. Sin API key no se llama al proveedor y se
 * responde 503. Nunca se registra la API key ni el contenido de la conversación.
 */
@Component
public class OpenRouterAsistenteIA implements AsistenteIAPort {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterAsistenteIA.class);
    private static final double TEMPERATURA = 0.4;

    static final String MSG_NO_CONFIGURADO = "El asistente de IA no está configurado";
    static final String MSG_LIMITE = "El asistente está recibiendo muchas solicitudes. Intenta de nuevo en unos segundos";
    static final String MSG_NO_DISPONIBLE = "El asistente de IA no está disponible en este momento";
    static final String MSG_TIMEOUT = "El asistente de IA tardó demasiado en responder";
    static final String MSG_INVALIDA = "El asistente de IA devolvió una respuesta inválida";

    private final String apiKey;
    private final String urlChat;
    private final String modelo;
    private final int maxTokens;
    private final String referer;
    private final RestTemplate restTemplate;

    @Autowired
    public OpenRouterAsistenteIA(
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.model:openrouter/auto}") String modelo,
            @Value("${openrouter.max-tokens:700}") int maxTokens,
            @Value("${app.frontend-url:http://localhost:4200}") String referer) {
        this(apiKey, baseUrl, modelo, maxTokens, referer, crearRestTemplate());
    }

    OpenRouterAsistenteIA(String apiKey, String baseUrl, String modelo, int maxTokens, String referer,
                          RestTemplate restTemplate) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String base = baseUrl == null ? "" : baseUrl.trim();
        this.urlChat = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/chat/completions";
        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.referer = referer;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(fabrica);
    }

    @Override
    public String responder(List<MensajeIA> conversacion) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MSG_NO_CONFIGURADO);
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo);
        cuerpo.put("messages", conversacion.stream().map(OpenRouterAsistenteIA::aMensajeJson).toList());
        cuerpo.put("max_tokens", maxTokens);
        cuerpo.put("temperature", TEMPERATURA);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("HTTP-Referer", referer);
        headers.set("X-Title", "GamerZone");

        try {
            Map<?, ?> respuesta = restTemplate.postForObject(urlChat, new HttpEntity<>(cuerpo, headers), Map.class);
            return extraerTexto(respuesta);
        } catch (HttpStatusCodeException e) {
            int estado = e.getStatusCode().value();
            log.error("OpenRouter respondió HTTP {}: {}", estado, e.getResponseBodyAsString());
            if (estado == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, MSG_LIMITE);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MSG_NO_DISPONIBLE);
        } catch (ResourceAccessException e) {
            log.error("No se pudo contactar con OpenRouter: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, MSG_TIMEOUT);
        } catch (RestClientException e) {
            log.error("Respuesta ilegible de OpenRouter: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, MSG_INVALIDA);
        }
    }

    private static Map<String, String> aMensajeJson(MensajeIA mensaje) {
        String rol = switch (mensaje.rol()) {
            case SISTEMA -> "system";
            case USUARIO -> "user";
            case ASISTENTE -> "assistant";
        };
        return Map.of("role", rol, "content", mensaje.contenido());
    }

    /** choices[0].message.content; cualquier otra forma (sin choices, sin contenido, en blanco) es inválida. */
    private static String extraerTexto(Map<?, ?> respuesta) {
        if (respuesta != null
                && respuesta.get("choices") instanceof List<?> choices && !choices.isEmpty()
                && choices.get(0) instanceof Map<?, ?> primera
                && primera.get("message") instanceof Map<?, ?> mensaje
                && mensaje.get("content") instanceof String texto && !texto.isBlank()) {
            return texto.trim();
        }
        log.error("OpenRouter devolvió una respuesta sin contenido utilizable");
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, MSG_INVALIDA);
    }
}
