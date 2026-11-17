package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.*;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.valueobject.Precio;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProductoMapper {

    public PublicarProductoResponse toPublicarResponse(Producto producto) {
        return new PublicarProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getGama(),
                producto.getExclusividad(),
                producto.getImagenUrl(),
                producto.getTipo()
        );
    }

    public ModificarProductoResponse toModificarResponse(Producto producto) {
        return new ModificarProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getPrecio()
        );
    }

    public ActualizarStockProductoResponse toStockResponse(Producto producto) {
        return new ActualizarStockProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getStock()
        );
    }

    public EditarInformacionProductoResponse toEditarInformacionResponse(Producto producto) {
        return new EditarInformacionProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getDescripcion()
        );
    }

    public CambiarVisibilidadProductoResponse toVisibilidadResponse(Producto producto) {
        return new CambiarVisibilidadProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.isPausado()
        );
    }

    public BuscarProductoResponse toBuscarResponse(Producto producto) {
        return toBuscarResponse(producto, null);
    }

    /**
     * @param promocion promoción de temporada vigente hoy del producto, o {@code null} si no tiene.
     *                  Se aplica sobre el precio actual (ya con el descuento inmediato, si lo hay),
     *                  igual que al comprar.
     */
    public BuscarProductoResponse toBuscarResponse(Producto producto, Promocion promocion) {
        Double precioPromocional = null;
        Double porcentajePromocion = null;
        LocalDate promocionHasta = null;
        if (promocion != null) {
            precioPromocional = promocion.aplicarA(new Precio(producto.getPrecio())).valor();
            porcentajePromocion = promocion.getPorcentaje();
            promocionHasta = promocion.getHasta();
        }
        return new BuscarProductoResponse(
                producto.getId(),
                producto.getVendedorId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getGama(),
                producto.getExclusividad(),
                producto.getCategoria(),
                producto.getPrecioOriginal(),
                producto.getPorcentajeDescuento(),
                precioPromocional,
                porcentajePromocion,
                promocionHasta,
                producto.getImagenUrl(),
                producto.getTipo()
        );
    }

    /** Listado del propio vendedor: lo mismo que el catálogo, más el estado `pausado`. */
    public MiProductoResponse toMiProductoResponse(Producto producto, Promocion promocion) {
        BuscarProductoResponse b = toBuscarResponse(producto, promocion);
        return new MiProductoResponse(
                b.id(), b.vendedorId(), b.nombre(), b.descripcion(), b.precio(), b.stock(),
                b.gama(), b.exclusividad(), b.categoria(), b.precioOriginal(), b.porcentajeDescuento(),
                b.precioPromocional(), b.porcentajePromocion(), b.promocionHasta(),
                producto.isPausado(), producto.getImagenUrl(), b.tipo()
        );
    }

    public ProductoStockBajoResponse toStockBajoResponse(Producto producto) {
        return ProductoStockBajoResponse.desde(producto);
    }
}
