package com.iglesia.caja.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.dto.ProductoRequest;
import com.iglesia.caja.model.Producto;
import com.iglesia.caja.repository.EventoRepository;
import com.iglesia.caja.repository.ProductoRepository;

@Service
public class ProductoService {
	private final ProductoRepository productos;
	private final EventoRepository eventos;
	private final AuditoriaService auditoria;

	public ProductoService(ProductoRepository productos, EventoRepository eventos, AuditoriaService auditoria) {
		this.productos = productos;
		this.eventos = eventos;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	public List<Producto> listar(long eventoId) {
		if (eventos.buscarPorId(eventoId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
		}
		return productos.buscarPorEvento(eventoId);
	}

	@Transactional(readOnly = true)
	public Producto buscar(long id) {
		return productos.buscarPorId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
	}

	@Transactional
	public Producto crear(ProductoRequest request) {
		validarCategoria(request);
		long id = productos.crear(request);
		if (request.stock() != null) {
			productos.registrarStock(id, null, request.stock().longValueExact(), "entrada");
		}
		auditoria.registrar("crear", "producto", id);
		return buscar(id);
	}

	@Transactional
	public Producto actualizar(long id, ProductoRequest request) {
		Producto anterior = buscar(id);
		if (anterior.eventoId() != request.eventoId()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar el evento de un producto");
		}
		validarCategoria(request);
		if (!productos.actualizar(id, request)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
		}
		Long nuevoStock = request.stock() == null ? null : request.stock().longValueExact();
		if (!Objects.equals(anterior.stock(), nuevoStock)) {
			productos.registrarStock(id, anterior.stock(), nuevoStock, "ajuste");
		}
		auditoria.registrar("actualizar", "producto", id);
		return buscar(id);
	}

	@Transactional
	public Producto cambiarActivo(long id, boolean activo) {
		if (!productos.cambiarActivo(id, activo)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
		}
		auditoria.registrar(activo ? "activar" : "desactivar", "producto", id);
		return buscar(id);
	}

	private void validarCategoria(ProductoRequest request) {
		if (!productos.categoriaAsociada(request.eventoId(), request.categoriaId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La categoría debe estar asociada al evento");
		}
	}
}
