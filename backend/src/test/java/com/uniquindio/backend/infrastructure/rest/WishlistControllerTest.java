package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.usecase.AnadirAWishlistUseCase;
import com.uniquindio.backend.application.usecase.ConsultarWishlistUseCase;
import com.uniquindio.backend.application.usecase.QuitarDeWishlistUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.config.SecurityConfig;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import com.uniquindio.backend.infrastructure.rest.mapper.WishlistMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static com.uniquindio.backend.infrastructure.rest.SesionTest.comprador;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.mockMvcConSeguridad;
import static com.uniquindio.backend.infrastructure.rest.SesionTest.vendedor;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@Import({SecurityConfig.class, WishlistMapper.class, ProductoMapper.class})
class WishlistControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc; // con Spring Security aplicado

    @MockitoBean
    private AnadirAWishlistUseCase anadirAWishlistUseCase;

    @MockitoBean
    private QuitarDeWishlistUseCase quitarDeWishlistUseCase;

    @MockitoBean
    private ConsultarWishlistUseCase consultarWishlistUseCase;

    @MockitoBean
    private EmisorToken emisorToken;

    @BeforeEach
    void configurarMockMvc() {
        mockMvc = mockMvcConSeguridad(contexto);
    }

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();

    private Wishlist wishlistCon(UUID producto) {
        Wishlist w = Wishlist.crear(UUID.randomUUID(), usuarioId);
        w.agregarProducto(producto);
        return w;
    }

    private Producto producto(String nombre) {
        return Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
    }

    private String cuerpoAnadir() {
        return """
        { "productoId": "%s" }
        """.formatted(productoId);
    }

    // ===================== listar =====================

    @Test
    void listarUsaElUsuarioDelToken() throws Exception {
        when(consultarWishlistUseCase.ejecutar(usuarioId))
                .thenReturn(List.of(producto("Mouse"), producto("Teclado")));

        mockMvc.perform(get("/api/wishlists").with(comprador(usuarioId)))
                .andExpect(status().isOk())                       // 200
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Mouse"))
                .andExpect(jsonPath("$[1].nombre").value("Teclado"));

        Mockito.verify(consultarWishlistUseCase).ejecutar(usuarioId);
    }

    @Test
    void listarSinFavoritosDevuelveListaVacia() throws Exception {
        when(consultarWishlistUseCase.ejecutar(usuarioId)).thenReturn(List.of());

        mockMvc.perform(get("/api/wishlists").with(comprador(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listarSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/wishlists"))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(consultarWishlistUseCase);
    }

    @Test
    void unVendedorNoPuedeListarRetorna403() throws Exception {
        mockMvc.perform(get("/api/wishlists").with(vendedor(UUID.randomUUID())))
                .andExpect(status().isForbidden())                // 403
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verifyNoInteractions(consultarWishlistUseCase);
    }

    // ===================== añadir =====================

    @Test
    void anadirUsaElUsuarioDelToken() throws Exception {
        when(anadirAWishlistUseCase.ejecutar(usuarioId, productoId)).thenReturn(wishlistCon(productoId));

        mockMvc.perform(post("/api/wishlists")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnadir()))
                .andExpect(status().isCreated())                  // 201
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()));

        Mockito.verify(anadirAWishlistUseCase).ejecutar(usuarioId, productoId);
    }

    @Test
    void anadirSinProductoRetorna400() throws Exception {
        mockMvc.perform(post("/api/wishlists")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest());              // 400
    }

    @Test
    void anadirProductoRepetidoRetorna409() throws Exception {
        when(anadirAWishlistUseCase.ejecutar(usuarioId, productoId))
                .thenThrow(new ConflictoException("El producto ya se encuentra en la wishlist"));

        mockMvc.perform(post("/api/wishlists")
                        .with(comprador(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAnadir()))
                .andExpect(status().isConflict())                 // 409
                .andExpect(jsonPath("$.status").value(409));
    }

    // ===================== quitar =====================

    @Test
    void quitarUsaElUsuarioDelToken() throws Exception {
        when(quitarDeWishlistUseCase.ejecutar(usuarioId, productoId))
                .thenReturn(Wishlist.crear(UUID.randomUUID(), usuarioId));

        mockMvc.perform(delete("/api/wishlists/{productoId}", productoId)
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk());                      // 200

        Mockito.verify(quitarDeWishlistUseCase).ejecutar(usuarioId, productoId);
    }

    @Test
    void quitarIgnoraUnUsuarioIdEnLaUrl() throws Exception {
        UUID ajeno = UUID.randomUUID();
        when(quitarDeWishlistUseCase.ejecutar(usuarioId, productoId))
                .thenReturn(Wishlist.crear(UUID.randomUUID(), usuarioId));

        mockMvc.perform(delete("/api/wishlists/{productoId}", productoId)
                        .param("usuarioId", ajeno.toString())
                        .with(comprador(usuarioId)))
                .andExpect(status().isOk());

        Mockito.verify(quitarDeWishlistUseCase, Mockito.never()).ejecutar(ajeno, productoId);
    }

    @Test
    void sinTokenRetorna401EnJson() throws Exception {
        mockMvc.perform(delete("/api/wishlists/{productoId}", productoId))
                .andExpect(status().isUnauthorized())             // 401
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
