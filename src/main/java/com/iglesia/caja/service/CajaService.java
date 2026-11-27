package com.iglesia.caja.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.dto.AbrirCajaRequest;
import com.iglesia.caja.model.Caja;
import com.iglesia.caja.model.PeriodoCaja;
import com.iglesia.caja.repository.CajaRepository;
import com.iglesia.caja.repository.EventoRepository;

@Service
public class CajaService {
	private static final long MAX_IMPORTE = 9007199254740991L;
	private final CajaRepository cajas;
	private final EventoRepository eventos;
	private final AuditoriaService auditoria;

	public CajaService(CajaRepository cajas, EventoRepository eventos, AuditoriaService auditoria) {
		this.cajas = cajas;
		this.eventos = eventos;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	public Caja consultar(long eventoId) {
		validarEvento(eventoId);
		return cajas.buscarPorEvento(eventoId)
				.orElseGet(() -> new Caja(null, eventoId, "cerrada", null, List.of()));
	}

	@Transactional
	public Caja abrir(long eventoId, AbrirCajaRequest request) {
		validarEvento(eventoId);
		long total = 0;
		for (AbrirCajaRequest.Aporte aporte : request.aportes()) {
			long importe = aporte.importe().longValueExact();
			if (importe > MAX_IMPORTE - total) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suma de los aportes supera el máximo permitido");
			}
			total += importe;
		}
		cajas.crearCaja(eventoId);
		Caja caja = cajaExistente(eventoId);
		if (!caja.periodos().isEmpty()) {
			throw conflicto("La apertura inicial ya fue registrada; utilizá la reapertura");
		}
		if (!"cerrada".equals(caja.estado()) || cajas.tieneDatosPrevios(caja.id())) {
			throw conflicto("La caja contiene datos anteriores sin períodos; no se puede modificar automáticamente");
		}
		if (!cajas.abrir(caja.id(), total)) {
			throw conflicto("La caja ya está abierta");
		}
		long periodoId = cajas.crearPeriodo(caja.id(), total);
		cajas.guardarAportes(periodoId, request.aportes());
		auditoria.registrar("abrir", "caja", caja.id());
		return cajaExistente(eventoId);
	}

	@Transactional
	public Caja cerrar(long eventoId, long contado) {
		validarEvento(eventoId);
		Caja caja = cajaExistente(eventoId);
		PeriodoCaja periodo = ultimoPeriodo(caja);
		if (!"abierta".equals(caja.estado()) || periodo.fechaCierre() != null || caja.saldoActual() == null) {
			throw conflicto("La caja no tiene un período abierto válido");
		}
		long esperado = caja.saldoActual();
		if (esperado < 0 || esperado > MAX_IMPORTE) {
			throw conflicto("El saldo de caja está fuera del rango permitido");
		}
		if (!cajas.cerrarCaja(caja.id(), contado) || !cajas.cerrarPeriodo(periodo.id(), esperado, contado)) {
			throw conflicto("La caja ya fue cerrada o cambió durante la operación");
		}
		auditoria.registrar("cerrar", "caja", caja.id());
		return cajaExistente(eventoId);
	}

	@Transactional
	public Caja reabrir(long eventoId) {
		validarEvento(eventoId);
		Caja caja = cajaExistente(eventoId);
		PeriodoCaja anterior = ultimoPeriodo(caja);
		if (!"cerrada".equals(caja.estado()) || anterior.fechaCierre() == null || anterior.efectivoContado() == null) {
			throw conflicto("La caja debe tener un cierre válido antes de reabrir");
		}
		long inicial = anterior.efectivoContado();
		if (!cajas.abrir(caja.id(), inicial)) {
			throw conflicto("La caja ya está abierta");
		}
		cajas.crearPeriodo(caja.id(), inicial);
		auditoria.registrar("reabrir", "caja", caja.id());
		return cajaExistente(eventoId);
	}

	private void validarEvento(long eventoId) {
		if (eventos.buscarPorId(eventoId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
		}
	}

	private Caja cajaExistente(long eventoId) {
		return cajas.buscarPorEvento(eventoId)
				.orElseThrow(() -> conflicto("El evento todavía no tiene una caja abierta"));
	}

	private PeriodoCaja ultimoPeriodo(Caja caja) {
		if (caja.periodos().isEmpty()) {
			throw conflicto("La caja no tiene períodos registrados");
		}
		return caja.periodos().get(caja.periodos().size() - 1);
	}

	private ResponseStatusException conflicto(String mensaje) {
		return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
	}
}
