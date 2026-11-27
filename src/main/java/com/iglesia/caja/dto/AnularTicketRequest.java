package com.iglesia.caja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularTicketRequest(
		@NotBlank @Size(max = 500) String motivo,
		Boolean devolucionTransferenciaConfirmada) {
}
