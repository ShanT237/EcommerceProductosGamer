package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.RealizarCompraResponse;
import com.uniquindio.backend.application.usecase.ConsultarHistorialComprasUseCase;
import com.uniquindio.backend.infrastructure.rest.mapper.CompraMapper;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class ConsultaComprasController {

    private final ConsultarHistorialComprasUseCase consultarHistorialComprasUseCase;
    private final CompraMapper mapper;

    // CU-31 Consultar Historial de Compras del usuario autenticado
    @GetMapping
    public List<RealizarCompraResponse> listarPorUsuario(@AuthenticationPrincipal UUID usuarioId) {
        return consultarHistorialComprasUseCase.ejecutar(usuarioId).stream()
                .map(mapper::toRealizarResponse)
                .toList();
    }
}