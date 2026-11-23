package com.iglesia.caja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/caja-test.db")
@AutoConfigureMockMvc
class CajaApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Validator validator;

	record DatosDePrueba(@NotBlank String nombre,
			@NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal importe) {
	}

	@Test
	void contextLoads() {
	}

	@Test
	void accesoAnonimoRedirigeAlLogin() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/login")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser
	void accesoAutenticadoYProteccionCsrf() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/plantilla/index.html"));
		mockMvc.perform(get("/index.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/plantilla/index.html"));
		mockMvc.perform(get("/plantilla/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("SB Admin 2 - Panel principal")));
		mockMvc.perform(post("/")).andExpect(status().isForbidden());
		mockMvc.perform(post("/").with(csrf())).andExpect(status().isMethodNotAllowed());
	}

	@Test
	@WithMockUser
	void recursosWebSeSirvenLocalmente() throws Exception {
		mockMvc.perform(get("/plantilla/css/sb-admin-2.min.css")).andExpect(status().isOk());
		mockMvc.perform(get("/plantilla/js/sb-admin-2.min.js")).andExpect(status().isOk());
		mockMvc.perform(get("/plantilla/vendor/jquery/jquery.min.js")).andExpect(status().isOk());
		mockMvc.perform(get("/plantilla/vendor/bootstrap/js/bootstrap.bundle.min.js")).andExpect(status().isOk());
	}

	@Test
	void springJdbcSeConectaASqlite() {
		String version = jdbcTemplate.queryForObject("SELECT sqlite_version()", String.class);
		assertTrue(version != null && !version.isBlank());
	}

	@Test
	void validacionRechazaDatosInvalidos() {
		assertTrue(validator.validate(new DatosDePrueba("Producto", new BigDecimal("10.50"))).isEmpty());
		assertFalse(validator.validate(new DatosDePrueba(" ", new BigDecimal("10.50"))).isEmpty());
		assertFalse(validator.validate(new DatosDePrueba("Producto", null)).isEmpty());
		assertFalse(validator.validate(new DatosDePrueba("Producto", new BigDecimal("-1.00"))).isEmpty());
		assertFalse(validator.validate(new DatosDePrueba("Producto", new BigDecimal("1.234"))).isEmpty());
	}

	@Test
	void importesSeCalculanYGuardanSinPerderPrecision(@TempDir Path directorio) throws Exception {
		BigDecimal total = new BigDecimal("0.10").add(new BigDecimal("0.20"));
		assertEquals(new BigDecimal("0.30"), total);
		assertThrows(ArithmeticException.class, () -> new BigDecimal("0.001").movePointRight(2).longValueExact());

		String url = "jdbc:sqlite:" + directorio.resolve("importes.db");
		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement()) {
			sentencia.executeUpdate("CREATE TABLE prueba_importes (centavos INTEGER NOT NULL)");
			try (PreparedStatement insercion = conexion.prepareStatement("INSERT INTO prueba_importes VALUES (?)")) {
				insercion.setLong(1, total.movePointRight(2).longValueExact());
				assertEquals(1, insercion.executeUpdate());
			}
		}

		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement();
				ResultSet resultado = sentencia.executeQuery("SELECT centavos, typeof(centavos) FROM prueba_importes")) {
			assertTrue(resultado.next());
			assertEquals("integer", resultado.getString(2));
			assertEquals(total, BigDecimal.valueOf(resultado.getLong(1), 2));
		}
	}

	@Test
	void respaldoYRestauracionAislados(@TempDir Path directorio) throws Exception {
		Path original = directorio.resolve("original.db");
		Path respaldo = directorio.resolve("respaldo.db");
		Path restaurada = directorio.resolve("restaurada.db");

		try (Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + original);
				Statement sentencia = conexion.createStatement()) {
			sentencia.execute("PRAGMA journal_mode=WAL");
			sentencia.executeUpdate("CREATE TABLE prueba (valor TEXT)");
			sentencia.executeUpdate("INSERT INTO prueba VALUES ('antes')");
			RespaldoSqlite.copiar(original, respaldo);
			sentencia.executeUpdate("UPDATE prueba SET valor = 'despues'");
			RespaldoSqlite.copiar(respaldo, restaurada);
			try (ResultSet resultado = sentencia.executeQuery("SELECT valor FROM prueba")) {
				assertTrue(resultado.next());
				assertEquals("despues", resultado.getString(1));
			}
		}

		try (Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + restaurada);
				Statement sentencia = conexion.createStatement();
				ResultSet resultado = sentencia.executeQuery("SELECT valor FROM prueba")) {
			assertTrue(resultado.next());
			assertEquals("antes", resultado.getString(1));
		}
		assertThrows(java.io.IOException.class, () -> RespaldoSqlite.copiar(original, respaldo));
		assertThrows(java.io.IOException.class, () -> RespaldoSqlite.copiar(directorio.resolve("ausente.db"), directorio.resolve("nueva.db")));
		assertFalse(Files.exists(directorio.resolve("ausente.db")));
		assertFalse(Files.exists(directorio.resolve("nueva.db")));
		Files.createFile(directorio.resolve("bloqueada.db-wal"));
		assertThrows(java.io.IOException.class, () -> RespaldoSqlite.copiar(original, directorio.resolve("bloqueada.db")));
		assertFalse(Files.exists(directorio.resolve("bloqueada.db")));
		assertEquals(1, CajaApplication.ejecutarRespaldo(new String[] { "--crear-respaldo" }));
		assertEquals(0, CajaApplication.ejecutarRespaldo(new String[] {
				"--crear-respaldo", original.toString(), directorio.resolve("comando.db").toString() }));
		assertEquals(1, CajaApplication.ejecutarRespaldo(new String[] {
				"--restaurar-respaldo", respaldo.toString(), original.toString() }));
	}

	@Test
	void sqlitePermiteOperacionesBasicasYPersisteDatos(@TempDir Path directorio) throws Exception {
		String url = "jdbc:sqlite:" + directorio.resolve("prueba.db");

		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement()) {
			sentencia.executeUpdate("CREATE TABLE prueba (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL)");
			assertEquals(1, sentencia.executeUpdate("INSERT INTO prueba (id, nombre) VALUES (1, 'Empanada')"));
		}

		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement()) {
			try (ResultSet resultado = sentencia.executeQuery("SELECT nombre FROM prueba WHERE id = 1")) {
				assertTrue(resultado.next());
				assertEquals("Empanada", resultado.getString("nombre"));
			}
			assertEquals(1, sentencia.executeUpdate("UPDATE prueba SET nombre = 'Pizza' WHERE id = 1"));
		}

		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement()) {
			try (ResultSet resultado = sentencia.executeQuery("SELECT nombre FROM prueba WHERE id = 1")) {
				assertTrue(resultado.next());
				assertEquals("Pizza", resultado.getString("nombre"));
			}
			assertEquals(1, sentencia.executeUpdate("DELETE FROM prueba WHERE id = 1"));
		}

		try (Connection conexion = DriverManager.getConnection(url);
				Statement sentencia = conexion.createStatement();
				ResultSet resultado = sentencia.executeQuery("SELECT COUNT(*) FROM prueba")) {
			assertTrue(resultado.next());
			assertEquals(0, resultado.getInt(1));
		}
	}

}
