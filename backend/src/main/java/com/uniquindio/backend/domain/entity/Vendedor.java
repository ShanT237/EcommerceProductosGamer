package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Vendedor.
 *  - Identidad: dos vendedores son el mismo solo si comparten el mismo id.
 *  - Ciclo de vida: se registra, publica productos, puede cambiar su correo
 *    y se puede dar de baja (eliminación lógica).
 *  - TipoVendedor (persona natural o empresa) está dentro de su límite.
 *  - Es independiente de Usuario (comprador): los productos lo referencian por id.
 */
@Getter
public class Vendedor {

    private final UUID id;
    private final String nombre;
    private String correo;
    private final TipoVendedor tipo;
    private final String documento;
    private boolean activo;

    public Vendedor(UUID id, String nombre, String correo, TipoVendedor tipo, String documento) {
        this.id = Objects.requireNonNull(id, "El id del vendedor es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de vendedor es obligatorio");

        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del vendedor no puede estar vacío");
        }
        validarCorreo(correo);
        if (documento == null || documento.isBlank()) {
            throw new ReglaDominioException(
                    "El documento del vendedor (" + tipo.tipoDocumento() + ") no puede estar vacío");
        }

        this.nombre = nombre;
        this.correo = correo;
        this.documento = documento;
        this.activo = true;
    }

    public boolean esEmpresa() {
        return tipo.esEmpresa();
    }

    public void actualizarCorreo(String nuevoCorreo) {
        validarCorreo(nuevoCorreo);
        this.correo = nuevoCorreo;
    }

    /**
     * Baja lógica: el vendedor deja de poder publicar, pero se conserva
     * su historial de ventas.
     */
    public void darDeBaja() {
        if (!activo) {
            throw new ReglaDominioException("El vendedor ya está dado de baja");
        }
        this.activo = false;
    }

    /**
     * Quien publique un producto debe validar primero que el vendedor esté activo.
     */
    public void validarPuedePublicar() {
        if (!activo) {
            throw new ReglaDominioException("Un vendedor dado de baja no puede publicar productos");
        }
    }

    private static void validarCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            throw new ReglaDominioException("El correo del vendedor no es válido");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vendedor vendedor)) return false;
        return id.equals(vendedor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}