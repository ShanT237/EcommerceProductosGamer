package com.uniquindio.backend.application.service;

import com.uniquindio.backend.application.usecase.ResumenConversacionChat;
import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Completa una conversación con los datos que necesita quien la mira (título y contraparte). */
@Component
@RequiredArgsConstructor
public class ArmadorResumenChat {

    private static final String NOMBRE_POR_DEFECTO = "Usuario";

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;

    public ResumenConversacionChat armar(ConversacionChat c, UUID usuarioQueMira, long noLeidos) {
        return new ResumenConversacionChat(c, titulo(c), contraparteNombre(c, usuarioQueMira), noLeidos);
    }

    private String titulo(ConversacionChat c) {
        return switch (c.getTipo()) {
            case PRODUCTO -> productoRepository.obtenerPorId(c.getReferenciaId())
                    .map(Producto::getNombre)
                    .orElse("Producto");
            case ORDEN -> "Pedido #" + c.getReferenciaId().toString().substring(0, 8).toUpperCase();
        };
    }

    private String contraparteNombre(ConversacionChat c, UUID usuarioQueMira) {
        if (c.getCompradorId().equals(usuarioQueMira)) {
            return vendedorRepository.obtenerPorId(c.getVendedorId())
                    .map(Vendedor::getNombre)
                    .orElse(NOMBRE_POR_DEFECTO);
        }
        return usuarioRepository.obtenerPorId(c.getCompradorId())
                .map(Usuario::getNombre)
                .orElse(NOMBRE_POR_DEFECTO);
    }
}
