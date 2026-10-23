package com.uniquindio.backend.application.port.out;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;

import java.util.List;
import java.util.UUID;

public interface ReporteMasVendidosPort {

    List<ProductoVendidoResponse> obtener(UUID vendedorIdOpcional, int top);
}
