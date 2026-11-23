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
    let editandoId = null;
    let modificado = false;

    function mostrarError(excepcion) {
        console.error("No se pudieron gestionar los eventos de demostración:", excepcion);
        error.textContent = "No se pudieron guardar o cargar los eventos de ejemplo en esta pestaña.";
        mensaje.textContent = "";
    }

    function descartarCambios() {
        return !modificado || window.confirm("Hay cambios sin guardar. ¿Querés descartarlos?");
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
                opcion.checked = evento.categorias.includes(opcion.value);
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

    function mostrarTabla(estado) {
        tabla.clear();
        estado.eventos.forEach(evento => {
            const fila = document.createElement("tr");
            const fechaCelda = celda(evento.fecha.split("-").reverse().join("/"));
            fila.append(celda(evento.nombre), fechaCelda, celda(evento.categorias.join(", ")),
                celda(evento.estado), celda(`Caja ${evento.caja.toLowerCase()}`));
            const accion = document.createElement("td");
            const boton = document.createElement("button");
            boton.type = "button";
            boton.className = "btn btn-sm " + (evento.id === estado.actualId ? "btn-primary" : "btn-outline-primary");
            boton.textContent = evento.id === estado.actualId ? "Seleccionado" : "Seleccionar";
            boton.setAttribute("aria-label", `Seleccionar ${evento.nombre}`);
            boton.setAttribute("aria-pressed", String(evento.id === estado.actualId));
            boton.addEventListener("click", () => {
                if (!descartarCambios()) return;
                try {
                    const datos = window.eventosSesion.leer();
                    datos.actualId = evento.id;
                    window.eventosSesion.guardar(datos);
                    llenarFormulario(datos.eventos.find(item => item.id === evento.id));
                    mostrarTabla(datos);
                } catch (excepcion) {
                    mostrarError(excepcion);
                }
            });
            accion.append(boton);
            fila.append(accion);
            tabla.row.add(fila);
        });
        tabla.draw(false);
    }

    form.addEventListener("input", () => { modificado = true; });
    form.addEventListener("change", () => { modificado = true; });
    document.getElementById("nuevo-evento").addEventListener("click", () => {
        if (descartarCambios()) llenarFormulario(null);
    });

    form.addEventListener("submit", evento => {
        evento.preventDefault();
        const categorias = Array.from(form.querySelectorAll('input[name="categoria"]:checked'), opcion => opcion.value);
        if (!nombre.value.trim()) {
            error.textContent = "Ingresá el nombre del evento.";
            nombre.focus();
            return;
        }
        if (!categorias.length) {
            error.textContent = "Seleccioná al menos una categoría de venta.";
            return;
        }
        try {
            const datos = window.eventosSesion.leer();
            const id = editandoId === null ? Math.max(0, ...datos.eventos.map(item => item.id)) + 1 : editandoId;
            const indice = datos.eventos.findIndex(item => item.id === id);
            if (editandoId !== null && indice === -1) throw new Error("El evento seleccionado ya no existe.");
            const productos = indice === -1 ? [] : datos.eventos[indice].productos;
            const categoriasEnUso = [...new Set(productos.filter(producto => !categorias.includes(producto.categoria))
                .map(producto => producto.categoria))];
            if (categoriasEnUso.length) {
                error.textContent = `No podés quitar categorías con productos: ${categoriasEnUso.join(", ")}.`;
                mensaje.textContent = "";
                return;
            }
            const registro = { ...(indice === -1 ? {} : datos.eventos[indice]), id, nombre: nombre.value.trim(),
                fecha: fecha.value, categorias, estado: estadoEvento.value,
                caja: indice === -1 ? "Cerrada" : datos.eventos[indice].caja, productos };
            if (indice === -1) datos.eventos.push(registro);
            else datos.eventos[indice] = registro;
            datos.actualId = id;
            window.eventosSesion.guardar(datos);
            llenarFormulario(registro);
            mostrarTabla(datos);
            mensaje.textContent = "Evento de ejemplo guardado en esta pestaña.";
        } catch (excepcion) {
            mostrarError(excepcion);
        }
    });

    try {
        mostrarTabla(window.eventosSesion.leer());
    } catch (excepcion) {
        mostrarError(excepcion);
        form.querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
    }
});
