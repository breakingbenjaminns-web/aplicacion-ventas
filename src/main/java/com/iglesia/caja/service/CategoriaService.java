package com.iglesia.caja.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.dto.CategoriaRequest;
import com.iglesia.caja.model.Categoria;
import com.iglesia.caja.repository.CategoriaRepository;
import com.iglesia.caja.repository.EventoRepository;

@Service
public class CategoriaService {
	private final CategoriaRepository categorias;
	private final EventoRepository eventos;
	private final AuditoriaService auditoria;

	public CategoriaService(CategoriaRepository categorias, EventoRepository eventos, AuditoriaService auditoria) {
		this.categorias = categorias;
		this.eventos = eventos;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	public List<Categoria> listar(Long eventoId) {
		if (eventoId == null) {
			return categorias.buscarTodas();
		}
		if (eventos.buscarPorId(eventoId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
		}
		return categorias.buscarPorEvento(eventoId);
	}

	@Transactional(readOnly = true)
	public Categoria buscar(long id) {
		return categorias.buscarPorId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
	}

	@Transactional
	public Categoria crear(CategoriaRequest request) {
		String nombre = request.nombre().trim();
		if (categorias.buscarPorNombre(nombre).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
		}
		Categoria categoria = categorias.crear(nombre, request.descripcion());
		auditoria.registrar("crear", "categoria", categoria.id());
		return categoria;
	}
}
