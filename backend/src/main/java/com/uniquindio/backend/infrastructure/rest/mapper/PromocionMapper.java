package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.PromocionResponse;
import com.uniquindio.backend.application.dto.response.PromocionVendedorResponse;
import com.uniquindio.backend.application.usecase.ConsultarPromocionesDelVendedorUseCase;
import com.uniquindio.backend.domain.entity.Promocion;
import org.springframework.stereotype.Component;

@Component
public class PromocionMapper {

    public PromocionResponse toResponse(Promocion promocion) {
        return PromocionResponse.desde(promocion);
    }

    public PromocionVendedorResponse toVendedorResponse(
            ConsultarPromocionesDelVendedorUseCase.PromocionDeProducto item) {
        Promocion p = item.promocion();
        return new PromocionVendedorResponse(p.getId(), p.getProductoId(), item.productoNombre(),
                p.getPorcentaje(), p.getDesde(), p.getHasta());
    }
}