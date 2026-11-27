package com.iglesia.caja.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(
		@NotNull @Min(1) Long eventoId,
		@Size(max = 500) String detalle,
		@NotEmpty List<@NotNull @Valid Linea> items) {
	public record Linea(
			@NotNull @Min(1) Long productoId,
			@NotNull @DecimalMin("1") @DecimalMax("9007199254740991") @Digits(integer = 16, fraction = 0)
			BigDecimal cantidad) {
	}
}
