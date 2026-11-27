package com.iglesia.caja.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService implements UserDetailsService {
	private final JdbcTemplate jdbc;
	private final PasswordEncoder passwords;

	public UsuarioService(JdbcTemplate jdbc, PasswordEncoder passwords) {
		this.jdbc = jdbc;
		this.passwords = passwords;
	}

	@Override
	public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
		List<UserDetails> usuarios = jdbc.query("""
				SELECT nombre, contrasena, activo FROM users WHERE nombre = ? AND rol = 'admin'
				""", (rs, row) -> User.withUsername(rs.getString("nombre"))
				.password(rs.getString("contrasena"))
				.roles("ADMIN")
				.disabled(!rs.getBoolean("activo"))
				.build(), nombre);
		return usuarios.stream().findFirst()
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
	}

	public boolean existeUsuario() {
		Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
		if (cantidad == null) throw new IllegalStateException("No se pudo consultar la inicialización de usuarios");
		return cantidad > 0;
	}

	@Transactional
	public void crearAdministradorInicial(String nombre, String contrasena, String confirmacion) {
		String usuario = nombre.trim();
		if (!usuario.matches("[A-Za-z0-9._-]{3,40}")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"El usuario debe tener entre 3 y 40 caracteres: letras, números, punto, guion o guion bajo.");
		}
		if (contrasena.length() < 12 || contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La contraseña debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8.");
		}
		if (!contrasena.equals(confirmacion)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden.");
		}
		String hash = passwords.encode(contrasena);
		int creados = jdbc.update("""
				INSERT INTO users (nombre, contrasena, rol, activo)
				SELECT ?, ?, 'admin', 1 WHERE NOT EXISTS (SELECT 1 FROM users)
				""", usuario, hash);
		if (creados != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "La inicialización ya fue realizada.");
		jdbc.update("""
				INSERT INTO activity_log (usuario_id, accion, entidad, entidad_id)
				SELECT id, 'crear', 'usuario', id FROM users WHERE nombre = ?
				""", usuario);
	}
}
