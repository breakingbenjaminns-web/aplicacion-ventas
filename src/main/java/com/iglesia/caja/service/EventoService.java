package com.iglesia.caja.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.dto.CrearEventoRequest;
import com.iglesia.caja.dto.EventoResponse;
import com.iglesia.caja.model.EstadoEvento;
import com.iglesia.caja.model.Evento;
import com.iglesia.caja.repository.EventoRepository;
import com.iglesia.caja.repository.ProductoRepository;

@Service
public class EventoService {

	private final EventoRepository eventoRepository;
	private final ProductoRepository productoRepository;
	private final AuditoriaService auditoria;

	public EventoService(EventoRepository eventoRepository, ProductoRepository productoRepository, AuditoriaService auditoria) {
		this.eventoRepository = eventoRepository;
		this.productoRepository = productoRepository;
		this.auditoria = auditoria;
	}

	@Transactional
	public EventoResponse crear(CrearEventoRequest request) {
		EstadoEvento estado = validarEstado(request.estado());
		validarCategorias(request.categorias());
		Evento evento = eventoRepository.crear(
				request.nombre().trim(),
				request.descripcion(),
				request.fecha(),
				normalizarCategorias(request.categorias()),
				estado);
		auditoria.registrar("crear", "evento", evento.id());
		return EventoResponse.desde(evento);
	}

	@Transactional
	public EventoResponse actualizar(long id, CrearEventoRequest request) {
		EstadoEvento estado = validarEstado(request.estado());
		validarCategorias(request.categorias());
		boolean categoriaEnUso = productoRepository.buscarPorEvento(id).stream()
				.anyMatch(producto -> request.categorias().stream()
						.map(String::trim).noneMatch(producto.categoria()::equalsIgnoreCase));
		if (categoriaEnUso) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"No se puede quitar una categoría que tiene productos asociados");
		}
		if (!eventoRepository.actualizar(
				id,
				request.nombre().trim(),
				request.descripcion(),
				request.fecha(),
				normalizarCategorias(request.categorias()),
				estado)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
		}
		auditoria.registrar("actualizar", "evento", id);
		return buscarPorId(id);
	}

	@Transactional(readOnly = true)
	public List<EventoResponse> buscarTodos() {
		return eventoRepository.buscarTodos().stream()
				.map(EventoResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public EventoResponse buscarPorId(long id) {
		return eventoRepository.buscarPorId(id)
				.map(EventoResponse::desde)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
	}

	@Transactional
	public EventoResponse cambiarEstado(long id, String valorEstado) {
		EstadoEvento estado = validarEstado(valorEstado);

		Evento evento = eventoRepository.buscarPorId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
		if (!eventoRepository.actualizarEstado(id, estado)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo actualizar el estado del evento");
		}
		auditoria.registrar("cambiar_estado", "evento", id);
		return EventoResponse.desde(new Evento(
				evento.id(),
				evento.nombre(),
				evento.fecha(),
				estado,
				evento.fechaCreacion(),
				evento.categorias(),
				evento.estadoCaja()));
	}

	private EstadoEvento validarEstado(String valorEstado) {
		try {
			return EstadoEvento.desdeValor(valorEstado);
		} catch (IllegalArgumentException excepcion) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, excepcion.getMessage(), excepcion);
		}
	}

	private void validarCategorias(List<String> categorias) {
		long categoriasUnicas = categorias.stream()
				.map(categoria -> categoria.trim().toLowerCase(Locale.ROOT))
				.distinct()
				.count();
		if (categoriasUnicas != categorias.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten categorías repetidas");
		}
	}

	private List<String> normalizarCategorias(List<String> categorias) {
		return categorias.stream().map(String::trim).toList();
	}
}
