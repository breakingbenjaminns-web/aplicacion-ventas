const fechaActual = document.getElementById("fecha-actual");
const fecha = new Date();

fechaActual.dateTime = fecha.toISOString().slice(0, 10);
fechaActual.textContent = new Intl.DateTimeFormat("es-AR", {
    day: "numeric",
    month: "long",
    year: "numeric"
}).format(fecha);