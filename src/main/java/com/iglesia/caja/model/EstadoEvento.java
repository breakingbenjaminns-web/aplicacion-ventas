package com.iglesia.caja.model;

import java.util.Arrays;

public enum EstadoEvento {
	PENDIENTE("pendiente"),
	ACTIVO("activo"),
	CERRADO("cerrado"),
	CANCELADO("cancelado");

	private final String valor;

	EstadoEvento(String valor) {
		this.valor = valor;
	}

	public String getValor() {
		return valor;
	}

	public static EstadoEvento desdeValor(String valor) {
		return Arrays.stream(values())
				.filter(estado -> estado.valor.equals(valor))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Estado de evento inválido: " + valor));
	}
}
