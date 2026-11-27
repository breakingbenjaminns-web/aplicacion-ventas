package com.iglesia.caja.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoProductoRequest(@NotNull Boolean activo) {
}
