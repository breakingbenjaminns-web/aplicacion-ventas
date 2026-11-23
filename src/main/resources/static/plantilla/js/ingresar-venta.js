(function () {
    "use strict";

    const productos = [
        { id: 1, nombre: "Gaseosa", precio: 250000, stock: 30 },
        { id: 2, nombre: "Agua", precio: 150000, stock: 25 },
        { id: 3, nombre: "Empanada", precio: 200000, stock: 40 },
        { id: 4, nombre: "Choripán", precio: 450000, stock: 20 },
        { id: 5, nombre: "Porción de torta", precio: 300000, stock: 15 }
    ];
    const tickets = [
        { id: 1, items: [{ productoId: 1, cantidad: 2 }], pago: "Efectivo", deudor: "", detalle: "", estado: "Abierto" },
        { id: 2, items: [{ productoId: 3, cantidad: 3 }], pago: "Transferencia", deudor: "", detalle: "", estado: "En espera" },
        { id: 3, items: [{ productoId: 4, cantidad: 1 }], pago: "Deuda", deudor: "Persona A", detalle: "Ejemplo de fiado", estado: "Abierto" },
        { id: 4, items: [{ productoId: 2, cantidad: 2 }], pago: "Efectivo", deudor: "", detalle: "", estado: "Cerrado" },
        { id: 5, items: [{ productoId: 5, cantidad: 1 }], pago: "Transferencia", deudor: "", detalle: "", estado: "Cancelado" }
    ];
    const colores = { "Abierto": "badge-primary", "En espera": "badge-warning", "Cerrado": "badge-success", "Cancelado": "badge-secondary" };
    const moneda = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS", maximumFractionDigits: 0 });
    const producto = document.getElementById("producto");
    const cantidad = document.getElementById("cantidad");
    const pago = document.getElementById("pago");
    const deudor = document.getElementById("deudor");
    const estado = document.getElementById("estado");
    const detalle = document.getElementById("detalle");
    const mensaje = document.getElementById("mensaje-venta");
    let actual = nuevoBorrador();
    let modificado = false;
    let siguienteId = 6;

    function nuevoBorrador() {
        return { id: null, items: [], pago: "Efectivo", deudor: "", detalle: "", estado: "Abierto" };
    }

    function copiar(ticket) {
        return { ...ticket, items: ticket.items.map(item => ({ ...item })) };
    }

    function productoPorId(id) {
        return productos.find(item => item.id === id);
    }

    function reservadoPorOtros(id) {
        return tickets
            .filter(ticket => ticket.id !== actual.id && ticket.estado !== "Cancelado")
            .reduce((total, ticket) => total + ticket.items
                .filter(item => item.productoId === id)
                .reduce((suma, item) => suma + item.cantidad, 0), 0);
    }

    function disponible(id, incluirActual) {
        const seleccionado = actual.items.find(item => item.productoId === id);
        const reservadoActual = incluirActual && actual.estado !== "Cancelado" && seleccionado ? seleccionado.cantidad : 0;
        return productoPorId(id).stock - reservadoPorOtros(id) - reservadoActual;
    }

    function total(ticket) {
        return ticket.items.reduce((suma, item) => suma + productoPorId(item.productoId).precio * item.cantidad, 0);
    }

    function formato(centavos) {
        return moneda.format(centavos / 100);
    }

    function elemento(tag, texto, clase) {
        const nodo = document.createElement(tag);
        nodo.textContent = texto;
        if (clase) nodo.className = clase;
        return nodo;
    }

    function mostrarProductos() {
        const seleccionado = producto.value;
        producto.replaceChildren();
        productos.forEach(item => {
            const opcion = elemento("option", `${item.nombre} — ${formato(item.precio)}`);
            opcion.value = item.id;
            producto.append(opcion);
        });
        if (seleccionado) producto.value = seleccionado;
        const id = Number(producto.value);
        const restante = disponible(id, true);
        document.getElementById("disponibilidad").textContent = restante < 0
            ? `El ticket supera el stock de ejemplo por ${-restante} unidades. Los tickets cancelados no reservan stock.`
            : `Stock de ejemplo disponible: ${restante}. Los tickets cancelados no reservan stock.`;
    }

    function mostrarTicket() {
        document.getElementById("titulo-ticket").textContent = actual.id ? `Ticket de ejemplo #${actual.id}` : "Nuevo ticket";
        const cuerpo = document.getElementById("productos-ticket");
        cuerpo.replaceChildren();
        if (!actual.items.length) {
            const fila = document.createElement("tr");
            const celda = elemento("td", "Todavía no agregaste productos.", "text-muted");
            celda.colSpan = 5;
            fila.append(celda);
            cuerpo.append(fila);
        }
        actual.items.forEach(item => {
            const informacion = productoPorId(item.productoId);
            const fila = document.createElement("tr");
            fila.append(elemento("td", informacion.nombre));
            fila.append(elemento("td", String(item.cantidad)));
            fila.append(elemento("td", formato(informacion.precio)));
            fila.append(elemento("td", formato(informacion.precio * item.cantidad)));
            const accion = document.createElement("td");
            const quitar = elemento("button", "Quitar", "btn btn-sm btn-outline-danger");
            quitar.type = "button";
            quitar.setAttribute("aria-label", `Quitar ${informacion.nombre}`);
            quitar.addEventListener("click", () => {
                actual.items = actual.items.filter(linea => linea.productoId !== item.productoId);
                modificado = true;
                mensaje.textContent = "";
                mostrarTicket();
            });
            accion.append(quitar);
            fila.append(accion);
            cuerpo.append(fila);
        });
        document.getElementById("total-ticket").textContent = formato(total(actual));
        mostrarProductos();
    }

    function mostrarLista() {
        const lista = document.getElementById("lista-tickets");
        lista.replaceChildren();
        tickets.forEach(ticket => {
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "list-group-item list-group-item-action";
            boton.setAttribute("aria-pressed", String(ticket.id === actual.id));
            const titulo = elemento("span", `Ticket #${ticket.id}`, "font-weight-bold");
            const insignia = elemento("span", ticket.estado, `badge ${colores[ticket.estado]} ml-2`);
            const resumen = elemento("span", `${ticket.pago} · ${formato(total(ticket))}`, "d-block small mt-1");
            boton.append(titulo, insignia, resumen);
            boton.addEventListener("click", () => {
                if (ticket.id === actual.id) return;
                if (modificado && !window.confirm("Hay cambios sin guardar. ¿Querés descartarlos y abrir otro ticket?")) return;
                actual = copiar(ticket);
                modificado = false;
                mensaje.textContent = "";
                mostrarFormulario();
            });
            lista.append(boton);
        });
    }

    function mostrarFormulario() {
        pago.value = actual.pago;
        deudor.value = actual.deudor;
        estado.value = actual.estado;
        detalle.value = actual.detalle;
        document.getElementById("grupo-deudor").hidden = actual.pago !== "Deuda";
        cantidad.value = 1;
        mostrarTicket();
        mostrarLista();
    }

    document.getElementById("agregar-producto").addEventListener("click", () => {
        const id = Number(producto.value);
        const unidades = Number(cantidad.value);
        if (!Number.isSafeInteger(unidades) || unidades < 1) {
            mensaje.textContent = "Ingresá una cantidad entera mayor que cero.";
            return;
        }
        const actualCantidad = actual.items.find(item => item.productoId === id);
        const yaAgregadas = actualCantidad ? actualCantidad.cantidad : 0;
        if (yaAgregadas + unidades > disponible(id, false)) {
            mensaje.textContent = "La cantidad supera el stock de ejemplo disponible.";
            return;
        }
        if (actualCantidad) actualCantidad.cantidad += unidades;
        else actual.items.push({ productoId: id, cantidad: unidades });
        modificado = true;
        mensaje.textContent = "";
        cantidad.value = 1;
        mostrarTicket();
    });

    producto.addEventListener("change", mostrarProductos);
    pago.addEventListener("change", () => {
        actual.pago = pago.value;
        document.getElementById("grupo-deudor").hidden = actual.pago !== "Deuda";
        modificado = true;
    });
    deudor.addEventListener("input", () => { actual.deudor = deudor.value; modificado = true; });
    detalle.addEventListener("input", () => { actual.detalle = detalle.value; modificado = true; });
    estado.addEventListener("change", () => { actual.estado = estado.value; modificado = true; mostrarProductos(); });

    document.getElementById("nuevo-ticket").addEventListener("click", () => {
        if (!actual.items.length) {
            mensaje.textContent = "Agregá al menos un producto antes de guardar el ticket de ejemplo.";
            return;
        }
        if (actual.pago === "Deuda" && !actual.deudor.trim()) {
            mensaje.textContent = "Ingresá el nombre del deudor.";
            deudor.focus();
            return;
        }
        if (actual.estado !== "Cancelado" && actual.items.some(item => item.cantidad > disponible(item.productoId, false))) {
            mensaje.textContent = "El ticket supera el stock de ejemplo disponible. Quitá productos o cambialo a cancelado.";
            return;
        }
        if (actual.id === null) {
            actual.id = siguienteId++;
            tickets.push(copiar(actual));
        } else {
            const indice = tickets.findIndex(ticket => ticket.id === actual.id);
            tickets[indice] = copiar(actual);
        }
        actual = nuevoBorrador();
        modificado = false;
        mensaje.textContent = "";
        mostrarFormulario();
    });

    mostrarFormulario();
})();
