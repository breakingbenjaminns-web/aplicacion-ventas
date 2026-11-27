package com.iglesia.caja.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.service.UsuarioService;

@Controller
public class InicializacionController {
	private final UsuarioService usuarios;

	public InicializacionController(UsuarioService usuarios) {
		this.usuarios = usuarios;
	}

	@GetMapping("/inicializar")
	public String formulario(HttpServletRequest request) {
		validarLocal(request);
		if (usuarios.existeUsuario()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		return "inicializar";
	}

	@PostMapping("/inicializar")
	public String crear(HttpServletRequest request, @RequestParam String nombre,
			@RequestParam String contrasena, @RequestParam String confirmacion, Model model) {
		validarLocal(request);
		try {
			usuarios.crearAdministradorInicial(nombre, contrasena, confirmacion);
		} catch (ResponseStatusException excepcion) {
			if (excepcion.getStatusCode().value() != 400) throw excepcion;
			model.addAttribute("error", excepcion.getReason());
			model.addAttribute("nombre", nombre);
			return "inicializar";
		}
		return "redirect:/login";
	}

	private void validarLocal(HttpServletRequest request) {
		try {
			String host = request.getServerName();
			boolean hostLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
					|| "::1".equals(host) || "[::1]".equals(host);
			if (!hostLocal || !InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
		} catch (UnknownHostException excepcion) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dirección local inválida", excepcion);
		}
	}
}
