package com.uniquindio.backend.infrastructure.cloudinary;

import com.uniquindio.backend.application.port.out.AlmacenImagenesProductos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Component
public class CloudinaryAlmacenImagenesProductos implements AlmacenImagenesProductos {

    private static final int TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final int DIMENSION_MAXIMA = 6000;

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String cloudinaryUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public CloudinaryAlmacenImagenesProductos(
            @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
            @Value("${CLOUDINARY_API_KEY:}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET:}") String apiSecret,
            @Value("${CLOUDINARY_URL:}") String cloudinaryUrl) {
        this(cloudName, apiKey, apiSecret, cloudinaryUrl, new RestTemplate());
    }

    CloudinaryAlmacenImagenesProductos(String cloudName, String apiKey, String apiSecret) {
        this(cloudName, apiKey, apiSecret, "", new RestTemplate());
    }

    CloudinaryAlmacenImagenesProductos(String cloudName, String apiKey, String apiSecret,
                                       String cloudinaryUrl, RestTemplate restTemplate) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.cloudinaryUrl = cloudinaryUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public String subir(byte[] contenido, String nombreArchivo) {
        String formato = validarImagen(contenido);
        Credenciales credenciales = credenciales();

        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> parametrosFirmados = new TreeMap<>();
        parametrosFirmados.put("folder", "gamerzone/productos");
        parametrosFirmados.put("timestamp", Long.toString(timestamp));

        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("file", archivo(contenido, nombreSeguro(nombreArchivo), formato));
        cuerpo.add("api_key", credenciales.apiKey());
        parametrosFirmados.forEach(cuerpo::add);
        cuerpo.add("signature", firmar(parametrosFirmados, credenciales.apiSecret()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            RespuestaCloudinary respuesta = restTemplate.postForObject(
                    "https://api.cloudinary.com/v1_1/" + credenciales.cloudName() + "/image/upload",
                    new HttpEntity<>(cuerpo, headers),
                    RespuestaCloudinary.class);
            if (respuesta == null || respuesta.secureUrl() == null || respuesta.secureUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Cloudinary no devolvió la URL segura de la imagen");
            }
            return respuesta.secureUrl();
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary rechazó la carga (HTTP " + e.getStatusCode().value()
                            + "). Verifica las credenciales y la firma configuradas");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con Cloudinary");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary no pudo procesar la respuesta de carga");
        }
    }

    private Credenciales credenciales() {
        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            return new Credenciales(cloudName, apiKey, apiSecret);
        }
        if (cloudinaryUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La carga de imágenes no está configurada en el backend");
        }

        try {
            URI uri = URI.create(cloudinaryUrl);
            String userInfo = uri.getUserInfo();
            int separador = userInfo == null ? -1 : userInfo.indexOf(':');
            if (!"cloudinary".equals(uri.getScheme()) || uri.getHost() == null
                    || separador <= 0 || separador == userInfo.length() - 1
                    || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid Cloudinary URL");
            }
            return new Credenciales(uri.getHost(), userInfo.substring(0, separador),
                    userInfo.substring(separador + 1));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CLOUDINARY_URL debe tener el formato cloudinary://API_KEY:API_SECRET@CLOUD_NAME");
        }
    }

    private String validarImagen(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La imagen no puede superar los 5 MB");
        }

        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            if (entrada == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen válida");
            }
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Solo se aceptan imágenes PNG o JPEG");
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                String formato = lector.getFormatName().toLowerCase();
                if (!formato.equals("png") && !formato.equals("jpeg")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Solo se aceptan imágenes PNG o JPEG");
                }
                if (lector.getWidth(0) > DIMENSION_MAXIMA || lector.getHeight(0) > DIMENSION_MAXIMA) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La imagen no puede superar 6000 × 6000 píxeles");
                }
                return formato;
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
        }
    }

    private HttpEntity<ByteArrayResource> archivo(byte[] contenido, String nombre, String formato) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(formato.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG);
        ByteArrayResource recurso = new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombre;
            }
        };
        return new HttpEntity<>(recurso, headers);
    }

    private String nombreSeguro(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return "producto";
        }
        return nombreArchivo.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String firmar(Map<String, String> parametros, String secreto) {
        String valor = parametros.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("") + secreto;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-1 no está disponible", e);
        }
    }

    private record RespuestaCloudinary(
            @com.fasterxml.jackson.annotation.JsonProperty("secure_url") String secureUrl) {}

    private record Credenciales(String cloudName, String apiKey, String apiSecret) {}
}
