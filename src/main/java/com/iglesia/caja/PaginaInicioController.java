package com.iglesia.caja;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PaginaInicioController {

	@GetMapping({ "/", "/index.html" })
	String inicio() {
		return "redirect:/plantilla/index.html";
	}
}