package com.uniquindio.backend.domain.service;

import com.uniquindio.backend.domain.valueobject.Rol;

import java.util.UUID;

/**
 * Datos de sesión que viajan dentro de un token válido: quién es y con qué rol.
 */
public record SesionToken(UUID usuarioId, Rol rol) {
}