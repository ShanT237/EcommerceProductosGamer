package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnadirAWishlistUseCaseTest {

    private UsuarioRepository usuarioRepository;
    private ProductoRepository productoRepository;
    private WishlistRepository wishlistRepository;
    private AnadirAWishlistUseCase useCase;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        wishlistRepository = new WishlistRepositoryEnMemoria();
        useCase = new AnadirAWishlistUseCase(usuarioRepository, productoRepository, wishlistRepository);

        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "gamer@test.com"));
    }

    private Producto guardarProducto() {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    @DisplayName("Reutiliza la wishlist existente del usuario al agregar un segundo producto")
    void reutilizaLaWishlistExistente() {
        Producto p1 = guardarProducto();
        Producto p2 = guardarProducto();

        Wishlist w1 = useCase.ejecutar(usuarioId, p1.getId());
        Wishlist w2 = useCase.ejecutar(usuarioId, p2.getId());

        assertEquals(w1.getId(), w2.getId());
        assertEquals(2, w2.getProductos().size());
    }

    @Test
    @DisplayName("Rechaza agregar el mismo producto dos veces (Regla 7)")
    void rechazaDuplicados() {
        Producto p = guardarProducto();
        useCase.ejecutar(usuarioId, p.getId());

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, p.getId()));
    }

    @Test
    @DisplayName("Rechaza agregar un producto eliminado")
    void rechazaProductoEliminado() {
        Producto p = guardarProducto();
        p.eliminar(false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, p.getId()));
    }
}