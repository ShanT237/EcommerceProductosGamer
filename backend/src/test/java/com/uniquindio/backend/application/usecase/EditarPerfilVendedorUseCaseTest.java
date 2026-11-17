package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EditarPerfilVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private EditarPerfilVendedorUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        useCase = new EditarPerfilVendedorUseCase(vendedorRepository, usuarioRepository);
        id = UUID.randomUUID();
        vendedorRepository.guardar(Vendedor.registrar(id, "GamerZone", "ventas@gamerzone.com",
                TipoVendedor.EMPRESA, "900123"));
        vendedorRepository.guardar(Vendedor.registrar(UUID.randomUUID(), "Otra Tienda", "otra@tienda.com",
                TipoVendedor.PERSONA_NATURAL, "1094"));
        usuarioRepository.guardar(Usuario.registrar(UUID.randomUUID(), "Comprador", "comprador@test.com"));
    }

    private Vendedor vendedor() {
        return vendedorRepository.obtenerPorId(id).get();
    }

    @Test
    void cambiaNombreYCorreoYGuarda() {
        Vendedor v = useCase.ejecutar(id, "GamerZone Pro", "pro@gamerzone.com");

        assertEquals("GamerZone Pro", v.getNombre());
        assertEquals("pro@gamerzone.com", vendedor().getCorreo());
    }

    @Test
    void noCambiaTipoNiDocumento() {
        useCase.ejecutar(id, "GamerZone Pro", "pro@gamerzone.com");

        assertEquals(TipoVendedor.EMPRESA, vendedor().getTipo());
        assertEquals("900123", vendedor().getDocumento());
    }

    @Test
    void permiteConservarElPropioCorreo() {
        assertDoesNotThrow(() -> useCase.ejecutar(id, "Nuevo Nombre", "VENTAS@gamerzone.com"));
        assertEquals("Nuevo Nombre", vendedor().getNombre());
    }

    @Test
    void correoDeOtroVendedorSeRechazaYNoCambiaNada() {
        assertThrows(ConflictoException.class, () -> useCase.ejecutar(id, "Nuevo", "otra@tienda.com"));

        assertEquals("GamerZone", vendedor().getNombre());
        assertEquals("ventas@gamerzone.com", vendedor().getCorreo());
    }

    @Test
    void correoDeUnCompradorSeRechazaYNoCambiaNada() {
        assertThrows(ConflictoException.class, () -> useCase.ejecutar(id, "Nuevo", "comprador@test.com"));

        assertEquals("GamerZone", vendedor().getNombre());
        assertEquals("ventas@gamerzone.com", vendedor().getCorreo());
    }

    @Test
    void datosInvalidosNoCambianNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, " ", "nuevo@gamerzone.com"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Nuevo", "sin-arroba"));

        assertEquals("GamerZone", vendedor().getNombre());
        assertEquals("ventas@gamerzone.com", vendedor().getCorreo());
    }

    @Test
    void vendedorDadoDeBajaOInexistenteRechaza() {
        vendedor().darDeBaja();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Nuevo", "nuevo@gamerzone.com"));
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), "Nuevo", "nuevo@gamerzone.com"));
    }
}
