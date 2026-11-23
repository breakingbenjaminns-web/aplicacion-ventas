(function () {
    "use strict";

    const clave = "caja.eventos.demostracion.v1";
    const ejemplos = [
        { id: 1, nombre: "Peña de ejemplo", fecha: "2026-03-14", categorias: ["Comida", "Bebida"], estado: "Abierto", caja: "Abierta",
            productos: [{ id: 1, nombre: "Gaseosa", categoria: "Bebida", stock: 30, precioCentavos: 250000 }, { id: 2, nombre: "Empanada", categoria: "Comida", stock: null, precioCentavos: 200000 }] },
        { id: 2, nombre: "Bingo de ejemplo", fecha: "2026-02-21", categorias: ["Bebida", "Productos"], estado: "Cerrado", caja: "Cerrada",
            productos: [{ id: 1, nombre: "Agua", categoria: "Bebida", stock: 25, precioCentavos: 150000 }] },
        { id: 3, nombre: "Feria de ejemplo", fecha: "2026-01-17", categorias: ["Comida", "Productos"], estado: "Cancelado", caja: "Cerrada",
            productos: [{ id: 1, nombre: "Artesanía", categoria: "Productos", stock: null, precioCentavos: 120000 }] }
    ];

    function leer() {
        const datos = sessionStorage.getItem(clave);
        const estado = datos === null
            ? { eventos: ejemplos.map(evento => ({ ...evento, categorias: [...evento.categorias], productos: evento.productos.map(producto => ({ ...producto })) })), actualId: 1 }
            : JSON.parse(datos);
        if (!estado || !Array.isArray(estado.eventos) || !estado.eventos.some(evento => evento.id === estado.actualId) ||
            estado.eventos.some(evento => !Number.isSafeInteger(evento.id) || typeof evento.nombre !== "string" ||
                typeof evento.fecha !== "string" || !Array.isArray(evento.categorias) ||
                !["Abierto", "Cerrado", "Cancelado"].includes(evento.estado) || !["Abierta", "Cerrada"].includes(evento.caja))) {
            throw new Error("Los datos de eventos de demostración no son válidos.");
        }
        estado.eventos.forEach(evento => {
            if (evento.productos === undefined) evento.productos = [];
            if (!Array.isArray(evento.productos) || evento.productos.some(producto =>
                !Number.isSafeInteger(producto.id) || producto.id < 1 || typeof producto.nombre !== "string" ||
                !evento.categorias.includes(producto.categoria) ||
                !(producto.stock === null || (Number.isSafeInteger(producto.stock) && producto.stock >= 0)) ||
                !(producto.precioCentavos === undefined || producto.precioCentavos === null ||
                    (Number.isSafeInteger(producto.precioCentavos) && producto.precioCentavos >= 0)))) {
                throw new Error("Los productos de demostración no son válidos.");
            }
            evento.productos.forEach(producto => {
                if (producto.precioCentavos === undefined) producto.precioCentavos = null;
            });
        });
        return estado;
    }

    function guardar(estado) {
        sessionStorage.setItem(clave, JSON.stringify(estado));
        window.dispatchEvent(new Event("eventos-sesion-cambiaron"));
    }

    window.eventosSesion = Object.freeze({ leer, guardar });
})();
