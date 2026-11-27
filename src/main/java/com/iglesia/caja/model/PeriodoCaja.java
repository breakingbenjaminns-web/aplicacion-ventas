package com.iglesia.caja.model;

import java.time.LocalDateTime;
import java.util.List;

public record PeriodoCaja(
		long id, long saldoInicial, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
		Long efectivoEsperado, Long efectivoContado, Long diferencia, List<AporteCaja> aportes) {
}
