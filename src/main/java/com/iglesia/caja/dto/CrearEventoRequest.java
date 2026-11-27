package com.iglesia.caja.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearEventoRequest(
		@NotBlank @Size(max = 100) String nombre,
		@Size(max = 2000) String descripcion,
		@NotNull LocalDate fecha,
		@NotEmpty List<@NotBlank @Size(max = 80) String> categorias,
		@NotBlank String estado) {
}
