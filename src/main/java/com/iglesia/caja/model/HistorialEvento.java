package com.iglesia.caja.model;

import java.util.List;

public record HistorialEvento(List<Inventario> inventario, List<Caja> caja) {
	public record Inventario(long id, long productoId, String producto, Long anterior, Long nuevo,
			String tipo, Long ticketId, String fecha) {
	}

	public record Caja(long id, long periodoId, long ticketId, String tipo, long monto, String fecha) {
	}
}
