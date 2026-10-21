package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resenas")
@Getter @Setter
@NoArgsConstructor
public class ResenaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "id_compra", nullable = false, length = 64)
    private String idCompra;

    @Column(name = "id_producto", nullable = false, length = 64)
    private String idProducto;

    @Column(name = "id_usuario", nullable = false, length = 64)
    private String idUsuario;

    @Column(nullable = false)
    private int calificacion;

    @Column(nullable = false, length = 500)
    private String comentario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "respuesta_vendedor", length = 500)
    private String respuestaVendedor;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}