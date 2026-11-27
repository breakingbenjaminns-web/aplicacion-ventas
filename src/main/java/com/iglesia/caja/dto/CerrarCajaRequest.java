package com.iglesia.caja.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CerrarCajaRequest(
		@NotNull @DecimalMin("0") @DecimalMax("9007199254740991") @Digits(integer = 16, fraction = 0)
		BigDecimal efectivoContado) {
}
