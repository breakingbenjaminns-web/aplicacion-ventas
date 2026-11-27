package com.iglesia.caja.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.iglesia.caja.dto.ProductoRequest;
import com.iglesia.caja.model.Producto;

@Repository
public class ProductoRepository {

	private static final String SELECT = """
			SELECT p.*, c.nombre AS categoria FROM products p
			JOIN categories c ON c.id = p.categoria_id
			""";
	private static final RowMapper<Producto> MAPPER = (rs, row) -> new Producto(
			rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"),
			rs.getLong("precio"), rs.getObject("stock") == null ? null : rs.getLong("stock"),
			rs.getLong("categoria_id"), rs.getString("categoria"), rs.getLong("evento_id"),
			rs.getBoolean("activo"), Timestamp.valueOf(rs.getString("fecha_creacion")).toLocalDateTime());
	private final JdbcTemplate jdbc;

	public ProductoRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public boolean categoriaAsociada(long eventoId, long categoriaId) {
		Integer cantidad = jdbc.queryForObject(
				"SELECT COUNT(*) FROM event_categories WHERE evento_id = ? AND categoria_id = ?",
				Integer.class, eventoId, categoriaId);
		return cantidad != null && cantidad > 0;
	}

	public List<Producto> buscarPorEvento(long eventoId) {
		return jdbc.query(SELECT + " WHERE p.evento_id = ? ORDER BY p.id DESC", MAPPER, eventoId);
	}

	public Optional<Producto> buscarPorId(long id) {
		return jdbc.query(SELECT + " WHERE p.id = ?", MAPPER, id).stream().findFirst();
	}

	public long crear(ProductoRequest request) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO products (nombre, descripcion, precio, stock, categoria_id, evento_id)
					VALUES (?, ?, ?, ?, ?, ?)
					""", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, request.nombre().trim());
			statement.setString(2, request.descripcion());
			statement.setLong(3, request.precio().longValueExact());
			if (request.stock() == null) {
				statement.setNull(4, Types.BIGINT);
			} else {
				statement.setLong(4, request.stock().longValueExact());
			}
			statement.setLong(5, request.categoriaId());
			statement.setLong(6, request.eventoId());
			return statement;
		}, keys);
		Number id = keys.getKey();
		if (id == null) {
			throw new IllegalStateException("No se pudo obtener el identificador del producto");
		}
		return id.longValue();
	}

	public boolean actualizar(long id, ProductoRequest request) {
		return jdbc.update("""
				UPDATE products SET nombre = ?, descripcion = ?, precio = ?, stock = ?,
					categoria_id = ?, evento_id = ? WHERE id = ?
				""", request.nombre().trim(), request.descripcion(), request.precio().longValueExact(),
				request.stock() == null ? null : request.stock().longValueExact(),
				request.categoriaId(), request.eventoId(), id) == 1;
	}

	public boolean cambiarActivo(long id, boolean activo) {
		return jdbc.update("UPDATE products SET activo = ? WHERE id = ?", activo ? 1 : 0, id) == 1;
	}

	public void registrarStock(long productoId, Long anterior, Long nuevo, String tipo) {
		jdbc.update("""
				INSERT INTO inventory_history (producto_id, cantidad_anterior, cantidad_nueva, tipo_movimiento)
				VALUES (?, ?, ?, ?)
				""", productoId, anterior, nuevo, tipo);
	}
}
