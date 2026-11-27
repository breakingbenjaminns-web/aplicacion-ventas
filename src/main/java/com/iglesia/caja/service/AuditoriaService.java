package com.iglesia.caja.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditoriaService {
	private final JdbcTemplate jdbc;

	public AuditoriaService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void registrar(String accion, String entidad, long entidadId) {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !autenticacion.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere una sesión válida");
		}
		List<Long> usuarios = jdbc.queryForList(
				"SELECT id FROM users WHERE nombre = ? AND rol = 'admin' AND activo = 1",
				Long.class, autenticacion.getName());
		if (usuarios.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado");
		jdbc.update("INSERT INTO activity_log (usuario_id, accion, entidad, entidad_id) VALUES (?, ?, ?, ?)",
				usuarios.get(0), accion, entidad, entidadId);
	}
}
