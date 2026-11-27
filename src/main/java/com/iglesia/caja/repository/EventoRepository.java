package com.iglesia.caja.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.iglesia.caja.model.EstadoEvento;
import com.iglesia.caja.model.Evento;

public interface EventoRepository {

	Evento crear(
			String nombre,
			String descripcion,
			LocalDate fecha,
			List<String> categorias,
			EstadoEvento estado);

	boolean actualizar(
			long id,
			String nombre,
			String descripcion,
			LocalDate fecha,
			List<String> categorias,
			EstadoEvento estado);

	List<Evento> buscarTodos();

	Optional<Evento> buscarPorId(long id);

	boolean actualizarEstado(long id, EstadoEvento estado);
}
