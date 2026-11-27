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
    const solicitar = window.api.solicitar;
    let eventoId = null;
    let productos = [];
    let editandoId = null;
    let modificado = false;

    function mostrarError(excepcion) {
        console.error("No se pudieron gestionar los productos:", excepcion);
        error.textContent = excepcion.message || "No se pudieron cargar o guardar los productos.";
        mensaje.textContent = "";
    }

    function descartarCambios() {
        return !modificado || window.confirm("Hay cambios sin guardar. ¿Querés descartarlos?");
    }

    function llenarFormulario(producto) {
        form.reset();
        editandoId = producto ? producto.id : null;
        document.getElementById("titulo-formulario").textContent = producto
            ? `Editar producto: ${producto.nombre}` : "Nuevo producto";
        if (producto) {
            nombre.value = producto.nombre;
            categoria.value = String(producto.categoriaId);
            precio.value = window.importes.texto(producto.precio);
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

    function actualizarProducto(producto) {
        const indice = productos.findIndex(item => item.id === producto.id);
        if (indice === -1) productos.unshift(producto);
        else productos[indice] = producto;
    }

    function mostrarTabla() {
        tabla.clear();
        productos.forEach(producto => {
            const fila = document.createElement("tr");
            fila.append(celda(producto.nombre), celda(producto.categoria),
                celda(window.importes.formato(producto.precio)),
                celda(producto.stock === null ? "Sin control" : String(producto.stock)));
            const acciones = document.createElement("td");
            const seleccionar = document.createElement("button");
            seleccionar.type = "button";
            seleccionar.className = "btn btn-sm btn-outline-primary mr-2";
            seleccionar.textContent = "Seleccionar";
            seleccionar.setAttribute("aria-label", `Seleccionar ${producto.nombre}`);
            seleccionar.addEventListener("click", () => {
                if (descartarCambios()) llenarFormulario(producto);
            });
            const activar = document.createElement("button");
            activar.type = "button";
            activar.className = "btn btn-sm " + (producto.activo ? "btn-outline-danger" : "btn-outline-success");
            activar.textContent = producto.activo ? "Desactivar" : "Activar";
            activar.setAttribute("aria-label", `${activar.textContent} ${producto.nombre}`);
            activar.addEventListener("click", async () => {
                if (!window.confirm(`¿Querés ${producto.activo ? "desactivar" : "activar"} «${producto.nombre}»?`)) return;
                activar.disabled = true;
                try {
                    const actualizado = await solicitar(`/api/productos/${producto.id}/activo`, {
                        method: "PATCH",
                        body: JSON.stringify({ activo: !producto.activo })
                    });
                    actualizarProducto(actualizado);
                    mostrarTabla();
                    error.textContent = "";
                    mensaje.textContent = actualizado.activo ? "Producto activado." : "Producto desactivado.";
                } catch (excepcion) {
                    mostrarError(excepcion);
                    activar.disabled = false;
                }
            });
            acciones.append(seleccionar, activar);
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

    form.addEventListener("submit", async evento => {
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
            mostrarError(excepcion);
            precio.focus();
            return;
        }
        if (cantidad !== null && (!Number.isSafeInteger(cantidad) || cantidad < 0)) {
            error.textContent = "Ingresá un stock entero de cero o más, o dejalo vacío.";
            stock.focus();
            return;
        }
        const anterior = productos.find(item => item.id === editandoId);
        const datos = {
            nombre: nombre.value.trim(),
            descripcion: anterior ? anterior.descripcion : null,
            precio: centavos,
            stock: cantidad,
            categoriaId: Number(categoria.value),
            eventoId
        };
        const eraEdicion = editandoId !== null;
        const botonGuardar = form.querySelector('button[type="submit"]');
        botonGuardar.disabled = true;
        try {
            const guardado = await solicitar(
                eraEdicion ? `/api/productos/${editandoId}` : "/api/productos",
                { method: eraEdicion ? "PUT" : "POST", body: JSON.stringify(datos) });
            actualizarProducto(guardado);
            llenarFormulario(guardado);
            mostrarTabla();
            mensaje.textContent = eraEdicion ? "Producto actualizado." : "Producto creado.";
        } catch (excepcion) {
            mostrarError(excepcion);
        } finally {
            botonGuardar.disabled = false;
        }
    });

    (async function iniciar() {
        form.querySelectorAll("input, select, button").forEach(control => { control.disabled = true; });
        try {
            const eventos = await solicitar("/api/eventos");
            const idGuardado = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
            const actual = eventos.find(item => item.id === idGuardado) || eventos[0];
            if (!actual) throw new Error("Primero creá y seleccioná un evento en la sección Eventos.");
            eventoId = actual.id;
            const categorias = await solicitar(`/api/categorias?eventoId=${eventoId}`);
            if (!categorias.length) throw new Error("El evento no tiene categorías asociadas.");
            categorias.forEach(item => {
                const opcion = document.createElement("option");
                opcion.value = String(item.id);
                opcion.textContent = item.nombre;
                categoria.append(opcion);
            });
            productos = await solicitar(`/api/productos?eventoId=${eventoId}`);
            document.getElementById("evento-productos").textContent = `Evento: ${actual.nombre}`;
            mostrarTabla();
            form.querySelectorAll("input, select, button").forEach(control => { control.disabled = false; });
        } catch (excepcion) {
            mostrarError(excepcion);
        }
    })();
});
