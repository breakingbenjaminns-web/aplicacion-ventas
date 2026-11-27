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

import com.iglesia.caja.dto.CategoriaRequest;
import com.iglesia.caja.model.Categoria;
import com.iglesia.caja.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
	private final CategoriaService service;

	public CategoriaController(CategoriaService service) {
		this.service = service;
	}

	@GetMapping
	public List<Categoria> listar(@RequestParam(required = false) Long eventoId) {
		return service.listar(eventoId);
	}

	@GetMapping("/{id}")
	public Categoria buscar(@PathVariable long id) {
		return service.buscar(id);
	}

	@PostMapping
	public ResponseEntity<Categoria> crear(@Valid @RequestBody CategoriaRequest request) {
		Categoria categoria = service.crear(request);
		return ResponseEntity.created(URI.create("/api/categorias/" + categoria.id())).body(categoria);
	}
}
