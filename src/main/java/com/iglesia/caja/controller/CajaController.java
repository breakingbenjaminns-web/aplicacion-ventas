package com.iglesia.caja.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.caja.dto.AbrirCajaRequest;
import com.iglesia.caja.dto.CerrarCajaRequest;
import com.iglesia.caja.model.Caja;
import com.iglesia.caja.service.CajaService;

@RestController
@RequestMapping("/api/cajas/evento/{eventoId}")
public class CajaController {
	private final CajaService service;

	public CajaController(CajaService service) {
		this.service = service;
	}

	@GetMapping
	public Caja consultar(@PathVariable long eventoId) {
		return service.consultar(eventoId);
	}

	@PostMapping("/abrir")
	public Caja abrir(@PathVariable long eventoId, @Valid @RequestBody AbrirCajaRequest request) {
		return service.abrir(eventoId, request);
	}

	@PostMapping("/cerrar")
	public Caja cerrar(@PathVariable long eventoId, @Valid @RequestBody CerrarCajaRequest request) {
		return service.cerrar(eventoId, request.efectivoContado().longValueExact());
	}

	@PostMapping("/reabrir")
	public Caja reabrir(@PathVariable long eventoId) {
		return service.reabrir(eventoId);
	}
}
