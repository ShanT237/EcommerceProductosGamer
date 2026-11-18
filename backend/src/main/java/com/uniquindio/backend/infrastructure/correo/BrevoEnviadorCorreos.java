package com.uniquindio.backend.infrastructure.correo;

import com.uniquindio.backend.application.port.out.EnviadorCorreos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Envía correos por la API HTTPS de Brevo. No usa SMTP a propósito: el plan gratuito de Render
 * bloquea los puertos SMTP salientes.
 *
 * Variables de entorno: BREVO_API_KEY, CORREO_REMITENTE (un remitente verificado en Brevo) y,
 * opcionalmente, CORREO_REMITENTE_NOMBRE. Si faltan, no se envía nada: el correo se imprime en el
 * log del backend (modo desarrollo), lo que permite probar el flujo sin configurar un proveedor.
 */
@Component
public class BrevoEnviadorCorreos implements EnviadorCorreos {

    private static final Logger log = LoggerFactory.getLogger(BrevoEnviadorCorreos.class);
    private static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final RestTemplate restTemplate;

    @Autowired
    public BrevoEnviadorCorreos(
            @Value("${BREVO_API_KEY:}") String apiKey,
            @Value("${CORREO_REMITENTE:}") String remitente,
            @Value("${CORREO_REMITENTE_NOMBRE:GamerZone}") String nombreRemitente) {
        this(apiKey, remitente, nombreRemitente, crearRestTemplate());
    }

    BrevoEnviadorCorreos(String apiKey, String remitente, String nombreRemitente, RestTemplate restTemplate) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(fabrica);
    }

    @Override
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        if (apiKey.isBlank() || remitente.isBlank()) {
            log.warn("Correo NO enviado: faltan BREVO_API_KEY o CORREO_REMITENTE (modo desarrollo). "
                    + "Para: {} | Asunto: {} | Contenido: {}", destinatario, asunto, contenidoHtml);
            return;
        }
        // En segundo plano: la respuesta HTTP no espera al proveedor ni delata si el correo existe.
        CompletableFuture.runAsync(() -> enviarAhora(destinatario, asunto, contenidoHtml));
    }

    private void enviarAhora(String destinatario, String asunto, String contenidoHtml) {
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", nombreRemitente, "email", remitente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "htmlContent", contenidoHtml);

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            restTemplate.postForEntity(URL_BREVO, new HttpEntity<>(cuerpo, headers), String.class);
            log.info("Correo enviado a {}", enmascarar(destinatario));
        } catch (HttpStatusCodeException e) {
            log.error("Brevo rechazó el envío (HTTP {}): {}", e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("No se pudo contactar con Brevo: {}", e.getMessage());
        }
    }

    private static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        return arroba <= 1 ? "***" : correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
