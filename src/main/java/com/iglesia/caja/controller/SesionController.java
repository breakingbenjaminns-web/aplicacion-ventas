package com.iglesia.caja.controller;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SesionController {
	private final JdbcTemplate jdbc;

	public SesionController(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping("/sesion")
	public Sesion sesion(Authentication autenticacion) {
		return new Sesion(autenticacion.getName(), "admin");
	}

	@GetMapping("/actividad")
	public List<Actividad> actividad() {
		return jdbc.query("""
				SELECT a.*, u.nombre FROM activity_log a JOIN users u ON u.id = a.usuario_id
				ORDER BY a.id DESC
				""", (rs, row) -> new Actividad(rs.getLong("id"), rs.getString("nombre"),
				rs.getString("accion"), rs.getString("entidad"), rs.getLong("entidad_id"), rs.getString("fecha")));
	}

	public record Sesion(String nombre, String rol) {
	}

	public record Actividad(long id, String usuario, String accion, String entidad, long entidadId, String fecha) {
	}
}
