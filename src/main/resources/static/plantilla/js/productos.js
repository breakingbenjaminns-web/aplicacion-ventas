$(document).ready(function () {
    "use strict";

    const form = document.getElementById("form-producto");
    const nombre = document.getElementById("nombre-producto");
    const categoria = document.getElementById("categoria-producto");
    const precio = document.getElementById("precio-producto");
    const stock = document.getElementById("stock-producto");
    const error = document.getElementById("error-producto");
    const mensaje = document.getElementById("mensaje-producto");
    const tabla = $("#dataTable").DataTable();
    let eventoId;
    let editandoId = null;
    let modificado = false;

    function mostrarError(excepcion) {
        console.error("No se pudieron gestionar los productos de demostración:", excepcion);
        error.textContent = "No se pudieron guardar o cargar los productos de ejemplo en esta pestaña.";
        mensaje.textContent = "";
    }

    function eventoActual(datos) {
        if (datos.actualId !== eventoId) throw new Error("Cambió el evento seleccionado; recargá la página.");
        const evento = datos.eventos.find(item => item.id === eventoId);
        if (!evento) throw new Error("El evento seleccionado ya no existe.");
        return evento;
    }

    function descartarCambios() {
        return !modificado || window.confirm("Hay cambios sin guardar. ¿Querés descartarlos?");
    }

    function llenarFormulario(producto) {
        form.reset();
        editandoId = producto ? producto.id : null;
        document.getElementById("titulo-formulario").textContent = producto ? `Editar producto: ${producto.nombre}` : "Nuevo producto";
        if (producto) {
            nombre.value = producto.nombre;
            categoria.value = producto.categoria;
            precio.value = producto.precioCentavos === null ? "" : window.importes.texto(producto.precioCentavos);
            stock.value = producto.stock === null ? "" : producto.stock;
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

    function mostrarTabla(evento) {
        tabla.clear();
        evento.productos.forEach(producto => {
            const fila = document.createElement("tr");
            fila.append(celda(producto.nombre), celda(producto.categoria),
                celda(producto.precioCentavos === null ? "Sin precio" : window.importes.formato(producto.precioCentavos)),
                celda(producto.stock === null ? "Sin control" : String(producto.stock)));
            const acciones = document.createElement("td");
            const seleccionar = document.createElement("button");
            seleccionar.type = "button";
            seleccionar.className = "btn btn-sm btn-outline-primary mr-2";
            seleccionar.textContent = "Seleccionar";
            seleccionar.setAttribute("aria-label", `Seleccionar ${producto.nombre}`);
            seleccionar.addEventListener("click", () => {
                if (!descartarCambios()) return;
                llenarFormulario(producto);
            });
            const borrar = document.createElement("button");
            borrar.type = "button";
            borrar.className = "btn btn-sm btn-outline-danger";
            borrar.textContent = "Borrar";
            borrar.setAttribute("aria-label", `Borrar ${producto.nombre}`);
            borrar.addEventListener("click", () => {
                const aviso = editandoId === producto.id && modificado
                    ? " Se descartarán los cambios sin guardar de este producto."
                    : "";
                if (!window.confirm(`¿Querés borrar el producto de ejemplo «${producto.nombre}»?${aviso}`)) return;
                try {
                    const datos = window.eventosSesion.leer();
                    const actual = eventoActual(datos);
                    const indice = actual.productos.findIndex(item => item.id === producto.id);
                    if (indice === -1) throw new Error("El producto seleccionado ya no existe.");
                    actual.productos.splice(indice, 1);
                    window.eventosSesion.guardar(datos);
                    if (editandoId === producto.id) llenarFormulario(null);
                    mostrarTabla(actual);
                    error.textContent = "";
                    mensaje.textContent = "Producto de ejemplo borrado en esta pestaña.";
                } catch (excepcion) {
                    mostrarError(excepcion);
                }
            });
            acciones.append(seleccionar, borrar);
            fila.append(acciones);
            tabla.row.add(fila);
        });
        tabla.draw(false);
    }

    form.addEventListener("input", () => { modificado = true; });
    form.addEventListener("change", () => { modificado = true; });
    document.getElementById("nuevo-producto").addEventListener("click", () => {
        if (descartarCambios()) llenarFormulario(null);
    });

    form.addEventListener("submit", evento => {
        evento.preventDefault();
        const cantidad = stock.value === "" ? null : Number(stock.value);
        if (!nombre.value.trim()) {
            error.textContent = "Ingresá el nombre del producto.";
            nombre.focus();
            return;
        }
        let centavos;
        try {
            centavos = window.importes.leer(precio.value);
        } catch (excepcion) {
            error.textContent = excepcion.message;
            mensaje.textContent = "";
            precio.focus();
            return;
        }
        if (cantidad !== null && (!Number.isSafeInteger(cantidad) || cantidad < 0)) {
            error.textContent = "Ingresá un stock entero de cero o más, o dejalo vacío.";
            stock.focus();
            return;
        }
        try {
            const datos = window.eventosSesion.leer();
            const actual = eventoActual(datos);
            if (!actual.categorias.includes(categoria.value)) {
                error.textContent = "Seleccioná una categoría del evento.";
                return;
            }
            const id = editandoId === null ? Math.max(0, ...actual.productos.map(item => item.id)) + 1 : editandoId;
            const indice = actual.productos.findIndex(item => item.id === id);
            if (editandoId !== null && indice === -1) throw new Error("El producto seleccionado ya no existe.");
            const registro = { id, nombre: nombre.value.trim(), categoria: categoria.value, precioCentavos: centavos, stock: cantidad };
            if (indice === -1) actual.productos.push(registro);
            else actual.productos[indice] = registro;
            window.eventosSesion.guardar(datos);
            llenarFormulario(registro);
            mostrarTabla(actual);
            mensaje.textContent = "Producto de ejemplo guardado en esta pestaña.";
        } catch (excepcion) {
            mostrarError(excepcion);
        }
    });

    try {
        const datos = window.eventosSesion.leer();
        eventoId = datos.actualId;
        const actual = eventoActual(datos);
        document.getElementById("evento-productos").textContent = `Evento de ejemplo: ${actual.nombre} (${actual.estado})`;
        actual.categorias.forEach(item => {
            const opcion = document.createElement("option");
            opcion.value = item;
            opcion.textContent = item;
            categoria.append(opcion);
        });
        mostrarTabla(actual);
    } catch (excepcion) {
        mostrarError(excepcion);
        form.querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
    }
});
