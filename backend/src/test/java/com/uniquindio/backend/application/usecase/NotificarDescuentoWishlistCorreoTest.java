package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.CorreoNotificaciones;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** El descuento de un producto de la wishlist también llega por correo al comprador. */
class NotificarDescuentoWishlistCorreoTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private WishlistRepositoryEnMemoria wishlistRepository;
    private NotificacionRepositoryEnMemoria notificacionRepository;
    private List<String[]> correos;
    private UUID vendedorId;
    private Producto producto;
    private Usuario ana;
    private Usuario luis;
    private UsuarioRepositoryEnMemoria usuarios;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        wishlistRepository = new WishlistRepositoryEnMemoria();
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        correos = new ArrayList<>();

        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);

        usuarios = usuariosConDosCompradores();
        for (Usuario u : List.of(ana, luis)) {
            Wishlist w = Wishlist.crear(UUID.randomUUID(), u.getId());
            w.agregarProducto(producto.getId());
            wishlistRepository.guardar(w);
        }
    }

    private UsuarioRepositoryEnMemoria usuariosConDosCompradores() {
        UsuarioRepositoryEnMemoria repo = new UsuarioRepositoryEnMemoria();
        ana = Usuario.registrar(UUID.randomUUID(), "Ana", "ana@test.com");
        luis = Usuario.registrar(UUID.randomUUID(), "Luis", "luis@test.com");
        repo.guardar(ana);
        repo.guardar(luis);
        return repo;
    }

    private AplicarDescuentoUseCase conCorreo() {
        CorreoNotificaciones correo = new CorreoNotificaciones(
                (d, a, h) -> correos.add(new String[]{d, a, h}),
                usuarios, new VendedorRepositoryEnMemoria(), "https://tienda.test", true);
        return new AplicarDescuentoUseCase(productoRepository,
                new NotificarDescuentoWishlistUseCase(wishlistRepository, productoRepository,
                        notificacionRepository, correo));
    }

    @Test
    void cadaCompradorDeLaWishlistRecibeUnCorreoDeDescuento() {
        conCorreo().ejecutar(producto.getId(), vendedorId, 10);

        assertEquals(2, correos.size());
        assertEquals(List.of("ana@test.com", "luis@test.com"),
                correos.stream().map(c -> c[0]).sorted().toList());
        correos.forEach(c -> assertTrue(c[1].contains("Bajó el precio")));
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(ana.getId()).size());
    }

    @Test
    void sinCorreoInyectadoSoloSeGuardanLasNotificaciones() {
        new AplicarDescuentoUseCase(productoRepository,
                new NotificarDescuentoWishlistUseCase(wishlistRepository, productoRepository, notificacionRepository))
                .ejecutar(producto.getId(), vendedorId, 10);

        assertTrue(correos.isEmpty());
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(ana.getId()).size());
    }
}
