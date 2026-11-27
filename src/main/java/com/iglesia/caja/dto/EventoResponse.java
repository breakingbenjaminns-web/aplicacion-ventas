package com.iglesia.caja.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.iglesia.caja.model.Evento;

public record EventoResponse(
		Long id,
		String nombre,
		LocalDate fecha,
		String estado,
		LocalDateTime fechaCreacion,
		List<String> categorias,
		String caja) {

	public static EventoResponse desde(Evento evento) {
		return new EventoResponse(
				evento.id(),
				evento.nombre(),
				evento.fecha(),
				evento.estado().getValor(),
				evento.fechaCreacion(),
				evento.categorias(),
				"abierta".equals(evento.estadoCaja()) ? "Abierta" : "Cerrada");
	}
}
