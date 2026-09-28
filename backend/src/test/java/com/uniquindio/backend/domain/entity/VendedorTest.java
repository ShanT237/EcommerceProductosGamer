package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VendedorTest {

    private Vendedor crearVendedorValido() {
        return new Vendedor(UUID.randomUUID(), "GamerZone SAS", "ventas@gamerzone.com",
                TipoVendedor.EMPRESA, "900123456-7");
    }

    @Test
    @DisplayName("Crea un vendedor válido, activo, y falla con datos inválidos")
    void creaVendedorValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        Vendedor vendedor = new Vendedor(id, "Juan Pérez", "juan@example.com",
                TipoVendedor.PERSONA_NATURAL, "1094123456");

        assertEquals(id, vendedor.getId());
        assertEquals("Juan Pérez", vendedor.getNombre());
        assertEquals("juan@example.com", vendedor.getCorreo());
        assertEquals(TipoVendedor.PERSONA_NATURAL, vendedor.getTipo());
        assertEquals("1094123456", vendedor.getDocumento());
        assertTrue(vendedor.isActivo());
        assertFalse(vendedor.esEmpresa());

        assertThrows(NullPointerException.class,
                () -> new Vendedor(null, "Juan", "juan@example.com", TipoVendedor.PERSONA_NATURAL, "123"));
        assertThrows(NullPointerException.class,
                () -> new Vendedor(id, "Juan", "juan@example.com", null, "123"));
        assertThrows(ReglaDominioException.class,
                () -> new Vendedor(id, "", "juan@example.com", TipoVendedor.PERSONA_NATURAL, "123"));
        assertThrows(ReglaDominioException.class,
                () -> new Vendedor(id, "Juan", "correo-invalido.com", TipoVendedor.PERSONA_NATURAL, "123"));
        assertThrows(ReglaDominioException.class,
                () -> new Vendedor(id, "Juan", null, TipoVendedor.PERSONA_NATURAL, "123"));
        assertThrows(ReglaDominioException.class,
                () -> new Vendedor(id, "Juan", "juan@example.com", TipoVendedor.PERSONA_NATURAL, "  "));
    }

    @Test
    @DisplayName("Un vendedor de tipo EMPRESA se reconoce como empresa")
    void vendedorEmpresa() {
        assertTrue(crearVendedorValido().esEmpresa());
    }

    @Test
    @DisplayName("El mensaje de documento vacío indica qué documento corresponde al tipo")
    void mensajeDocumentoSegunTipo() {
        ReglaDominioException excepcion = assertThrows(ReglaDominioException.class,
                () -> new Vendedor(UUID.randomUUID(), "GamerZone", "a@b.com", TipoVendedor.EMPRESA, ""));

        assertTrue(excepcion.getMessage().contains("NIT"));
    }

    @Test
    @DisplayName("actualizarCorreo cambia el correo y rechaza correos inválidos")
    void actualizarCorreo() {
        Vendedor vendedor = crearVendedorValido();

        vendedor.actualizarCorreo("nuevo@gamerzone.com");
        assertEquals("nuevo@gamerzone.com", vendedor.getCorreo());

        assertThrows(ReglaDominioException.class, () -> vendedor.actualizarCorreo("sin-arroba"));
        assertThrows(ReglaDominioException.class, () -> vendedor.actualizarCorreo(null));
        assertEquals("nuevo@gamerzone.com", vendedor.getCorreo());
    }

    @Test
    @DisplayName("darDeBaja desactiva al vendedor y rechaza darlo de baja dos veces")
    void darDeBaja() {
        Vendedor vendedor = crearVendedorValido();

        vendedor.darDeBaja();

        assertFalse(vendedor.isActivo());
        assertThrows(ReglaDominioException.class, vendedor::darDeBaja);
    }

    @Test
    @DisplayName("Un vendedor activo puede publicar; uno dado de baja no")
    void validarPuedePublicar() {
        Vendedor vendedor = crearVendedorValido();

        assertDoesNotThrow(vendedor::validarPuedePublicar);

        vendedor.darDeBaja();

        assertThrows(ReglaDominioException.class, vendedor::validarPuedePublicar);
    }

    @Test
    @DisplayName("La identidad del vendedor depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Vendedor vendedor1 = new Vendedor(id, "Nombre A", "a@example.com",
                TipoVendedor.PERSONA_NATURAL, "111");
        Vendedor vendedor2 = new Vendedor(id, "Nombre B", "b@example.com",
                TipoVendedor.EMPRESA, "222");
        Vendedor vendedor3 = new Vendedor(UUID.randomUUID(), "Nombre A", "a@example.com",
                TipoVendedor.PERSONA_NATURAL, "111");

        assertEquals(vendedor1, vendedor2);
        assertEquals(vendedor1.hashCode(), vendedor2.hashCode());
        assertNotEquals(vendedor1, vendedor3);
    }
}