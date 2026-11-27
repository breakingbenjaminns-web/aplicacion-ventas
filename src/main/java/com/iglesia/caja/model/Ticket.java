package com.iglesia.caja.model;

import java.util.List;

public record Ticket(long id, long eventoId, String estado, long total, String metodoPago,
		String deudor, String detalle, Long periodoId, String fechaCreacion, String fechaConfirmacion,
		List<Linea> items, Anulacion anulacion) {
	public record Linea(long productoId, String nombre, long cantidad, long precioUnitario, long subtotal) {
	}

	public record Anulacion(String estadoAnterior, String motivo, String fecha, boolean devolucionTransferenciaConfirmada) {
	}
}
