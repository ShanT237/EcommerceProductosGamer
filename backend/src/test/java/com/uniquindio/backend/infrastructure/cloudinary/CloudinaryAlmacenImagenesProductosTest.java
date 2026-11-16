package com.uniquindio.backend.infrastructure.cloudinary;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class CloudinaryAlmacenImagenesProductosTest {

    @Test
    void sinCredencialesRespondeServicioNoDisponibleSinIntentarSubir() throws IOException {
        CloudinaryAlmacenImagenesProductos almacen =
                new CloudinaryAlmacenImagenesProductos("", "", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacen.subir(imagenPng(), "producto.png"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void rechazaArchivosQueNoSonImagenes() {
        CloudinaryAlmacenImagenesProductos almacen =
                new CloudinaryAlmacenImagenesProductos("", "", "");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacen.subir(new byte[]{1, 2, 3}, "archivo.png"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void usaCloudinaryUrlParaFirmarYEnviarLaImagen() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(restTemplate).build();
        String imagenUrl = "https://res.cloudinary.com/demo/image/upload/producto.png";
        CloudinaryAlmacenImagenesProductos almacen = new CloudinaryAlmacenImagenesProductos(
                "", "", "", "cloudinary://123456789012345:secret987654321@demo", restTemplate);
        servidor.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"secure_url\":\"" + imagenUrl + "\"}", MediaType.APPLICATION_JSON));

        assertEquals(imagenUrl, almacen.subir(imagenPng(), "producto.png"));
        servidor.verify();
    }

    @Test
    void rechazaCloudinaryUrlConFormatoInvalidoSinMostrarCredenciales() throws IOException {
        CloudinaryAlmacenImagenesProductos almacen =
                new CloudinaryAlmacenImagenesProductos("", "", "", "no-es-una-url", new RestTemplate());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacen.subir(imagenPng(), "producto.png"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertEquals("CLOUDINARY_URL debe tener el formato cloudinary://API_KEY:API_SECRET@CLOUD_NAME",
                error.getReason());
    }

    private byte[] imagenPng() throws IOException {
        BufferedImage imagen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
