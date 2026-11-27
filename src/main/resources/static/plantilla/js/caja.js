(function () {
    "use strict";

    const formApertura = document.getElementById("form-apertura");
    const formCierre = document.getElementById("form-cierre");
    const error = document.getElementById("error-caja");
    const mensaje = document.getElementById("mensaje-caja");
    const solicitar = window.api.solicitar;
    let eventoId = null;
    let caja = null;
    let numeroAporte = 0;

    function informarError(excepcion) {
        console.error("No se pudo completar la operación de caja:", excepcion);
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

    function mostrarImporte(id, valor) {
        document.getElementById(id).textContent = valor === null ? "—" : window.importes.formato(valor);
    }

    function mostrar() {
        const periodos = caja.periodos;
        const ultimo = periodos[periodos.length - 1];
        const cierre = [...periodos].reverse().find(item => item.fechaCierre !== null);
        const sinHistorial = caja.id !== null && periodos.length === 0;
        document.getElementById("estado-caja").textContent = `Caja ${caja.estado}`;
        document.getElementById("aviso-caja").textContent = sinHistorial && caja.estado === "abierta"
            ? "Esta caja tiene datos anteriores sin períodos registrados; no puede modificarse automáticamente."
            : "";
        document.getElementById("seccion-apertura").hidden = periodos.length > 0 || caja.estado === "abierta";
        document.getElementById("seccion-cierre").hidden = !ultimo || caja.estado !== "abierta";
        document.getElementById("seccion-reapertura").hidden = !ultimo || caja.estado !== "cerrada";

        const aportes = periodos.length ? periodos[0].aportes : [];
        const total = aportes.reduce((suma, aporte) => suma + BigInt(aporte.importe), 0n);
        mostrarImporte("total-aportes", periodos.length ? total : null);
        mostrarImporte("base-caja", ultimo ? ultimo.saldoInicial : null);
        mostrarImporte("efectivo-esperado", caja.estado === "abierta" ? caja.saldoActual : null);
        document.getElementById("ultimo-cierre").hidden = !cierre;
        if (cierre) {
            mostrarImporte("efectivo-contado", cierre.efectivoContado);
            mostrarImporte("diferencia-cierre", cierre.diferencia);
        }

        const listaAportes = document.getElementById("lista-aportes");
        listaAportes.replaceChildren();
        if (!aportes.length) filaVacia(listaAportes, "No hay aportes iniciales registrados.", 2);
        else aportes.forEach(item => {
            const fila = document.createElement("tr");
            fila.append(celda(item.responsable), celda(window.importes.formato(item.importe)));
            listaAportes.append(fila);
        });

        const historial = document.getElementById("historial-caja");
        historial.replaceChildren();
        if (!periodos.length) filaVacia(historial, "No hay períodos registrados.", 3);
        else [...periodos].reverse().forEach((periodo, indice) => {
            if (periodo.fechaCierre) {
                const fila = document.createElement("tr");
                fila.append(celda(new Date(`${periodo.fechaCierre}Z`).toLocaleString("es-AR")), celda("Cierre"),
                    celda(`Esperado: ${window.importes.formato(periodo.efectivoEsperado)}; ` +
                        `contado: ${window.importes.formato(periodo.efectivoContado)}; ` +
                        `diferencia: ${window.importes.formato(periodo.diferencia)}.`));
                historial.append(fila);
            }
            const apertura = document.createElement("tr");
            const accion = indice === periodos.length - 1 ? "Apertura" : "Reapertura";
            apertura.append(celda(new Date(`${periodo.fechaApertura}Z`).toLocaleString("es-AR")),
                celda(accion), celda(`Base inicial: ${window.importes.formato(periodo.saldoInicial)}.`));
            historial.append(apertura);
        });
    }

    function bloquear(bloqueado) {
        document.querySelectorAll("#form-apertura input, #form-apertura button, #form-cierre input, #form-cierre button, #reabrir-caja")
            .forEach(control => { control.disabled = bloqueado; });
    }

    async function operar(accion, datos, texto) {
        bloquear(true);
        try {
            const opciones = { method: "POST" };
            if (datos !== null) opciones.body = JSON.stringify(datos);
            caja = await solicitar(`/api/cajas/evento/${eventoId}/${accion}`, opciones);
            mostrar();
            error.textContent = "";
            mensaje.textContent = texto;
            window.dispatchEvent(new Event("evento-seleccionado-cambio"));
            return true;
        } catch (excepcion) {
            informarError(excepcion);
            return false;
        } finally {
            bloquear(false);
        }
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

    formApertura.addEventListener("submit", async accion => {
        accion.preventDefault();
        try {
            const aportes = Array.from(document.querySelectorAll("#aportes-formulario .form-row"), fila => {
                const responsable = fila.querySelector(".responsable-aporte").value.trim();
                if (!responsable) throw new Error("Ingresá el responsable de cada aporte.");
                const importe = window.importes.leer(fila.querySelector(".importe-aporte").value);
                if (importe === 0) throw new Error("Cada aporte debe ser mayor que cero; quitá la fila si no hay aporte.");
                return { responsable, importe };
            });
            const total = aportes.reduce((suma, aporte) => suma + BigInt(aporte.importe), 0n);
            if (total > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error("La suma de los aportes supera el máximo permitido.");
            if (await operar("abrir", { aportes }, "Caja abierta.")) {
                document.getElementById("aportes-formulario").replaceChildren();
            }
        } catch (excepcion) {
            informarError(excepcion);
        }
    });

    formCierre.addEventListener("submit", async accion => {
        accion.preventDefault();
        try {
            const contado = window.importes.leer(document.getElementById("contado").value);
            if (await operar("cerrar", { efectivoContado: contado }, "Caja cerrada.")) formCierre.reset();
        } catch (excepcion) {
            informarError(excepcion);
        }
    });

    document.getElementById("reabrir-caja").addEventListener("click", async () => {
        await operar("reabrir", null, "Caja reabierta.");
    });

    (async function iniciar() {
        bloquear(true);
        try {
            const eventos = await solicitar("/api/eventos");
            const idGuardado = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
            const evento = eventos.find(item => item.id === idGuardado) || eventos[0];
            if (!evento) throw new Error("Primero creá y seleccioná un evento en la sección Eventos.");
            eventoId = evento.id;
            document.getElementById("caja-evento").textContent = `Evento: ${evento.nombre}`;
            caja = await solicitar(`/api/cajas/evento/${eventoId}`);
            mostrar();
            bloquear(false);
        } catch (excepcion) {
            informarError(excepcion);
        }
    })();
})();
