package com.iglesia.caja.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.iglesia.caja.model.Categoria;

@Repository
public class CategoriaRepository {

	private static final RowMapper<Categoria> MAPPER = (rs, row) ->
			new Categoria(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"));
	private final JdbcTemplate jdbc;

	public CategoriaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Categoria> buscarTodas() {
		return jdbc.query("SELECT * FROM categories ORDER BY nombre COLLATE NOCASE", MAPPER);
	}

	public List<Categoria> buscarPorEvento(long eventoId) {
		return jdbc.query("""
				SELECT c.* FROM categories c
				JOIN event_categories ec ON ec.categoria_id = c.id
				WHERE ec.evento_id = ? ORDER BY c.nombre COLLATE NOCASE
				""", MAPPER, eventoId);
	}

	public Optional<Categoria> buscarPorNombre(String nombre) {
		return jdbc.query("SELECT * FROM categories WHERE nombre = ? COLLATE NOCASE", MAPPER, nombre)
				.stream().findFirst();
	}

	public Optional<Categoria> buscarPorId(long id) {
		return jdbc.query("SELECT * FROM categories WHERE id = ?", MAPPER, id).stream().findFirst();
	}

	public Categoria crear(String nombre, String descripcion) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO categories (nombre, descripcion) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, nombre);
			statement.setString(2, descripcion);
			return statement;
		}, keys);
		Number id = keys.getKey();
		if (id == null) {
			throw new IllegalStateException("No se pudo obtener el identificador de la categoría");
		}
		return buscarPorId(id.longValue()).orElseThrow();
	}
}
