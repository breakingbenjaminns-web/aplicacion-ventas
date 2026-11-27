package com.iglesia.caja.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
		@NotBlank @Size(max = 100) String nombre,
		@Size(max = 2000) String descripcion,
		@NotNull @DecimalMin("0") @DecimalMax("9007199254740991") @Digits(integer = 16, fraction = 0)
		BigDecimal precio,
		@DecimalMin("0") @DecimalMax("9007199254740991") @Digits(integer = 16, fraction = 0)
		BigDecimal stock,
		@NotNull @Min(1) Long categoriaId,
		@NotNull @Min(1) Long eventoId) {
}
