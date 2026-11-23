(function () {
    "use strict";

    const formApertura = document.getElementById("form-apertura");
    const formCierre = document.getElementById("form-cierre");
    const error = document.getElementById("error-caja");
    const mensaje = document.getElementById("mensaje-caja");
    const ejemplo = { efectivo: 5000000, transferencia: 1200000, deuda: 700000 };
    let eventoId;
    let numeroAporte = 0;

    function esImporte(valor) {
        return Number.isSafeInteger(valor) && valor >= 0;
    }

    function sumar(...valores) {
        const total = valores.reduce((resultado, valor) => resultado + BigInt(valor), 0n);
        if (total > BigInt(Number.MAX_SAFE_INTEGER)) {
            throw new Error("La suma supera el máximo permitido para la demostración.");
        }
        return Number(total);
    }

    function leerEvento(datos) {
        if (datos.actualId !== eventoId) throw new Error("Cambió el evento seleccionado; recargá la página.");
        const evento = datos.eventos.find(item => item.id === eventoId);
        if (!evento) throw new Error("No se encontró el evento seleccionado.");
        const gestion = evento.gestionCaja;
        if (gestion === undefined) return evento;
        if (!gestion || !Array.isArray(gestion.aportes) || !Array.isArray(gestion.ciclos) ||
            !gestion.ciclos.length || !Array.isArray(gestion.historial) ||
            gestion.aportes.some(item => typeof item.responsable !== "string" || !esImporte(item.importe)) ||
            gestion.ciclos.some(ciclo => !esImporte(ciclo.inicial) || !esImporte(ciclo.efectivo) ||
                !esImporte(ciclo.transferencia) || !esImporte(ciclo.deuda) ||
                (ciclo.cierre !== null && (!ciclo.cierre || !esImporte(ciclo.cierre.esperado) ||
                    !esImporte(ciclo.cierre.contado) || !Number.isSafeInteger(ciclo.cierre.diferencia)))) ||
            gestion.historial.some(item => typeof item.fecha !== "string" ||
                typeof item.accion !== "string" || typeof item.detalle !== "string")) {
            throw new Error("Los datos de caja de demostración no son válidos.");
        }
        const ultimo = gestion.ciclos[gestion.ciclos.length - 1];
        if ((evento.caja === "Abierta") !== (ultimo.cierre === null)) {
            throw new Error("El estado de caja no coincide con su historial de demostración.");
        }
        return evento;
    }

    function informarError(excepcion) {
        console.error("No se pudo completar la operación de caja de demostración:", excepcion);
        error.textContent = excepcion.message || "No se pudo completar la operación de caja.";
        mensaje.textContent = "";
    }

    function celda(texto) {
        const nodo = document.createElement("td");
        nodo.textContent = texto;
        return nodo;
    }

    function filaVacia(contenedor, texto, columnas) {
        const fila = document.createElement("tr");
        const dato = celda(texto);
        dato.colSpan = columnas;
        fila.append(dato);
        contenedor.append(fila);
    }

    function mostrar(evento) {
        const gestion = evento.gestionCaja;
        const ciclos = gestion ? gestion.ciclos : [];
        const ultimo = ciclos[ciclos.length - 1];
        document.getElementById("caja-evento").textContent = `Evento de ejemplo: ${evento.nombre} (${evento.estado})`;
        document.getElementById("estado-caja").textContent = `Caja ${evento.caja.toLowerCase()}`;
        document.getElementById("aviso-caja").textContent = !gestion && evento.caja === "Abierta"
            ? "Esta caja estaba marcada como abierta en una versión anterior, sin apertura registrada. Registrá los aportes para continuar."
            : "";
        document.getElementById("seccion-apertura").hidden = !!gestion;
        document.getElementById("titulo-apertura").textContent = evento.caja === "Abierta" ? "Registrar apertura inicial" : "Abrir caja";
        document.getElementById("abrir-caja").textContent = evento.caja === "Abierta" ? "Registrar apertura inicial" : "Abrir caja";
        document.getElementById("seccion-cierre").hidden = !gestion || evento.caja !== "Abierta";
        document.getElementById("seccion-reapertura").hidden = !gestion || evento.caja !== "Cerrada";

        const mostrarImporte = (id, valor) => {
            document.getElementById(id).textContent = valor === null ? "—" : window.importes.formato(valor);
        };
        mostrarImporte("total-aportes", gestion ? sumar(...gestion.aportes.map(item => item.importe)) : null);
        mostrarImporte("ventas-efectivo", gestion ? sumar(...ciclos.map(item => item.efectivo)) : null);
        mostrarImporte("ventas-transferencia", gestion ? sumar(...ciclos.map(item => item.transferencia)) : null);
        mostrarImporte("ventas-deuda", gestion ? sumar(...ciclos.map(item => item.deuda)) : null);
        mostrarImporte("base-caja", ultimo ? ultimo.inicial : null);
        mostrarImporte("efectivo-esperado", ultimo ? sumar(ultimo.inicial, ultimo.efectivo) : null);

        const cerrados = ciclos.filter(item => item.cierre);
        const cierre = cerrados.length ? cerrados[cerrados.length - 1].cierre : null;
        document.getElementById("ultimo-cierre").hidden = !cierre;
        if (cierre) {
            mostrarImporte("efectivo-contado", cierre.contado);
            mostrarImporte("diferencia-cierre", cierre.diferencia);
        }

        const listaAportes = document.getElementById("lista-aportes");
        listaAportes.replaceChildren();
        if (!gestion || !gestion.aportes.length) filaVacia(listaAportes, "No hay aportes iniciales registrados.", 2);
        else gestion.aportes.forEach(item => {
            const fila = document.createElement("tr");
            fila.append(celda(item.responsable), celda(window.importes.formato(item.importe)));
            listaAportes.append(fila);
        });

        const historial = document.getElementById("historial-caja");
        historial.replaceChildren();
        if (!gestion || !gestion.historial.length) filaVacia(historial, "No hay movimientos de caja registrados.", 3);
        else gestion.historial.slice().reverse().forEach(item => {
            const fila = document.createElement("tr");
            fila.append(celda(new Date(item.fecha).toLocaleString("es-AR")), celda(item.accion), celda(item.detalle));
            historial.append(fila);
        });
    }

    document.getElementById("agregar-aporte").addEventListener("click", () => {
        const id = ++numeroAporte;
        const fila = document.createElement("div");
        fila.className = "form-row align-items-end";
        const responsable = document.createElement("div");
        responsable.className = "form-group col-md-5";
        const etiquetaResponsable = document.createElement("label");
        etiquetaResponsable.htmlFor = `responsable-${id}`;
        etiquetaResponsable.textContent = "Responsable";
        const campoResponsable = document.createElement("input");
        campoResponsable.id = `responsable-${id}`;
        campoResponsable.className = "form-control responsable-aporte";
        campoResponsable.maxLength = 100;
        campoResponsable.required = true;
        responsable.append(etiquetaResponsable, campoResponsable);
        const importe = document.createElement("div");
        importe.className = "form-group col-md-5";
        const etiquetaImporte = document.createElement("label");
        etiquetaImporte.htmlFor = `importe-${id}`;
        etiquetaImporte.textContent = "Importe";
        const campoImporte = document.createElement("input");
        campoImporte.id = `importe-${id}`;
        campoImporte.className = "form-control importe-aporte";
        campoImporte.type = "text";
        campoImporte.inputMode = "decimal";
        campoImporte.placeholder = "Ej.: 10000,00";
        campoImporte.required = true;
        importe.append(etiquetaImporte, campoImporte);
        const accion = document.createElement("div");
        accion.className = "form-group col-md-2";
        const quitar = document.createElement("button");
        quitar.type = "button";
        quitar.className = "btn btn-outline-danger";
        quitar.textContent = "Quitar";
        quitar.setAttribute("aria-label", `Quitar aporte ${id}`);
        quitar.addEventListener("click", () => fila.remove());
        accion.append(quitar);
        fila.append(responsable, importe, accion);
        document.getElementById("aportes-formulario").append(fila);
        campoResponsable.focus();
    });

    formApertura.addEventListener("submit", accion => {
        accion.preventDefault();
        try {
            const datos = window.eventosSesion.leer();
            const evento = leerEvento(datos);
            if (evento.gestionCaja) throw new Error("La apertura inicial ya fue registrada.");
            const aportes = Array.from(document.querySelectorAll("#aportes-formulario .form-row"), fila => {
                const responsable = fila.querySelector(".responsable-aporte").value.trim();
                if (!responsable) throw new Error("Ingresá el responsable de cada aporte.");
                const importe = window.importes.leer(fila.querySelector(".importe-aporte").value);
                if (importe === 0) throw new Error("Cada aporte debe ser mayor que cero; quitá la fila si no hay aporte.");
                return { responsable, importe };
            });
            const inicial = sumar(...aportes.map(item => item.importe));
            sumar(inicial, ejemplo.efectivo);
            const fecha = new Date().toISOString();
            const accionHistorial = evento.caja === "Abierta" ? "Registro de apertura previa" : "Apertura";
            evento.gestionCaja = {
                aportes,
                ciclos: [{ inicial, ...ejemplo, cierre: null }],
                historial: [{ fecha, accion: accionHistorial, detalle: `Aportes iniciales: ${window.importes.formato(inicial)}.` }]
            };
            evento.caja = "Abierta";
            window.eventosSesion.guardar(datos);
            document.getElementById("aportes-formulario").replaceChildren();
            error.textContent = "";
            mensaje.textContent = "Apertura de ejemplo registrada en esta pestaña.";
            mostrar(evento);
        } catch (excepcion) {
            informarError(excepcion);
        }
    });

    formCierre.addEventListener("submit", accion => {
        accion.preventDefault();
        try {
            const contado = window.importes.leer(document.getElementById("contado").value);
            const datos = window.eventosSesion.leer();
            const evento = leerEvento(datos);
            if (!evento.gestionCaja || evento.caja !== "Abierta") throw new Error("La caja no está abierta con una apertura registrada.");
            const ciclo = evento.gestionCaja.ciclos[evento.gestionCaja.ciclos.length - 1];
            const esperado = sumar(ciclo.inicial, ciclo.efectivo);
            const diferencia = contado - esperado;
            const fecha = new Date().toISOString();
            ciclo.cierre = { esperado, contado, diferencia };
            evento.gestionCaja.historial.push({ fecha, accion: "Cierre", detalle: `Esperado: ${window.importes.formato(esperado)}; contado: ${window.importes.formato(contado)}; diferencia: ${window.importes.formato(diferencia)}.` });
            evento.caja = "Cerrada";
            window.eventosSesion.guardar(datos);
            formCierre.reset();
            error.textContent = "";
            mensaje.textContent = "Cierre de ejemplo registrado en esta pestaña.";
            mostrar(evento);
        } catch (excepcion) {
            informarError(excepcion);
        }
    });

    document.getElementById("reabrir-caja").addEventListener("click", () => {
        try {
            const datos = window.eventosSesion.leer();
            const evento = leerEvento(datos);
            if (!evento.gestionCaja || evento.caja !== "Cerrada") throw new Error("La caja no tiene un cierre para reabrir.");
            const anterior = evento.gestionCaja.ciclos[evento.gestionCaja.ciclos.length - 1];
            if (!anterior.cierre) throw new Error("Falta el efectivo contado del último cierre.");
            const inicial = anterior.cierre.contado;
            evento.gestionCaja.ciclos.push({ inicial, efectivo: 0, transferencia: 0, deuda: 0, cierre: null });
            evento.gestionCaja.historial.push({ fecha: new Date().toISOString(), accion: "Reapertura", detalle: `Base del efectivo contado: ${window.importes.formato(inicial)}.` });
            evento.caja = "Abierta";
            window.eventosSesion.guardar(datos);
            error.textContent = "";
            mensaje.textContent = "Reapertura de ejemplo registrada en esta pestaña.";
            mostrar(evento);
        } catch (excepcion) {
            informarError(excepcion);
        }
    });

    try {
        const datos = window.eventosSesion.leer();
        eventoId = datos.actualId;
        mostrar(leerEvento(datos));
    } catch (excepcion) {
        informarError(excepcion);
        document.querySelectorAll("#form-apertura input, #form-apertura button, #form-cierre input, #form-cierre button, #reabrir-caja")
            .forEach(control => { control.disabled = true; });
    }
})();
