package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.valueobject.Rol;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorCuentaActivaRepositorioTest {

    private UsuarioRepositoryEnMemoria usuarios;
    private VendedorRepositoryEnMemoria vendedores;
    private VerificadorCuentaActivaRepositorio verificador;

    @BeforeEach
    void setUp() {
        usuarios = new UsuarioRepositoryEnMemoria();
        vendedores = new VendedorRepositoryEnMemoria();
        verificador = new VerificadorCuentaActivaRepositorio(usuarios, vendedores);
    }

    @Test
    void compradorActivoEstaActivo() {
        UUID id = UUID.randomUUID();
        usuarios.guardar(Usuario.registrar(id, "Gamer", "gamer@test.com"));

        assertTrue(verificador.estaActiva(new SesionToken(id, Rol.COMPRADOR)));
    }

    @Test
    void compradorDadoDeBajaNoEstaActivo() {
        UUID id = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(id, "Gamer", "gamer@test.com");
        usuario.darDeBaja();
        usuarios.guardar(usuario);

        assertFalse(verificador.estaActiva(new SesionToken(id, Rol.COMPRADOR)));
    }

    @Test
    void compradorInexistenteSeTrataComoInactivo() {
        assertFalse(verificador.estaActiva(new SesionToken(UUID.randomUUID(), Rol.COMPRADOR)));
    }

    @Test
    void vendedorActivoEstaActivo() {
        UUID id = UUID.randomUUID();
        vendedores.guardar(Vendedor.registrar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123"));

        assertTrue(verificador.estaActiva(new SesionToken(id, Rol.VENDEDOR)));
    }

    @Test
    void vendedorDadoDeBajaNoEstaActivo() {
        UUID id = UUID.randomUUID();
        Vendedor vendedor = Vendedor.registrar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123");
        vendedor.darDeBaja();
        vendedores.guardar(vendedor);

        assertFalse(verificador.estaActiva(new SesionToken(id, Rol.VENDEDOR)));
    }

    @Test
    void vendedorInexistenteSeTrataComoInactivo() {
        assertFalse(verificador.estaActiva(new SesionToken(UUID.randomUUID(), Rol.VENDEDOR)));
    }

    @Test
    void elRolDelTokenDecideQueRepositorioSeConsulta() {
        // Un id de comprador activo no habilita una sesión con rol VENDEDOR (y viceversa).
        UUID id = UUID.randomUUID();
        usuarios.guardar(Usuario.registrar(id, "Gamer", "gamer@test.com"));

        assertFalse(verificador.estaActiva(new SesionToken(id, Rol.VENDEDOR)));
    }
}
