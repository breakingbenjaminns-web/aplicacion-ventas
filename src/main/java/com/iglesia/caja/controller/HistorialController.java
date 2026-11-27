package com.iglesia.caja.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.caja.model.HistorialEvento;
import com.iglesia.caja.service.TicketService;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {
	private final TicketService service;

	public HistorialController(TicketService service) {
		this.service = service;
	}

	@GetMapping
	public HistorialEvento consultar(@RequestParam long eventoId) {
		return service.historial(eventoId);
	}
}
