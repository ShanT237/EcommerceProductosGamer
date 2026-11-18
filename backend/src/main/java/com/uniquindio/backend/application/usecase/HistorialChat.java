package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MensajeChat;

import java.util.List;

/** Página de historial en orden cronológico ascendente; {@code hayMas} indica si quedan más en esa dirección. */
public record HistorialChat(List<MensajeChat> mensajes, boolean hayMas) {
}
