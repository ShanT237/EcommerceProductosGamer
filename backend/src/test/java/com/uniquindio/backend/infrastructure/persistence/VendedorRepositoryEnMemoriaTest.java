package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VendedorRepositoryEnMemoriaTest {

    private final VendedorRepositoryEnMemoria repository = new VendedorRepositoryEnMemoria();

    private Vendedor guardar(String correo, String documento) {
        Vendedor v = Vendedor.registrar(UUID.randomUUID(), "GamerZone", correo,
                TipoVendedor.EMPRESA, documento);
        repository.guardar(v);
        return v;
    }

    @Test
    @DisplayName("obtenerPorCorreo encuentra al vendedor sin distinguir mayúsculas ni espacios")
    void encuentraPorCorreoSinDistinguirMayusculas() {
        Vendedor v = guardar("ventas@gamerzone.com", "900111");

        assertEquals(v, repository.obtenerPorCorreo("  VENTAS@gamerzone.com ").orElseThrow());
    }

    @Test
    @DisplayName("obtenerPorCorreo devuelve vacío si no existe o el correo es nulo")
    void noEncuentraCorreoInexistenteONulo() {
        guardar("ventas@gamerzone.com", "900111");

        assertTrue(repository.obtenerPorCorreo("otro@gamerzone.com").isEmpty());
        assertTrue(repository.obtenerPorCorreo(null).isEmpty());
    }

    @Test
    @DisplayName("existeConCorreo refleja si el correo ya está registrado")
    void existeConCorreo() {
        guardar("ventas@gamerzone.com", "900111");

        assertTrue(repository.existeConCorreo("Ventas@GamerZone.com"));
        assertFalse(repository.existeConCorreo("otro@gamerzone.com"));
        assertFalse(repository.existeConCorreo(null));
    }
}