(async function () {
    "use strict";

    try {
        const eventos = await window.api.solicitar("/api/eventos");
        const idGuardado = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
        const evento = eventos.find(item => item.id === idGuardado) || eventos[0];
        if (!evento) throw new Error("Primero creá y seleccioná un evento.");
        document.getElementById("evento-resumen").textContent = `Evento: ${evento.nombre}`;

        const tickets = await window.api.solicitar(`/api/tickets?eventoId=${evento.id}`);
        const totales = { efectivo: 0n, transferencia: 0n, deuda: 0n };
        tickets.forEach(ticket => {
            if (ticket.eventoId !== evento.id || !Number.isSafeInteger(ticket.total) || ticket.total < 0) {
                throw new Error("El servidor devolvió un ticket con datos inválidos.");
            }
            if (ticket.estado === "pagado" &&
                (ticket.metodoPago === "efectivo" || ticket.metodoPago === "transferencia")) {
                totales[ticket.metodoPago] += BigInt(ticket.total);
            } else if (ticket.estado === "deuda" && ticket.metodoPago === "deuda") {
                totales.deuda += BigInt(ticket.total);
            } else if (ticket.estado !== "abierto" && ticket.estado !== "anulado") {
                throw new Error("El servidor devolvió un ticket con estado o medio de pago inválido.");
            }
        });

        document.getElementById("total-efectivo").textContent = window.importes.formato(totales.efectivo);
        document.getElementById("total-transferencias").textContent = window.importes.formato(totales.transferencia);
        document.getElementById("total-deudas").textContent = window.importes.formato(totales.deuda);
        document.getElementById("total-vendido").textContent =
            window.importes.formato(totales.efectivo + totales.transferencia + totales.deuda);
    } catch (error) {
        console.error("No se pudo cargar el resumen de ventas:", error);
        document.getElementById("error-resumen-ventas").textContent = error.message || "No se pudo cargar el resumen de ventas.";
    }
})();
