(async function () {
    "use strict";
    try {
        const actividad = await window.api.solicitar("/api/actividad");
        const cuerpo = document.getElementById("actividad");
        actividad.forEach(registro => {
            const fila = document.createElement("tr");
            [registro.fecha, registro.usuario, registro.accion, registro.entidad, registro.entidadId].forEach(valor => {
                const celda = document.createElement("td");
                celda.textContent = String(valor);
                fila.append(celda);
            });
            cuerpo.append(fila);
        });
    } catch (error) {
        console.error("No se pudo cargar la actividad:", error);
        document.getElementById("error-actividad").textContent = error.message;
    }
})();
