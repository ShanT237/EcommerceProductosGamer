package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContrasenaTest {

    @Test
    void aceptaContrasenaSegura() {
        assertDoesNotThrow(() -> Contrasena.validarFortaleza("Gamer2026"));
    }

    @Test
    void rechazaContrasenaNulaCortaSinMayusculaOSinNumero() {
        assertThrows(ReglaDominioException.class, () -> Contrasena.validarFortaleza(null));
        assertThrows(ReglaDominioException.class, () -> Contrasena.validarFortaleza("Ab1"));
        assertThrows(ReglaDominioException.class, () -> Contrasena.validarFortaleza("gamer2026"));
        assertThrows(ReglaDominioException.class, () -> Contrasena.validarFortaleza("GamerGamer"));
    }
}
