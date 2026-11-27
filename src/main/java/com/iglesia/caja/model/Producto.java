package com.iglesia.caja.model;

import java.time.LocalDateTime;

public record Producto(
		long id, String nombre, String descripcion, long precio, Long stock,
		long categoriaId, String categoria, long eventoId, boolean activo, LocalDateTime fechaCreacion) {
}
