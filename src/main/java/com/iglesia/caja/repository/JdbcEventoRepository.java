package com.iglesia.caja.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.iglesia.caja.model.EstadoEvento;
import com.iglesia.caja.model.Evento;

@Repository
public class JdbcEventoRepository implements EventoRepository {

	private static final RowMapper<Evento> EVENTO_ROW_MAPPER = (rs, rowNum) -> new Evento(
			rs.getLong("id"),
			rs.getString("nombre"),
			rs.getString("fecha") == null ? null : LocalDate.parse(rs.getString("fecha")),
			EstadoEvento.desdeValor(rs.getString("estado")),
			Timestamp.valueOf(rs.getString("fecha_creacion")).toLocalDateTime(),
			List.of(),
			rs.getString("estado_caja"));

	private final JdbcTemplate jdbcTemplate;

	public JdbcEventoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Evento crear(
			String nombre,
			String descripcion,
			LocalDate fecha,
			List<String> categorias,
			EstadoEvento estado) {
		String sql = """
				INSERT INTO events (nombre, descripcion, fecha, estado)
				VALUES (?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, nombre);
			statement.setString(2, descripcion);
			statement.setString(3, fecha.toString());
			statement.setString(4, estado.getValor());
			return statement;
		}, keyHolder);

		Number id = keyHolder.getKey();
		if (id == null) {
			throw new IllegalStateException("No se pudo obtener el identificador del evento creado");
		}
		guardarCategorias(id.longValue(), categorias);
		return buscarPorId(id.longValue())
				.orElseThrow(() -> new IllegalStateException("No se encontró el evento recién creado"));
	}

	@Override
	public boolean actualizar(
			long id,
			String nombre,
			String descripcion,
			LocalDate fecha,
			List<String> categorias,
			EstadoEvento estado) {
		int actualizados = jdbcTemplate.update("""
				UPDATE events
				SET nombre = ?, descripcion = ?, fecha = ?, estado = ?
				WHERE id = ?
				""",
				nombre,
				descripcion,
				fecha.toString(),
				estado.getValor(),
				id);
		if (actualizados != 1) {
			return false;
		}
		List<String> existentes = jdbcTemplate.queryForList("""
				SELECT c.nombre FROM categories c
				JOIN event_categories ec ON ec.categoria_id = c.id
				WHERE ec.evento_id = ?
				""", String.class, id);
		for (String nombreExistente : existentes) {
			if (categorias.stream().noneMatch(nombreExistente::equalsIgnoreCase)) {
				jdbcTemplate.update("""
						DELETE FROM event_categories
						WHERE evento_id = ? AND categoria_id =
							(SELECT id FROM categories WHERE nombre = ? COLLATE NOCASE)
						""", id, nombreExistente);
			}
		}
		guardarCategorias(id, categorias);
		return true;
	}

	@Override
	public List<Evento> buscarTodos() {
		return jdbcTemplate.query("""
				SELECT e.*, COALESCE(b.estado, 'cerrada') AS estado_caja
				FROM events e
				LEFT JOIN box b ON b.evento_id = e.id
				ORDER BY e.id DESC
				""", EVENTO_ROW_MAPPER).stream()
				.map(this::cargarCategorias)
				.toList();
	}

	@Override
	public Optional<Evento> buscarPorId(long id) {
		return jdbcTemplate.query("""
				SELECT e.*, COALESCE(b.estado, 'cerrada') AS estado_caja
				FROM events e
				LEFT JOIN box b ON b.evento_id = e.id
				WHERE e.id = ?
				""", EVENTO_ROW_MAPPER, id)
				.stream()
				.map(this::cargarCategorias)
				.findFirst();
	}

	@Override
	public boolean actualizarEstado(long id, EstadoEvento estado) {
		return jdbcTemplate.update("UPDATE events SET estado = ? WHERE id = ?", estado.getValor(), id) == 1;
	}

	private void guardarCategorias(long eventoId, List<String> categorias) {
		for (String categoria : categorias.stream().map(String::trim).distinct().toList()) {
			jdbcTemplate.update("""
					INSERT OR IGNORE INTO categories (nombre)
					VALUES (?)
					""", categoria);
			Long categoriaId = jdbcTemplate.queryForObject(
					"SELECT id FROM categories WHERE nombre = ? COLLATE NOCASE",
					Long.class,
					categoria);
			if (categoriaId == null) {
				throw new IllegalStateException("No se pudo obtener el identificador de la categoría");
			}
			jdbcTemplate.update("""
					INSERT OR IGNORE INTO event_categories (evento_id, categoria_id)
					VALUES (?, ?)
					""", eventoId, categoriaId);
		}
	}

	private Evento cargarCategorias(Evento evento) {
		List<String> categorias = jdbcTemplate.queryForList("""
				SELECT c.nombre
				FROM categories c
				JOIN event_categories ec ON ec.categoria_id = c.id
				WHERE ec.evento_id = ?
				ORDER BY c.nombre COLLATE NOCASE
				""", String.class, evento.id());
		return new Evento(
				evento.id(),
				evento.nombre(),
				evento.fecha(),
				evento.estado(),
				evento.fechaCreacion(),
				categorias,
				evento.estadoCaja());
	}
}
