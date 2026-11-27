package com.iglesia.caja.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.iglesia.caja.dto.AbrirCajaRequest;
import com.iglesia.caja.model.AporteCaja;
import com.iglesia.caja.model.Caja;
import com.iglesia.caja.model.PeriodoCaja;

@Repository
public class CajaRepository {
	private static final RowMapper<PeriodoCaja> PERIODO = (rs, row) -> new PeriodoCaja(
			rs.getLong("id"), rs.getLong("saldo_inicial"), fecha(rs.getString("fecha_apertura")),
			fecha(rs.getString("fecha_cierre")),
			numero(rs.getObject("efectivo_esperado")),
			numero(rs.getObject("efectivo_contado")),
			numero(rs.getObject("diferencia")), List.of());
	private final JdbcTemplate jdbc;

	public CajaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Caja> buscarPorEvento(long eventoId) {
		return jdbc.query("SELECT * FROM box WHERE evento_id = ?", (rs, row) -> new Caja(
				rs.getLong("id"), rs.getLong("evento_id"), rs.getString("estado"),
				numero(rs.getObject("saldo_actual")), List.of()), eventoId)
				.stream().findFirst().map(this::cargarPeriodos);
	}

	public boolean crearCaja(long eventoId) {
		return jdbc.update("INSERT OR IGNORE INTO box (evento_id, estado) VALUES (?, 'cerrada')", eventoId) == 1;
	}

	public boolean tieneDatosPrevios(long cajaId) {
		Integer cantidad = jdbc.queryForObject("""
				SELECT COUNT(*) FROM box
				WHERE id = ? AND (saldo_inicial IS NOT NULL OR saldo_actual IS NOT NULL
					OR fecha_apertura IS NOT NULL OR fecha_cierre IS NOT NULL)
				""", Integer.class, cajaId);
		return cantidad != null && cantidad > 0;
	}

	public boolean abrir(long cajaId, long saldoInicial) {
		return jdbc.update("""
				UPDATE box SET estado = 'abierta', saldo_inicial = ?, saldo_actual = ?,
					fecha_apertura = CURRENT_TIMESTAMP, fecha_cierre = NULL
				WHERE id = ? AND estado = 'cerrada'
				""", saldoInicial, saldoInicial, cajaId) == 1;
	}

	public long crearPeriodo(long cajaId, long saldoInicial) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			PreparedStatement statement = connection.prepareStatement("""
					INSERT INTO cash_periods (caja_id, saldo_inicial) VALUES (?, ?)
					""", Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, cajaId);
			statement.setLong(2, saldoInicial);
			return statement;
		}, keys);
		Number id = keys.getKey();
		if (id == null) {
			throw new IllegalStateException("No se pudo obtener el identificador del período de caja");
		}
		return id.longValue();
	}

	public void guardarAportes(long periodoId, List<AbrirCajaRequest.Aporte> aportes) {
		for (AbrirCajaRequest.Aporte aporte : aportes) {
			jdbc.update("INSERT INTO cash_contributions (periodo_id, responsable, importe) VALUES (?, ?, ?)",
					periodoId, aporte.responsable().trim(), aporte.importe().longValueExact());
		}
	}

	public boolean cerrarCaja(long cajaId, long contado) {
		return jdbc.update("""
				UPDATE box SET estado = 'cerrada', saldo_actual = ?, fecha_cierre = CURRENT_TIMESTAMP
				WHERE id = ? AND estado = 'abierta'
				""", contado, cajaId) == 1;
	}

	public boolean cerrarPeriodo(long periodoId, long esperado, long contado) {
		return jdbc.update("""
				UPDATE cash_periods SET fecha_cierre = CURRENT_TIMESTAMP, efectivo_esperado = ?,
					efectivo_contado = ?, diferencia = ? WHERE id = ? AND fecha_cierre IS NULL
				""", esperado, contado, contado - esperado, periodoId) == 1;
	}

	private Caja cargarPeriodos(Caja caja) {
		List<PeriodoCaja> periodos = jdbc.query(
				"SELECT * FROM cash_periods WHERE caja_id = ? ORDER BY id", PERIODO, caja.id())
				.stream().map(periodo -> new PeriodoCaja(
						periodo.id(), periodo.saldoInicial(), periodo.fechaApertura(), periodo.fechaCierre(),
						periodo.efectivoEsperado(), periodo.efectivoContado(), periodo.diferencia(),
						jdbc.query("SELECT * FROM cash_contributions WHERE periodo_id = ? ORDER BY id",
								(rs, row) -> new AporteCaja(rs.getLong("id"), rs.getString("responsable"),
										rs.getLong("importe")), periodo.id())))
				.toList();
		return new Caja(caja.id(), caja.eventoId(), caja.estado(), caja.saldoActual(), periodos);
	}

	private static LocalDateTime fecha(String valor) {
		return valor == null ? null : Timestamp.valueOf(valor).toLocalDateTime();
	}

	private static Long numero(Object valor) {
		return valor == null ? null : ((Number) valor).longValue();
	}
}
