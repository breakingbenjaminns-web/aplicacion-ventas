package com.iglesia.caja.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.iglesia.caja.dto.ConfirmarTicketRequest;
import com.iglesia.caja.dto.CrearTicketRequest;
import com.iglesia.caja.dto.AnularTicketRequest;
import com.iglesia.caja.model.HistorialEvento;
import com.iglesia.caja.model.Producto;
import com.iglesia.caja.model.Ticket;
import com.iglesia.caja.repository.EventoRepository;
import com.iglesia.caja.repository.ProductoRepository;
import com.iglesia.caja.repository.TicketRepository;

@Service
public class TicketService {
	private static final long MAX = 9007199254740991L;
	private final TicketRepository tickets;
	private final ProductoRepository productos;
	private final EventoRepository eventos;
	private final AuditoriaService auditoria;

	public TicketService(TicketRepository tickets, ProductoRepository productos, EventoRepository eventos, AuditoriaService auditoria) {
		this.tickets = tickets;
		this.productos = productos;
		this.eventos = eventos;
		this.auditoria = auditoria;
	}

	@Transactional(readOnly = true)
	public Ticket buscar(long id) {
		return tickets.buscar(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
	}

	@Transactional(readOnly = true)
	public List<Ticket> listar(long eventoId) {
		validarEvento(eventoId);
		return tickets.listar(eventoId);
	}

	@Transactional
	public Ticket crear(CrearTicketRequest request) {
		validarEvento(request.eventoId());
		List<Ticket.Linea> lineas = new ArrayList<>();
		Set<Long> ids = new HashSet<>();
		long total = 0;
		for (CrearTicketRequest.Linea item : request.items()) {
			if (!ids.add(item.productoId())) throw error(HttpStatus.BAD_REQUEST, "No se puede repetir un producto");
			Producto producto = productos.buscarPorId(item.productoId())
					.orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Producto no encontrado"));
			if (producto.eventoId() != request.eventoId() || !producto.activo())
				throw error(HttpStatus.BAD_REQUEST, "El producto debe estar activo y pertenecer al evento");
			long cantidad = item.cantidad().longValueExact();
			if (producto.precio() > MAX / cantidad) throw error(HttpStatus.BAD_REQUEST, "El subtotal supera el máximo permitido");
			long subtotal = producto.precio() * cantidad;
			if (subtotal > MAX - total) throw error(HttpStatus.BAD_REQUEST, "El total supera el máximo permitido");
			total += subtotal;
			lineas.add(new Ticket.Linea(producto.id(), producto.nombre(), cantidad, producto.precio(), subtotal));
		}
		long id = tickets.crear(request.eventoId(), request.detalle(), total);
		lineas.forEach(linea -> tickets.agregarLinea(id, linea));
		auditoria.registrar("crear", "ticket", id);
		return buscar(id);
	}

	@Transactional
	public Ticket confirmar(long id, ConfirmarTicketRequest request) {
		String deudor = request.deudor() == null ? null : request.deudor().trim();
		if ("deuda".equals(request.metodoPago())) {
			if (deudor == null || deudor.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "Ingresá el nombre del deudor");
		} else if (deudor != null && !deudor.isEmpty()) {
			throw error(HttpStatus.BAD_REQUEST, "El deudor solo corresponde a una venta a deuda");
		} else {
			deudor = null;
		}
		if (!tickets.confirmar(id, request.metodoPago(), deudor)) {
			buscar(id);
			throw error(HttpStatus.CONFLICT, "El ticket ya fue confirmado o la caja no tiene un período abierto");
		}
		Ticket ticket = buscar(id);
		for (Ticket.Linea linea : ticket.items()) {
			if (!tickets.descontarStock(linea.productoId(), ticket.eventoId(), linea.cantidad(), id))
				throw error(HttpStatus.CONFLICT, "Producto inactivo o stock insuficiente: " + linea.nombre());
		}
		if ("efectivo".equals(request.metodoPago()) && !tickets.ingresarEfectivo(ticket))
			throw error(HttpStatus.CONFLICT, "La caja no está abierta o su saldo supera el máximo permitido");
		auditoria.registrar("confirmar", "ticket", id);
		return ticket;
	}

	@Transactional
	public Ticket anular(long id, AnularTicketRequest request) {
		boolean transferenciaConfirmada = Boolean.TRUE.equals(request.devolucionTransferenciaConfirmada());
		if (!tickets.registrarAnulacion(id, request.motivo().trim(), transferenciaConfirmada)) {
			buscar(id);
			throw error(HttpStatus.CONFLICT, "El ticket ya está anulado");
		}
		Ticket ticket = buscar(id);
		if ("transferencia".equals(ticket.metodoPago()) && !transferenciaConfirmada)
			throw error(HttpStatus.BAD_REQUEST, "Confirmá que la transferencia fue devuelta fuera del sistema");
		if (!"transferencia".equals(ticket.metodoPago()) && transferenciaConfirmada)
			throw error(HttpStatus.BAD_REQUEST, "La devolución por transferencia no corresponde a este ticket");
		if (!"abierto".equals(ticket.estado())) {
			for (Ticket.Linea linea : ticket.items()) {
				if (!tickets.devolverStock(ticket, linea))
					throw error(HttpStatus.CONFLICT, "Revisá el control de stock o el máximo permitido: " + linea.nombre());
			}
			if ("efectivo".equals(ticket.metodoPago()) && !tickets.devolverEfectivo(ticket))
				throw error(HttpStatus.CONFLICT, "La caja debe estar abierta y tener efectivo suficiente para la devolución");
		}
		if (!tickets.marcarAnulado(id)) throw error(HttpStatus.CONFLICT, "El ticket cambió durante la anulación");
		auditoria.registrar("anular", "ticket", id);
		return buscar(id);
	}

	@Transactional(readOnly = true)
	public HistorialEvento historial(long eventoId) {
		validarEvento(eventoId);
		return tickets.historial(eventoId);
	}

	private void validarEvento(long id) {
		if (eventos.buscarPorId(id).isEmpty()) throw error(HttpStatus.NOT_FOUND, "Evento no encontrado");
	}

	private ResponseStatusException error(HttpStatus estado, String mensaje) {
		return new ResponseStatusException(estado, mensaje);
	}
}
