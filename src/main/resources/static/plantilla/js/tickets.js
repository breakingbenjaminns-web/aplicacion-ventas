$(document).ready(async function () {
    "use strict";

    const tabla = $("#dataTable").DataTable();
    function celda(texto, orden) {
        const td = document.createElement("td");
        td.textContent = texto;
        if (orden !== undefined) td.dataset.order = String(orden);
        return td;
    }

    try {
        const eventos = await window.api.solicitar("/api/eventos");
        const id = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
        const evento = eventos.find(item => item.id === id) || eventos[0];
        if (!evento) throw new Error("Primero creá y seleccioná un evento.");
        document.getElementById("evento-tickets").textContent = `Evento: ${evento.nombre}`;
        const [tickets, historial] = await Promise.all([
            window.api.solicitar(`/api/tickets?eventoId=${evento.id}`),
            window.api.solicitar(`/api/historial?eventoId=${evento.id}`)
        ]);
        tickets.forEach(ticket => {
            const fila = document.createElement("tr");
            fila.append(celda(String(ticket.id)), celda(ticket.fechaCreacion),
                celda(ticket.estado), celda(ticket.metodoPago || "Sin confirmar"),
                celda(ticket.deudor || "—"), celda(window.importes.formato(ticket.total), ticket.total),
                celda(ticket.anulacion ? `${ticket.anulacion.fecha} UTC — ${ticket.anulacion.motivo}` +
                    (ticket.anulacion.devolucionTransferenciaConfirmada ? " — Transferencia devuelta externamente (confirmada)" : "") : "—"));
            tabla.row.add(fila);
        });
        tabla.draw();
        const inventario = document.getElementById("historial-inventario");
        historial.inventario.forEach(movimiento => {
            const fila = document.createElement("tr");
            fila.append(celda(movimiento.fecha), celda(movimiento.producto), celda(movimiento.tipo),
                celda(movimiento.anterior === null ? "Sin control" : String(movimiento.anterior)),
                celda(movimiento.nuevo === null ? "Sin control" : String(movimiento.nuevo)),
                celda(movimiento.ticketId === null ? "—" : String(movimiento.ticketId)));
            inventario.append(fila);
        });
        const caja = document.getElementById("historial-caja");
        historial.caja.forEach(movimiento => {
            const fila = document.createElement("tr");
            fila.append(celda(movimiento.fecha), celda(String(movimiento.periodoId)), celda(String(movimiento.ticketId)),
                celda(movimiento.tipo), celda(window.importes.formato(movimiento.monto)));
            caja.append(fila);
        });
    } catch (error) {
        console.error("No se pudieron cargar los tickets:", error);
        document.getElementById("error-tickets").textContent = error.message;
    }
});
