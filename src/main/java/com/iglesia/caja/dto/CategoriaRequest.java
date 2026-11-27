package com.iglesia.caja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
		@NotBlank @Size(max = 80) String nombre,
		@Size(max = 2000) String descripcion) {
}
