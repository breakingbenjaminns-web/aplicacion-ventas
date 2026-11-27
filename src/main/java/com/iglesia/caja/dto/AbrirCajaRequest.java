package com.iglesia.caja.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AbrirCajaRequest(@NotNull @Size(max = 100) List<@NotNull @Valid Aporte> aportes) {
	public record Aporte(
			@NotBlank @Size(max = 100) String responsable,
			@NotNull @DecimalMin(value = "0", inclusive = false)
			@DecimalMax("9007199254740991") @Digits(integer = 16, fraction = 0) BigDecimal importe) {
	}
}
