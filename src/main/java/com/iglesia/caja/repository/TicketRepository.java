package com.iglesia.caja.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.iglesia.caja.model.Ticket;
import com.iglesia.caja.model.HistorialEvento;

@Repository
public class TicketRepository {
	private final JdbcTemplate jdbc;

	public TicketRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public long crear(long eventoId, String detalle, long total) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO tickets (evento_id, detalle, total) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, eventoId);
			statement.setString(2, detalle);
			statement.setLong(3, total);
			return statement;
		}, keys);
		Number id = keys.getKey();
		if (id == null) throw new IllegalStateException("No se pudo obtener el identificador del ticket");
		return id.longValue();
	}

	public void agregarLinea(long ticketId, Ticket.Linea linea) {
		jdbc.update("INSERT INTO ticket_details (ticket_id, producto_id, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)",
				ticketId, linea.productoId(), linea.cantidad(), linea.precioUnitario(), linea.subtotal());
	}

	public Optional<Ticket> buscar(long id) {
		return jdbc.query("""
				SELECT t.*, a.estado_anterior, a.motivo AS motivo_anulacion, a.fecha AS fecha_anulacion,
					a.devolucion_transferencia FROM tickets t
				LEFT JOIN ticket_cancellations a ON a.ticket_id = t.id WHERE t.id = ?
				""", (rs, row) -> new Ticket(
				rs.getLong("id"), rs.getLong("evento_id"), rs.getString("estado"), rs.getLong("total"),
				rs.getString("metodo_pago"), rs.getString("deudor"), rs.getString("detalle"),
				rs.getObject("periodo_id") == null ? null : rs.getLong("periodo_id"),
				rs.getString("fecha_creacion"), rs.getString("fecha_confirmacion"), List.of(),
				rs.getString("estado_anterior") == null ? null : new Ticket.Anulacion(
						rs.getString("estado_anterior"), rs.getString("motivo_anulacion"),
						rs.getString("fecha_anulacion"), rs.getBoolean("devolucion_transferencia"))), id)
				.stream().findFirst().map(this::cargarLineas);
	}

	public List<Ticket> listar(long eventoId) {
		return jdbc.queryForList("SELECT id FROM tickets WHERE evento_id = ? ORDER BY id DESC", Long.class, eventoId)
				.stream().map(id -> buscar(id).orElseThrow()).toList();
	}

	private Ticket cargarLineas(Ticket ticket) {
		List<Ticket.Linea> items = jdbc.query("""
				SELECT d.*, p.nombre FROM ticket_details d JOIN products p ON p.id = d.producto_id
				WHERE d.ticket_id = ? ORDER BY d.id
				""", (rs, row) -> new Ticket.Linea(rs.getLong("producto_id"), rs.getString("nombre"),
				rs.getLong("cantidad"), rs.getLong("precio_unitario"), rs.getLong("subtotal")), ticket.id());
		return new Ticket(ticket.id(), ticket.eventoId(), ticket.estado(), ticket.total(), ticket.metodoPago(),
				ticket.deudor(), ticket.detalle(), ticket.periodoId(), ticket.fechaCreacion(), ticket.fechaConfirmacion(),
				items, ticket.anulacion());
	}

	public boolean confirmar(long id, String metodo, String deudor) {
		return jdbc.update("""
				UPDATE tickets SET estado = ?, metodo_pago = ?, deudor = ?,
					periodo_id = (SELECT cp.id FROM box b JOIN cash_periods cp ON cp.caja_id = b.id
						WHERE b.evento_id = tickets.evento_id AND b.estado = 'abierta' AND cp.fecha_cierre IS NULL),
					fecha_confirmacion = CURRENT_TIMESTAMP
				WHERE id = ? AND estado = 'abierto'
					AND EXISTS (SELECT 1 FROM box b JOIN cash_periods cp ON cp.caja_id = b.id
						WHERE b.evento_id = tickets.evento_id AND b.estado = 'abierta'
						AND b.saldo_actual IS NOT NULL AND cp.fecha_cierre IS NULL)
				""", "deuda".equals(metodo) ? "deuda" : "pagado", metodo, deudor, id) == 1;
	}

	public boolean descontarStock(long productoId, long eventoId, long cantidad, long ticketId) {
		int actualizado = jdbc.update("""
				UPDATE products SET stock = stock - ?
				WHERE id = ? AND evento_id = ? AND activo = 1 AND stock IS NOT NULL AND stock >= ?
				""", cantidad, productoId, eventoId, cantidad);
		if (actualizado == 1) {
			jdbc.update("""
					INSERT INTO inventory_history (producto_id, cantidad_anterior, cantidad_nueva, tipo_movimiento, ticket_id)
					SELECT id, stock + ?, stock, 'venta', ? FROM products WHERE id = ?
					""", cantidad, ticketId, productoId);
			return true;
		}
		Integer sinControl = jdbc.queryForObject("""
				SELECT COUNT(*) FROM products WHERE id = ? AND evento_id = ? AND activo = 1 AND stock IS NULL
				""", Integer.class, productoId, eventoId);
		return sinControl != null && sinControl == 1;
	}

	public boolean ingresarEfectivo(Ticket ticket) {
		int actualizado = jdbc.update("""
				UPDATE box SET saldo_actual = saldo_actual + ?
				WHERE evento_id = ? AND estado = 'abierta' AND saldo_actual BETWEEN 0 AND ?
					AND EXISTS (SELECT 1 FROM cash_periods WHERE id = ? AND caja_id = box.id AND fecha_cierre IS NULL)
				""", ticket.total(), ticket.eventoId(), 9007199254740991L - ticket.total(), ticket.periodoId());
		if (actualizado != 1) return false;
		jdbc.update("""
				INSERT INTO box_movements (caja_id, periodo_id, ticket_id, tipo, monto)
				SELECT id, ?, ?, 'ingreso', ? FROM box WHERE evento_id = ?
				""", ticket.periodoId(), ticket.id(), ticket.total(), ticket.eventoId());
		return true;
	}

	public boolean registrarAnulacion(long id, String motivo, boolean transferenciaConfirmada) {
		return jdbc.update("""
				INSERT INTO ticket_cancellations (ticket_id, estado_anterior, motivo, devolucion_transferencia)
				SELECT id, estado, ?, ? FROM tickets WHERE id = ? AND estado IN ('abierto', 'pagado', 'deuda')
					AND NOT EXISTS (SELECT 1 FROM ticket_cancellations WHERE ticket_id = tickets.id)
				""", motivo, transferenciaConfirmada ? 1 : 0, id) == 1;
	}

	public boolean devolverStock(Ticket ticket, Ticket.Linea linea) {
		Integer ventas = jdbc.queryForObject("""
				SELECT COUNT(*) FROM inventory_history
				WHERE ticket_id = ? AND producto_id = ? AND tipo_movimiento = 'venta'
				""", Integer.class, ticket.id(), linea.productoId());
		if (ventas == null || ventas > 1) return false;
		if (ventas == 0) {
			Integer sinControl = jdbc.queryForObject("""
					SELECT COUNT(*) FROM products WHERE id = ? AND evento_id = ? AND stock IS NULL
					""", Integer.class, linea.productoId(), ticket.eventoId());
			return sinControl != null && sinControl == 1;
		}
		int actualizado = jdbc.update("""
				UPDATE products SET stock = stock + ?
				WHERE id = ? AND evento_id = ? AND stock IS NOT NULL AND stock <= ?
				""", linea.cantidad(), linea.productoId(), ticket.eventoId(), 9007199254740991L - linea.cantidad());
		if (actualizado != 1) return false;
		jdbc.update("""
				INSERT INTO inventory_history (producto_id, cantidad_anterior, cantidad_nueva, tipo_movimiento, ticket_id)
				SELECT id, stock - ?, stock, 'entrada', ? FROM products WHERE id = ?
				""", linea.cantidad(), ticket.id(), linea.productoId());
		return true;
	}

	public boolean devolverEfectivo(Ticket ticket) {
		int actualizado = jdbc.update("""
				UPDATE box SET saldo_actual = saldo_actual - ?
				WHERE evento_id = ? AND estado = 'abierta' AND saldo_actual >= ?
					AND EXISTS (SELECT 1 FROM cash_periods WHERE caja_id = box.id AND fecha_cierre IS NULL)
				""", ticket.total(), ticket.eventoId(), ticket.total());
		if (actualizado != 1) return false;
		jdbc.update("""
				INSERT INTO cash_refunds (caja_id, periodo_id, ticket_id, monto)
				SELECT b.id, cp.id, ?, ? FROM box b JOIN cash_periods cp ON cp.caja_id = b.id
				WHERE b.evento_id = ? AND b.estado = 'abierta' AND cp.fecha_cierre IS NULL
				""", ticket.id(), ticket.total(), ticket.eventoId());
		return true;
	}

	public boolean marcarAnulado(long id) {
		return jdbc.update("UPDATE tickets SET estado = 'anulado' WHERE id = ? AND estado IN ('abierto', 'pagado', 'deuda')",
				id) == 1;
	}

	public HistorialEvento historial(long eventoId) {
		List<HistorialEvento.Inventario> inventario = jdbc.query("""
				SELECT h.*, p.nombre,
					CASE WHEN h.tipo_movimiento = 'entrada' AND h.ticket_id IS NOT NULL
						AND EXISTS (SELECT 1 FROM ticket_cancellations a WHERE a.ticket_id = h.ticket_id)
						THEN 'devolucion' ELSE h.tipo_movimiento END AS tipo
				FROM inventory_history h JOIN products p ON p.id = h.producto_id
				WHERE p.evento_id = ? ORDER BY h.id DESC
				""", (rs, row) -> new HistorialEvento.Inventario(rs.getLong("id"), rs.getLong("producto_id"),
				rs.getString("nombre"), rs.getObject("cantidad_anterior") == null ? null : rs.getLong("cantidad_anterior"),
				rs.getObject("cantidad_nueva") == null ? null : rs.getLong("cantidad_nueva"), rs.getString("tipo"),
				rs.getObject("ticket_id") == null ? null : rs.getLong("ticket_id"), rs.getString("fecha")), eventoId);
		List<HistorialEvento.Caja> caja = jdbc.query("""
				SELECT m.id, m.periodo_id, m.ticket_id, 'ingreso' AS tipo, m.monto, m.fecha
				FROM box_movements m JOIN box b ON b.id = m.caja_id WHERE b.evento_id = ?
				UNION ALL
				SELECT r.id, r.periodo_id, r.ticket_id, 'egreso' AS tipo, r.monto, r.fecha
				FROM cash_refunds r JOIN box b ON b.id = r.caja_id WHERE b.evento_id = ?
				ORDER BY fecha DESC, ticket_id DESC
				""", (rs, row) -> new HistorialEvento.Caja(rs.getLong("id"), rs.getLong("periodo_id"),
				rs.getLong("ticket_id"), rs.getString("tipo"), rs.getLong("monto"), rs.getString("fecha")),
				eventoId, eventoId);
		return new HistorialEvento(inventario, caja);
	}
}
