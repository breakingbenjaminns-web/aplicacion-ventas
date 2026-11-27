$(document).ready(function () {
    "use strict";

    const form = document.getElementById("form-evento");
    const nombre = document.getElementById("nombre-evento");
    const fecha = document.getElementById("fecha-evento");
    const estadoEvento = document.getElementById("estado-evento");
    const caja = document.getElementById("estado-caja");
    const error = document.getElementById("error-evento");
    const mensaje = document.getElementById("mensaje-evento");
    const tabla = $("#dataTable").DataTable();
    const claveSeleccion = "caja.eventos.seleccionado.v1";
    let eventos = [];
    let editandoId = null;
    let modificado = false;
    let cargado = false;

    const solicitar = window.api.solicitar;

    function agregarCategoria(nombreCategoria) {
        const opciones = form.querySelectorAll('input[name="categoria"]');
        if (Array.from(opciones).some(opcion => opcion.value.toLowerCase() === nombreCategoria.toLowerCase())) return;
        const contenedor = document.createElement("div");
        contenedor.className = "custom-control custom-checkbox custom-control-inline";
        const entrada = document.createElement("input");
        entrada.className = "custom-control-input";
        entrada.type = "checkbox";
        entrada.name = "categoria";
        entrada.value = nombreCategoria;
        entrada.id = `categoria-adicional-${opciones.length}`;
        const etiqueta = document.createElement("label");
        etiqueta.className = "custom-control-label";
        etiqueta.htmlFor = entrada.id;
        etiqueta.textContent = nombreCategoria;
        contenedor.append(entrada, etiqueta);
        document.getElementById("categorias-evento").append(contenedor);
    }

    document.getElementById("crear-categoria").addEventListener("click", async () => {
        const campo = document.getElementById("nueva-categoria");
        if (!campo.value.trim()) {
            error.textContent = "Ingresá el nombre de la categoría.";
            campo.focus();
            return;
        }
        try {
            const categoria = await solicitar("/api/categorias", {
                method: "POST",
                body: JSON.stringify({ nombre: campo.value.trim() })
            });
            agregarCategoria(categoria.nombre);
            campo.value = "";
            error.textContent = "";
            mensaje.textContent = "Categoría creada. Seleccionala para asociarla al evento.";
        } catch (excepcion) {
            mostrarError(excepcion);
        }
    });

    function mostrarError(excepcion) {
        console.error("No se pudieron gestionar los eventos:", excepcion);
        error.textContent = excepcion.message || "No se pudieron cargar o guardar los eventos.";
        mensaje.textContent = "";
    }

    function etiquetaEstado(estado) {
        return ({ pendiente: "Pendiente", activo: "Abierto", cerrado: "Cerrado", cancelado: "Cancelado" })[estado] || estado;
    }

    function descartarCambios() {
        return !modificado || window.confirm("Hay cambios sin guardar. ¿Querés descartarlos?");
    }

    function guardarSeleccion(id) {
        if (id === null) {
            sessionStorage.removeItem(claveSeleccion);
        } else {
            sessionStorage.setItem(claveSeleccion, String(id));
        }
        window.dispatchEvent(new Event("evento-seleccionado-cambio"));
    }

    function llenarFormulario(evento) {
        form.reset();
        caja.textContent = evento ? evento.caja : "Cerrada";
        editandoId = evento ? evento.id : null;
        document.getElementById("titulo-formulario").textContent = evento ? `Editar evento: ${evento.nombre}` : "Nuevo evento";
        if (evento) {
            nombre.value = evento.nombre;
            fecha.value = evento.fecha;
            estadoEvento.value = evento.estado;
            form.querySelectorAll('input[name="categoria"]').forEach(opcion => {
                opcion.checked = evento.categorias.some(item => item.toLowerCase() === opcion.value.toLowerCase());
            });
        }
        modificado = false;
        error.textContent = "";
        mensaje.textContent = "";
    }

    function celda(texto) {
        const td = document.createElement("td");
        td.textContent = texto;
        return td;
    }

    function mostrarTabla() {
        const idSeleccionado = Number(sessionStorage.getItem(claveSeleccion));
        tabla.clear();
        eventos.forEach(evento => {
            const fila = document.createElement("tr");
            const fechaCelda = celda(evento.fecha.split("-").reverse().join("/"));
            fila.append(
                celda(evento.nombre),
                fechaCelda,
                celda(evento.categorias.join(", ")),
                celda(etiquetaEstado(evento.estado)),
                celda(`Caja ${evento.caja.toLowerCase()}`));
            const accion = document.createElement("td");
            const boton = document.createElement("button");
            const seleccionado = evento.id === idSeleccionado;
            boton.type = "button";
            boton.className = "btn btn-sm " + (seleccionado ? "btn-primary" : "btn-outline-primary");
            boton.textContent = seleccionado ? "Seleccionado" : "Seleccionar";
            boton.setAttribute("aria-label", `Seleccionar ${evento.nombre}`);
            boton.setAttribute("aria-pressed", String(seleccionado));
            boton.addEventListener("click", () => {
                if (!descartarCambios()) return;
                guardarSeleccion(evento.id);
                llenarFormulario(evento);
                mostrarTabla();
            });
            accion.append(boton);
            fila.append(accion);
            tabla.row.add(fila);
        });
        tabla.draw(false);
    }

    async function cargarEventos(seleccionId) {
        eventos = await solicitar("/api/eventos");
        const idActual = eventos.some(evento => evento.id === seleccionId)
            ? seleccionId
            : eventos.length ? eventos[0].id : null;
        guardarSeleccion(idActual);
        mostrarTabla();
        return eventos.find(evento => evento.id === idActual) || null;
    }

    form.addEventListener("input", () => { modificado = true; });
    form.addEventListener("change", () => { modificado = true; });
    document.getElementById("nuevo-evento").addEventListener("click", () => {
        if (descartarCambios()) llenarFormulario(null);
    });

    form.addEventListener("submit", async evento => {
        evento.preventDefault();
        const categorias = Array.from(form.querySelectorAll('input[name="categoria"]:checked'), opcion => opcion.value);
        if (!nombre.value.trim()) {
            error.textContent = "Ingresá el nombre del evento.";
            nombre.focus();
            return;
        }
        if (!fecha.value) {
            error.textContent = "Seleccioná la fecha del evento.";
            fecha.focus();
            return;
        }
        if (!categorias.length) {
            error.textContent = "Seleccioná al menos una categoría de venta.";
            return;
        }

        const datos = {
            nombre: nombre.value.trim(),
            fecha: fecha.value,
            categorias,
            estado: estadoEvento.value
        };
        const botonGuardar = form.querySelector('button[type="submit"]');
        const eraEdicion = editandoId !== null;
        botonGuardar.disabled = true;
        try {
            const guardado = await solicitar(
                editandoId === null ? "/api/eventos" : `/api/eventos/${editandoId}`,
                {
                    method: editandoId === null ? "POST" : "PUT",
                    body: JSON.stringify(datos)
                });
            const indice = eventos.findIndex(item => item.id === guardado.id);
            if (indice === -1) eventos.unshift(guardado);
            else eventos[indice] = guardado;
            guardarSeleccion(guardado.id);
            mostrarTabla();
            llenarFormulario(guardado);
            mensaje.textContent = eraEdicion ? "Evento actualizado correctamente." : "Evento guardado correctamente.";
        } catch (excepcion) {
            mostrarError(excepcion);
        } finally {
            botonGuardar.disabled = !cargado;
        }
    });

    (async function iniciar() {
        form.querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
        try {
            const categorias = await solicitar("/api/categorias");
            categorias.forEach(categoria => agregarCategoria(categoria.nombre));
            const valorGuardado = Number(sessionStorage.getItem(claveSeleccion));
            const seleccionado = await cargarEventos(Number.isSafeInteger(valorGuardado) ? valorGuardado : null);
            llenarFormulario(seleccionado);
            cargado = true;
            form.querySelectorAll("input, select, button").forEach(control => { control.disabled = false; });
            document.getElementById("nuevo-evento").disabled = false;
        } catch (excepcion) {
            mostrarError(excepcion);
        }
    })();
});
