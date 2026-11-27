package com.iglesia.caja.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.caja.dto.CambiarEstadoEventoRequest;
import com.iglesia.caja.dto.CrearEventoRequest;
import com.iglesia.caja.dto.EventoResponse;
import com.iglesia.caja.service.EventoService;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService eventoService;

	public EventoController(EventoService eventoService) {
		this.eventoService = eventoService;
	}

	@PostMapping
	public ResponseEntity<EventoResponse> crear(@Valid @RequestBody CrearEventoRequest request) {
		EventoResponse evento = eventoService.crear(request);
		return ResponseEntity.created(URI.create("/api/eventos/" + evento.id())).body(evento);
	}

	@PutMapping("/{id}")
	public EventoResponse actualizar(@PathVariable long id, @Valid @RequestBody CrearEventoRequest request) {
		return eventoService.actualizar(id, request);
	}

	@GetMapping
	public List<EventoResponse> buscarTodos() {
		return eventoService.buscarTodos();
	}

	@GetMapping("/{id}")
	public EventoResponse buscarPorId(@PathVariable long id) {
		return eventoService.buscarPorId(id);
	}

	@PatchMapping("/{id}/estado")
	public EventoResponse cambiarEstado(
			@PathVariable long id,
			@Valid @RequestBody CambiarEstadoEventoRequest request) {
		return eventoService.cambiarEstado(id, request.estado());
	}
}
