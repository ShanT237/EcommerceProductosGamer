package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Cupon (CU-17).
 *  - Identidad: id único; el código se normaliza a mayúsculas.
 *  - Descuento porcentual (mayor a 0 y menor a 100), vigente hasta una fecha inclusive,
 *    con un máximo de usos.
 *  - Dueño opcional: con vendedorId el descuento solo aplica a productos de ese vendedor;
 *    con vendedorId null es un cupón global (aplica a cualquier producto).
 *  - Un cupón consume un uso por pedido (no uno por línea) y cada comprador puede usarlo una sola
 *    vez; esa regla vive en CrearPedidoUseCase porque depende del historial del comprador.
 */
@Getter
public class Cupon {

    private final UUID id;
    private final UUID vendedorId;   // null = cupón global
    private final String codigo;
    private final double porcentaje;
    private final LocalDate vence;
    private final int usosMaximos;
    private int usos;

    private Cupon(UUID id, UUID vendedorId, String codigo, double porcentaje, LocalDate vence, int usosMaximos) {
        this.id = id;
        this.vendedorId = vendedorId;
        this.codigo = codigo;
        this.porcentaje = porcentaje;
        this.vence = vence;
        this.usosMaximos = usosMaximos;
        this.usos = 0;
    }

    /** Cupón global (sin dueño). */
    public static Cupon crear(UUID id, String codigo, double porcentaje, LocalDate vence, int usosMaximos) {
        return crear(id, null, codigo, porcentaje, vence, usosMaximos);
    }

    /** Cupón de un vendedor; con vendedorId null equivale a un cupón global. */
    public static Cupon crear(UUID id, UUID vendedorId, String codigo, double porcentaje,
                              LocalDate vence, int usosMaximos) {
        Objects.requireNonNull(id, "El id del cupón es obligatorio");
        Objects.requireNonNull(vence, "La fecha de vencimiento es obligatoria");
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDominioException("El código del cupón no puede estar vacío");
        }
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new ReglaDominioException("El descuento debe ser mayor a 0 y menor que 100");
        }
        if (usosMaximos <= 0) {
            throw new ReglaDominioException("Los usos máximos deben ser positivos");
        }
        return new Cupon(id, vendedorId, codigo.trim().toUpperCase(), porcentaje, vence, usosMaximos);
    }

    /**
     * Solo para el adaptador de persistencia: "revive" un cupón que YA existía.
     * NO valida ni normaliza el código: eso ya ocurrió en crear(...).
     */
    public static Cupon reconstruir(UUID id, String codigo, double porcentaje, LocalDate vence,
                                    int usosMaximos, int usos, UUID vendedorId) {
        Cupon cupon = new Cupon(id, vendedorId, codigo, porcentaje, vence, usosMaximos);
        cupon.usos = usos;
        return cupon;
    }

    public void validarUso(LocalDate hoy) {
        if (hoy.isAfter(vence)) {
            throw new ReglaDominioException("El cupón está vencido");
        }
        if (usos >= usosMaximos) {
            throw new ReglaDominioException("El cupón ya no tiene usos disponibles");
        }
    }

    public void registrarUso() {
        if (usos >= usosMaximos) {
            throw new ReglaDominioException("El cupón ya no tiene usos disponibles");
        }
        this.usos++;
    }

    public int usosDisponibles() {
        return usosMaximos - usos;
    }

    /** Un cupón global aplica a todo producto; uno con dueño, solo a los productos de ese vendedor. */
    public boolean aplicaA(UUID vendedorDelProducto) {
        return vendedorId == null || vendedorId.equals(vendedorDelProducto);
    }

    public Precio aplicarA(Precio precio) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        return precio.aplicarDescuento(porcentaje);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cupon cupon)) return false;
        return id.equals(cupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
