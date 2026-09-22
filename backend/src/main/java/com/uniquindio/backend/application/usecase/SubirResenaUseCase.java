package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ResenaRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.valueobject.Calificacion;

import java.util.UUID;

/**
 * Caso de uso: SubirResena.
 * Recibe la intención de un usuario de crear una reseña sobre una compra previa.
 * Coordina los repositorios para obtener Usuario y Compra, y delega en el dominio
 * las reglas: validar reseña, marcar compra como reseñada y acumular puntos.
 */
public class SubirResenaUseCase {

    private final CompraRepository compraRepository;
    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final int PUNTOS_POR_RESENA = 10;

    public SubirResenaUseCase(CompraRepository compraRepository,
                              ResenaRepository resenaRepository,
                              UsuarioRepository usuarioRepository) {
        this.compraRepository = compraRepository;
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Resena ejecutar(String idResena, UUID idCompra, UUID idUsuario, int valorCalificacion, String comentario) {
        // 1. Obtener la compra y el usuario
        Compra compra = compraRepository.obtenerPorId(idCompra)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));

        if (!compra.getUsuarioId().equals(idUsuario)) {
            throw new ReglaDominioException("La compra no pertenece a este usuario");
        }

        Usuario usuario = usuarioRepository.obtenerPorId(idUsuario)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));

        // 2. Crear el VO y la entidad (El dominio valida el contenido)
        Calificacion calificacion = new Calificacion(valorCalificacion);

        // Asumimos que si la compra existe, está completada a efectos de este modelo base
        boolean compraCompletada = true;

        Resena resena = Resena.crear(
                idResena,
                idCompra.toString(),
                compra.getProductoId().toString(),
                idUsuario.toString(),
                compraCompletada,
                calificacion,
                comentario
        );

        // 3. Invocar comportamiento de las demás entidades involucradas (Reglas 2 y 6)
        compra.registrarResena();
        int puntosAAsignar = compra.asignarPuntos(PUNTOS_POR_RESENA);
        usuario.acumularPuntos(puntosAAsignar);

        // 4. Persistir cambios en todos los agregados
        resenaRepository.guardar(resena);
        compraRepository.guardar(compra);
        usuarioRepository.guardar(usuario);

        return resena;
    }
}