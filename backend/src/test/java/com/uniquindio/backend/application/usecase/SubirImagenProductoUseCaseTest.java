package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.port.out.AlmacenImagenesProductos;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SubirImagenProductoUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private AlmacenImagenesProductos almacenImagenes;

    private SubirImagenProductoUseCase useCase;
    private Producto producto;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        useCase = new SubirImagenProductoUseCase(productoRepository, almacenImagenes);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    void subeLaImagenYGuardaLaUrlEnElProducto() {
        String url = "https://res.cloudinary.com/demo/image/upload/producto.png";
        when(productoRepository.obtenerPorId(producto.getId())).thenReturn(Optional.of(producto));
        when(almacenImagenes.subir(any(byte[].class), eq("producto.png"))).thenReturn(url);

        String resultado = useCase.ejecutar(producto.getId(), vendedorId, new byte[]{1, 2, 3}, "producto.png");

        assertEquals(url, resultado);
        assertEquals(url, producto.getImagenUrl());
        verify(productoRepository).guardar(producto);
    }

    @Test
    void vendedorAjenoNoPuedeSubirImagenNiLlamarAlAlmacen() {
        when(productoRepository.obtenerPorId(producto.getId())).thenReturn(Optional.of(producto));

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), new byte[]{1}, "producto.png"));

        verify(almacenImagenes, never()).subir(any(byte[].class), eq("producto.png"));
        verify(productoRepository, never()).guardar(producto);
    }
}
