package com.iglesia.caja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class EsquemaUsuariosTests {

	@Test
	void eventosPuedenTenerUsuarioOpcionalPeroNoInexistente(@TempDir Path directorio) throws Exception {
		try (Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + directorio.resolve("esquema.db"));
				Statement sentencia = conexion.createStatement()) {
			sentencia.execute("PRAGMA foreign_keys=ON");
			ScriptUtils.executeSqlScript(conexion, new ClassPathResource("schema.sql"));
			ScriptUtils.executeSqlScript(conexion, new ClassPathResource("schema.sql"));

			sentencia.executeUpdate("INSERT INTO events (nombre, fecha) VALUES ('Evento sin usuario', '2026-12-10')");
			sentencia.executeUpdate("INSERT INTO users (nombre, contrasena) VALUES ('usuario-prueba', 'hash-prueba')");
			sentencia.executeUpdate("""
					INSERT INTO events (nombre, fecha, usuario_id)
					SELECT 'Evento con usuario', '2026-12-10', id FROM users WHERE nombre = 'usuario-prueba'
					""");

			assertThrows(SQLException.class, () -> sentencia.executeUpdate("""
					INSERT INTO events (nombre, fecha, usuario_id)
					VALUES ('Evento inválido', '2026-12-10', 999999)
					"""));
			try (ResultSet resultado = sentencia.executeQuery("SELECT COUNT(*) FROM events")) {
				assertTrue(resultado.next());
				assertEquals(2, resultado.getInt(1));
			}
		}
	}
}
