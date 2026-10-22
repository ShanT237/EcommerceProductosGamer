package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vendedores")
@Getter @Setter
@NoArgsConstructor
public class VendedorEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoVendedor tipo;

    @Column(nullable = false, unique = true, length = 30)
    private String documento;

    @Column(nullable = false)
    private boolean activo;

    // Nullable: los vendedores creados sin credenciales no tienen contraseña.
    @Column(name = "contrasena_hash", length = 255)
    private String contrasenaHash;
}