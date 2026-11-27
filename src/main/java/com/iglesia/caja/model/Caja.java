package com.iglesia.caja.model;

import java.util.List;

public record Caja(Long id, long eventoId, String estado, Long saldoActual, List<PeriodoCaja> periodos) {
}
