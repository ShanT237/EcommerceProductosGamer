package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ResenaRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: SubirResena.
 * Orquesta la creación de una reseña: valida compra y usuario, delega en las entidades
 * y persiste los agregados involucrados.
 */
@Service
@RequiredArgsConstructor
public class SubirResenaUseCase {

    private final CompraRepository compraRepository;
    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    public Resena ejecutar(String idResena, UUID idCompra, UUID idUsuario, int valorCalificacion, String comentario) {
        Compra compra = compraRepository.obtenerPorId(idCompra)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(idUsuario);

        Usuario usuario = usuarioRepository.obtenerPorId(idUsuario)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));

        if (resenaRepository.existeResenaDeUsuarioYProducto(idUsuario.toString(), compra.getProductoId().toString())) {
            throw new ReglaDominioException("El usuario ya publicó una reseña para este producto");
        }

        Calificacion calificacion = new Calificacion(valorCalificacion);
        Resena resena = Resena.crear(
                idResena,
                idCompra.toString(),
                compra.getProductoId().toString(),
                idUsuario.toString(),
                compra.estaCompletada(),
                calificacion,
                comentario
        );

        compra.registrarResena();
        usuario.acumularPuntos(compra.asignarPuntosPorResena());

        resenaRepository.guardar(resena);
        compraRepository.guardar(compra);
        usuarioRepository.guardar(usuario);

        return resena;
    }
}
