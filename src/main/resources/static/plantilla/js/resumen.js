(function () {
    "use strict";

    function mostrarResumen() {
        const campos = {
            nombre: document.getElementById("resumen-nombre"),
            fecha: document.getElementById("resumen-fecha"),
            estado: document.getElementById("resumen-estado"),
            caja: document.getElementById("resumen-caja"),
            categorias: document.getElementById("resumen-categorias"),
            productos: document.getElementById("resumen-productos")
        };
        const error = document.getElementById("error-resumen");
        try {
            const datos = window.eventosSesion.leer();
            const evento = datos.eventos.find(item => item.id === datos.actualId);
            campos.nombre.textContent = evento.nombre;
            campos.fecha.textContent = evento.fecha.split("-").reverse().join("/");
            campos.estado.textContent = evento.estado;
            campos.caja.textContent = evento.caja;
            campos.categorias.textContent = evento.categorias.join(", ") || "Sin categorías";
            campos.productos.textContent = String(evento.productos.length);
            error.textContent = "";
        } catch (excepcion) {
            console.error("No se pudo cargar el resumen del evento de demostración:", excepcion);
            Object.values(campos).forEach(campo => { campo.textContent = "—"; });
            error.textContent = "No se pudo cargar el evento de ejemplo en esta pestaña.";
        }
    }

    window.addEventListener("eventos-sesion-cambiaron", mostrarResumen);
    mostrarResumen();
})();
