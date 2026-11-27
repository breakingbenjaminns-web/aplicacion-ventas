package com.iglesia.caja.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoEventoRequest(@NotBlank String estado) {
}
