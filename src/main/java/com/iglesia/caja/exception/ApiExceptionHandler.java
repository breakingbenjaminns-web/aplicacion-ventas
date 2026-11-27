package com.iglesia.caja.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> manejarEstado(ResponseStatusException excepcion) {
		String mensaje = excepcion.getReason() == null ? "Solicitud no completada" : excepcion.getReason();
		return ResponseEntity.status(excepcion.getStatusCode())
				.body(Map.of("message", mensaje));
	}
}
