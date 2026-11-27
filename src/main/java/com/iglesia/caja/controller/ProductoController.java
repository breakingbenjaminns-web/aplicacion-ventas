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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.caja.dto.EstadoProductoRequest;
import com.iglesia.caja.dto.ProductoRequest;
import com.iglesia.caja.model.Producto;
import com.iglesia.caja.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
	private final ProductoService service;

	public ProductoController(ProductoService service) {
		this.service = service;
	}

	@GetMapping
	public List<Producto> listar(@RequestParam long eventoId) {
		return service.listar(eventoId);
	}

	@GetMapping("/{id}")
	public Producto buscar(@PathVariable long id) {
		return service.buscar(id);
	}

	@PostMapping
	public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest request) {
		Producto producto = service.crear(request);
		return ResponseEntity.created(URI.create("/api/productos/" + producto.id())).body(producto);
	}

	@PutMapping("/{id}")
	public Producto actualizar(@PathVariable long id, @Valid @RequestBody ProductoRequest request) {
		return service.actualizar(id, request);
	}

	@PatchMapping("/{id}/activo")
	public Producto cambiarActivo(@PathVariable long id, @Valid @RequestBody EstadoProductoRequest request) {
		return service.cambiarActivo(id, request.activo());
	}
}
