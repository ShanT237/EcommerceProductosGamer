package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepositoriosEnMemoriaTest {

    @Test
    @DisplayName("ProductoRepositoryEnMemoria guarda y devuelve el mismo producto por id")
    void guardaYRecuperaProductoPorId() {
        ProductoRepositoryEnMemoria repository = new ProductoRepositoryEnMemoria();
        Producto producto = new Producto(UUID.randomUUID(), "Mouse Gamer", "Mouse RGB 16000dpi",
                150000, 10, Gama.ALTA, Exclusividad.NORMAL);

        repository.guardar(producto);
        Optional<Producto> encontrado = repository.obtenerPorId(producto.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(producto, encontrado.get());
        assertTrue(repository.obtenerPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("CompraRepositoryEnMemoria detecta correctamente si un usuario ya compró un producto")
    void detectaCompraExistenteDeUnUsuario() {
        CompraRepositoryEnMemoria repository = new CompraRepositoryEnMemoria();
        UUID usuarioId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Compra compra = new Compra(UUID.randomUUID(), usuarioId, productoId,
                1, LocalDate.of(2026, 9, 22), new Precio(50000));

        assertFalse(repository.existeCompraDe(usuarioId, productoId));

        repository.guardar(compra);

        assertTrue(repository.existeCompraDe(usuarioId, productoId));
        assertFalse(repository.existeCompraDe(usuarioId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("UsuarioRepositoryEnMemoria guarda y devuelve el mismo usuario por id")
    void guardaYRecuperaUsuarioPorId() {
        UsuarioRepositoryEnMemoria repository = new UsuarioRepositoryEnMemoria();
        Usuario usuario = new Usuario(UUID.randomUUID(), "Jose Bedoya", "jose@correo.com");

        repository.guardar(usuario);
        Optional<Usuario> encontrado = repository.obtenerPorId(usuario.getId());

        assertTrue(encontrado.isPresent());
        assertEquals(usuario, encontrado.get());
    }

    @Test
    @DisplayName("CompraRepositoryEnMemoria sobreescribe la compra si se guarda dos veces con el mismo id")
    void guardarDosVecesSobreescribeLaMismaCompra() {
        CompraRepositoryEnMemoria repository = new CompraRepositoryEnMemoria();
        UUID id = UUID.randomUUID();
        Compra compraOriginal = new Compra(id, UUID.randomUUID(), UUID.randomUUID(),
                1, LocalDate.of(2026, 9, 22), new Precio(30000));
        Compra compraActualizada = new Compra(id, UUID.randomUUID(), UUID.randomUUID(),
                5, LocalDate.of(2026, 9, 22), new Precio(30000));

        repository.guardar(compraOriginal);
        repository.guardar(compraActualizada);

        Optional<Compra> encontrada = repository.obtenerPorId(id);
        assertTrue(encontrada.isPresent());
        assertEquals(5, encontrada.get().getCantidad());
    }
}