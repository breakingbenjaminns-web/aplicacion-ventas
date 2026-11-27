package com.iglesia.caja.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Evento(
		Long id,
		String nombre,
		LocalDate fecha,
		EstadoEvento estado,
		LocalDateTime fechaCreacion,
		List<String> categorias,
		String estadoCaja) {
}
