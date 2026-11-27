package com.iglesia.caja;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/caja-test.db")
@AutoConfigureMockMvc
class EventoControllerTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void limpiarEventos() {
		jdbcTemplate.update("INSERT OR IGNORE INTO users (nombre, contrasena, rol, activo) VALUES ('user', 'no-utilizada', 'admin', 1)");
		jdbcTemplate.update("DELETE FROM event_categories");
		jdbcTemplate.update("DELETE FROM box");
		jdbcTemplate.update("DELETE FROM events");
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void crearYConsultarEvento() throws Exception {
		mockMvc.perform(post("/api/eventos")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Festival","descripcion":"Evento anual","fecha":"2026-12-10",
						 "categorias":["Bebida","Comida"],"estado":"activo"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesPattern("/api/eventos/\\d+")))
				.andExpect(jsonPath("$.nombre").value("Festival"))
				.andExpect(jsonPath("$.estado").value("activo"))
				.andExpect(jsonPath("$.fecha").value("2026-12-10"))
				.andExpect(jsonPath("$.categorias.length()").value(2))
				.andExpect(jsonPath("$.caja").value("Cerrada"));

		Long id = jdbcTemplate.queryForObject("SELECT id FROM events", Long.class);
		jdbcTemplate.update("INSERT INTO box (evento_id, estado) VALUES (?, 'abierta')", id);
		mockMvc.perform(get("/api/eventos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].nombre").value("Festival"))
				.andExpect(jsonPath("$[0].categorias.length()").value(2))
				.andExpect(jsonPath("$[0].caja").value("Abierta"));
		mockMvc.perform(get("/api/eventos/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.caja").value("Abierta"))
				.andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void actualizarEventoYReutilizarCategoriasCompartidas() throws Exception {
		String solicitud = """
				{"nombre":"Feria","fecha":"2026-12-11","categorias":["Bebida"],"estado":"activo"}
				""";
		mockMvc.perform(post("/api/eventos")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isCreated());

		Long primerId = jdbcTemplate.queryForObject("SELECT id FROM events", Long.class);
		mockMvc.perform(post("/api/eventos")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Festival","fecha":"2026-12-12","categorias":["Bebida"],"estado":"cerrado"}
						"""))
				.andExpect(status().isCreated());
		Long segundoId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM events", Long.class);
		org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM categories WHERE nombre = 'Bebida' COLLATE NOCASE", Integer.class));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
						"/api/eventos/{id}", primerId)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Feria actualizada","fecha":"2026-12-13",
						 "categorias":["Comida"],"estado":"cancelado"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Feria actualizada"))
				.andExpect(jsonPath("$.fecha").value("2026-12-13"))
				.andExpect(jsonPath("$.estado").value("cancelado"))
				.andExpect(jsonPath("$.categorias[0]").value("Comida"));

		org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM event_categories WHERE evento_id = ?", Integer.class, segundoId));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void cambiarEstadoYRechazarEstadoDesconocido() throws Exception {
		jdbcTemplate.update("INSERT INTO events (nombre, fecha) VALUES ('Feria', '2026-12-10')");
		Long id = jdbcTemplate.queryForObject("SELECT id FROM events", Long.class);

		mockMvc.perform(patch("/api/eventos/{id}/estado", id)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"estado":"activo"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("activo"));

		mockMvc.perform(patch("/api/eventos/{id}/estado", id)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"estado":"inexistente"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void validarCamposYEventoInexistente() throws Exception {
		mockMvc.perform(post("/api/eventos")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":" ","fecha":"2026-12-10","categorias":[],"estado":"activo"}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/eventos/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	void tokenCsrfSeEntregaEnCookieYPermiteGuardarDesdeElNavegador() throws Exception {
		MockHttpSession sesionNueva = new MockHttpSession();
		MvcResult consulta = mockMvc.perform(get("/api/eventos").session(sesionNueva))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpServletResponse respuesta = consulta.getResponse();
		jakarta.servlet.http.Cookie cookie = respuesta.getCookie("XSRF-TOKEN");
		org.junit.jupiter.api.Assertions.assertNotNull(cookie);

		mockMvc.perform(post("/api/eventos")
				.session(sesionNueva)
				.cookie(cookie)
				.header("X-XSRF-TOKEN", cookie.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nombre":"Evento web","fecha":"2026-12-10","categorias":["Bebida"],"estado":"activo"}
						"""))
				.andExpect(status().isCreated());
	}
}
