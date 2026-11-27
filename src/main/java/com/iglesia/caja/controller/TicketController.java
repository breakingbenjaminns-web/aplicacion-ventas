package com.iglesia.caja.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.caja.dto.ConfirmarTicketRequest;
import com.iglesia.caja.dto.CrearTicketRequest;
import com.iglesia.caja.dto.AnularTicketRequest;
import com.iglesia.caja.model.Ticket;
import com.iglesia.caja.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
	private final TicketService service;

	public TicketController(TicketService service) {
		this.service = service;
	}

	@GetMapping
	public List<Ticket> listar(@RequestParam long eventoId) {
		return service.listar(eventoId);
	}

	@GetMapping("/{id}")
	public Ticket buscar(@PathVariable long id) {
		return service.buscar(id);
	}

	@PostMapping
	public ResponseEntity<Ticket> crear(@Valid @RequestBody CrearTicketRequest request) {
		Ticket ticket = service.crear(request);
		return ResponseEntity.created(URI.create("/api/tickets/" + ticket.id())).body(ticket);
	}

	@PostMapping("/{id}/confirmar")
	public Ticket confirmar(@PathVariable long id, @Valid @RequestBody ConfirmarTicketRequest request) {
		return service.confirmar(id, request);
	}

	@PostMapping("/{id}/anular")
	public Ticket anular(@PathVariable long id, @Valid @RequestBody AnularTicketRequest request) {
		return service.anular(id, request);
	}
}
