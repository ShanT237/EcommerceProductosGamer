package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.port.out.AlmacenImagenesProductos;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubirImagenProductoUseCase {

    private final ProductoRepository productoRepository;
    private final AlmacenImagenesProductos almacenImagenes;

    @Transactional
    public String ejecutar(UUID productoId, UUID vendedorId, byte[] contenido, String nombreArchivo) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);
        producto.asegurarNoEliminado();

        String imagenUrl = almacenImagenes.subir(contenido, nombreArchivo);
        if (imagenUrl == null || !imagenUrl.startsWith("https://res.cloudinary.com/")) {
            throw new ReglaDominioException("El servicio de imágenes devolvió una URL inválida");
        }
        producto.asignarImagenUrl(imagenUrl);
        productoRepository.guardar(producto);
        return imagenUrl;
    }
}
