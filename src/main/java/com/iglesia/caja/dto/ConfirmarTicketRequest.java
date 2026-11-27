package com.iglesia.caja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmarTicketRequest(
		@NotBlank @Pattern(regexp = "efectivo|transferencia|deuda") String metodoPago,
		@Size(max = 100) String deudor) {
}
