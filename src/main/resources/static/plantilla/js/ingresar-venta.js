(function () {
    "use strict";

    const solicitar = window.api.solicitar;
    const formato = window.importes.formato;
    const producto = document.getElementById("producto");
    const cantidad = document.getElementById("cantidad");
    const pago = document.getElementById("pago");
    const deudor = document.getElementById("deudor");
    const detalle = document.getElementById("detalle");
    const mensaje = document.getElementById("mensaje-venta");
    const guardar = document.getElementById("guardar-ticket");
    const confirmar = document.getElementById("confirmar-ticket");
    const anular = document.getElementById("anular-ticket");
    const nuevo = document.getElementById("nuevo-ticket");
    let eventoId;
    let productos = [];
    let tickets = [];
    let actual = null;
    let items = [];
    let ocupado = false;
    let cargado = false;

    function nodo(tag, texto, clase) {
        const elemento = document.createElement(tag);
        elemento.textContent = texto;
        if (clase) elemento.className = clase;
        return elemento;
    }

    function error(excepcion) {
        console.error("No se pudo completar la operación de venta:", excepcion);
        mensaje.textContent = excepcion.message;
    }

    function controles() {
        const editable = cargado && !ocupado && actual === null;
        producto.disabled = !editable;
        cantidad.disabled = !editable;
        detalle.disabled = !editable;
        document.getElementById("agregar-producto").disabled = !editable;
        guardar.disabled = !editable || items.length === 0;
        confirmar.disabled = !cargado || ocupado || !actual || actual.estado !== "abierto";
        anular.disabled = !cargado || ocupado || !actual || actual.estado === "anulado";
        pago.disabled = !cargado || ocupado || !actual || actual.estado !== "abierto";
        deudor.disabled = pago.disabled;
        nuevo.disabled = !cargado || ocupado;
    }

    function mostrarProductos() {
        const seleccionado = producto.value;
        producto.replaceChildren();
        productos.filter(item => item.activo).forEach(item => {
            const opcion = nodo("option", `${item.nombre} — ${formato(item.precio)}`);
            opcion.value = String(item.id);
            producto.append(opcion);
        });
        if (Array.from(producto.options).some(opcion => opcion.value === seleccionado)) producto.value = seleccionado;
        const seleccionadoProducto = productos.find(item => item.id === Number(producto.value));
        document.getElementById("disponibilidad").textContent = !seleccionadoProducto
            ? "No hay productos activos."
            : seleccionadoProducto.stock === null ? "Sin control de stock."
                : `Stock disponible: ${seleccionadoProducto.stock}. Se comprueba al confirmar.`;
    }

    function mostrarTicket() {
        document.getElementById("titulo-ticket").textContent = actual
            ? `Ticket #${actual.id} — ${actual.estado}` : "Nuevo ticket";
        const cuerpo = document.getElementById("productos-ticket");
        cuerpo.replaceChildren();
        const lineas = actual ? actual.items : items;
        if (!lineas.length) {
            const fila = document.createElement("tr");
            const celda = nodo("td", "Todavía no agregaste productos.", "text-muted");
            celda.colSpan = 5;
            fila.append(celda);
            cuerpo.append(fila);
        }
        lineas.forEach(item => {
            const fila = document.createElement("tr");
            fila.append(nodo("td", item.nombre), nodo("td", String(item.cantidad)),
                nodo("td", formato(item.precioUnitario)), nodo("td", formato(item.subtotal)));
            const accion = document.createElement("td");
            if (!actual) {
                const quitar = nodo("button", "Quitar", "btn btn-sm btn-outline-danger");
                quitar.type = "button";
                quitar.disabled = ocupado;
                quitar.addEventListener("click", () => {
                    items = items.filter(linea => linea.productoId !== item.productoId);
                    mostrarTicket();
                });
                accion.append(quitar);
            }
            fila.append(accion);
            cuerpo.append(fila);
        });
        const total = actual ? actual.total : items.reduce((suma, item) => suma + item.subtotal, 0);
        document.getElementById("total-ticket").textContent = formato(total);
        detalle.value = actual ? actual.detalle || "" : detalle.value;
        document.getElementById("anulacion-info").textContent = actual && actual.anulacion
            ? `Anulado: ${actual.anulacion.motivo} (${actual.anulacion.fecha}, UTC).` +
                (actual.anulacion.devolucionTransferenciaConfirmada ? " Devolución externa de transferencia confirmada." : "")
            : "";
        controles();
    }

    function mostrarLista() {
        const lista = document.getElementById("lista-tickets");
        lista.replaceChildren();
        tickets.forEach(ticket => {
            const boton = nodo("button", `Ticket #${ticket.id} — ${ticket.estado} — ${formato(ticket.total)}`,
                "list-group-item list-group-item-action");
            boton.type = "button";
            boton.disabled = ocupado;
            boton.addEventListener("click", () => {
                if (!actual && items.length && !window.confirm("Hay un ticket sin guardar. ¿Querés descartarlo?")) return;
                actual = ticket;
                items = [];
                pago.value = ticket.metodoPago || "efectivo";
                deudor.value = ticket.deudor || "";
                document.getElementById("grupo-deudor").hidden = pago.value !== "deuda";
                mensaje.textContent = "";
                mostrarTicket();
            });
            lista.append(boton);
        });
    }

    async function recargar() {
        [productos, tickets] = await Promise.all([
            solicitar(`/api/productos?eventoId=${eventoId}`),
            solicitar(`/api/tickets?eventoId=${eventoId}`)
        ]);
        if (actual) actual = tickets.find(ticket => ticket.id === actual.id);
        mostrarProductos();
        mostrarLista();
        mostrarTicket();
    }

    document.getElementById("agregar-producto").addEventListener("click", () => {
        const seleccionado = productos.find(item => item.id === Number(producto.value) && item.activo);
        const unidades = Number(cantidad.value);
        if (!seleccionado || !Number.isSafeInteger(unidades) || unidades < 1) {
            mensaje.textContent = "Elegí un producto e ingresá una cantidad entera mayor que cero.";
            return;
        }
        const anterior = items.find(item => item.productoId === seleccionado.id);
        const nuevaCantidad = (anterior ? anterior.cantidad : 0) + unidades;
        const subtotal = nuevaCantidad * seleccionado.precio;
        const total = items.reduce((suma, item) => suma + (item === anterior ? 0 : item.subtotal), 0) + subtotal;
        if (!Number.isSafeInteger(nuevaCantidad) || !Number.isSafeInteger(subtotal) || !Number.isSafeInteger(total)) {
            mensaje.textContent = "La cantidad o el total supera el máximo permitido.";
            return;
        }
        if (anterior) {
            anterior.cantidad = nuevaCantidad;
            anterior.subtotal = subtotal;
        } else {
            items.push({ productoId: seleccionado.id, nombre: seleccionado.nombre, cantidad: unidades,
                precioUnitario: seleccionado.precio, subtotal });
        }
        mensaje.textContent = "";
        mostrarTicket();
    });

    producto.addEventListener("change", mostrarProductos);
    pago.addEventListener("change", () => {
        document.getElementById("grupo-deudor").hidden = pago.value !== "deuda";
    });
    nuevo.addEventListener("click", () => {
        if (!actual && items.length && !window.confirm("Hay un ticket sin guardar. ¿Querés descartarlo?")) return;
        actual = null;
        items = [];
        detalle.value = "";
        deudor.value = "";
        pago.value = "efectivo";
        document.getElementById("grupo-deudor").hidden = true;
        mensaje.textContent = "";
        mostrarTicket();
    });

    guardar.addEventListener("click", async () => {
        ocupado = true;
        controles();
        mostrarLista();
        try {
            actual = await solicitar("/api/tickets", {
                method: "POST",
                body: JSON.stringify({ eventoId, detalle: detalle.value,
                    items: items.map(item => ({ productoId: item.productoId, cantidad: item.cantidad })) })
            });
            items = [];
            await recargar();
            mensaje.textContent = "Ticket guardado. Confirmá la venta para descontar el stock.";
        } catch (excepcion) {
            error(excepcion);
        } finally {
            ocupado = false;
            controles();
            mostrarLista();
        }
    });

    confirmar.addEventListener("click", async () => {
        if (pago.value === "deuda" && !deudor.value.trim()) {
            mensaje.textContent = "Ingresá el nombre del deudor.";
            deudor.focus();
            return;
        }
        ocupado = true;
        controles();
        mostrarLista();
        try {
            actual = await solicitar(`/api/tickets/${actual.id}/confirmar`, {
                method: "POST",
                body: JSON.stringify({ metodoPago: pago.value, deudor: pago.value === "deuda" ? deudor.value.trim() : null })
            });

            await recargar();
            mensaje.textContent = actual.estado === "deuda" ? "Deuda registrada; el cobro queda pendiente." : "Venta confirmada.";
            window.dispatchEvent(new Event("evento-seleccionado-cambio"));
        } catch (excepcion) {
            error(excepcion);
        } finally {
            ocupado = false;
            controles();
            mostrarLista();
        }
    });

    anular.addEventListener("click", async () => {
        const motivo = window.prompt("Ingresá el motivo de la anulación (máximo 500 caracteres):");
        if (motivo === null) return;
        if (!motivo.trim() || motivo.length > 500) {
            mensaje.textContent = "Ingresá un motivo de hasta 500 caracteres.";
            return;
        }
        let devolucionTransferenciaConfirmada = false;
        if (actual.metodoPago === "transferencia") {
            devolucionTransferenciaConfirmada = window.confirm(
                "¿Confirmás que ya devolviste la transferencia fuera del sistema? La aplicación no realiza transferencias.");
            if (!devolucionTransferenciaConfirmada) return;
        } else if (!window.confirm("¿Querés anular este ticket? Esta acción no se puede deshacer.")) {
            return;
        }
        ocupado = true;
        controles();
        mostrarLista();
        try {
            actual = await solicitar(`/api/tickets/${actual.id}/anular`, {
                method: "POST",
                body: JSON.stringify({ motivo: motivo.trim(), devolucionTransferenciaConfirmada })
            });
            await recargar();
            mensaje.textContent = "Ticket anulado; se conserva su historial.";
            window.dispatchEvent(new Event("evento-seleccionado-cambio"));
        } catch (excepcion) {
            error(excepcion);
        } finally {
            ocupado = false;
            controles();
            mostrarLista();
        }
    });

    (async function iniciar() {
        try {
            const eventos = await solicitar("/api/eventos");
            const id = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
            const evento = eventos.find(item => item.id === id) || eventos[0];
            if (!evento) throw new Error("Primero creá y seleccioná un evento.");
            eventoId = evento.id;
            document.getElementById("evento-venta").textContent = `Evento: ${evento.nombre}. La caja debe estar abierta para confirmar.`;
            await recargar();
            cargado = true;
            controles();
        } catch (excepcion) {
            error(excepcion);
        }
    })();
})();
