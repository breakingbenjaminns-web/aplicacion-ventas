(function () {
    "use strict";

    document.currentScript.insertAdjacentHTML("beforebegin", `
        <nav class="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">
            <button id="sidebarToggleTop" class="btn btn-link d-md-none rounded-circle mr-3"
                type="button" aria-label="Alternar menú lateral">
                <i class="fa fa-bars"></i>
            </button>
            <a href="eventos.html" class="d-flex flex-column flex-sm-row align-items-sm-center small text-gray-600 overflow-hidden mr-2"
                style="min-width: 0" title="Ver eventos">
                <span id="evento-actual" class="text-truncate"></span>
                <span class="d-none d-sm-inline mx-2" aria-hidden="true">|</span>
                <span id="caja-actual" class="text-nowrap"></span>
            </a>
            <ul class="navbar-nav ml-auto">
                <li class="nav-item dropdown no-arrow">
                    <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button"
                        data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <span class="mr-2 text-gray-600 small" id="usuario-actual">Usuario</span>
                        <img class="img-profile rounded-circle" src="img/undraw_profile.svg" alt="">
                    </a>
                    <div class="dropdown-menu dropdown-menu-right shadow animated--grow-in"
                        aria-labelledby="userDropdown">
                        <a class="dropdown-item" href="actividad.html">
                            <i class="fas fa-history fa-sm fa-fw mr-2 text-gray-400"></i>
                            Actividad
                        </a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="/logout">
                            <i class="fas fa-sign-out-alt fa-sm fa-fw mr-2 text-gray-400"></i>
                            Cerrar sesión
                        </a>
                    </div>
                </li>
            </ul>
        </nav>
    `);

    function mostrarEventoActual() {
        const nombre = document.getElementById("evento-actual");
        const caja = document.getElementById("caja-actual");
        const idGuardado = Number(sessionStorage.getItem("caja.eventos.seleccionado.v1"));
        fetch("/api/eventos", { headers: { Accept: "application/json" }, credentials: "same-origin" })
            .then(respuesta => {
                if (!respuesta.ok || !(respuesta.headers.get("content-type") || "").includes("application/json")) {
                    throw new Error(`No se pudieron cargar los eventos (HTTP ${respuesta.status}).`);
                }
                return respuesta.json();
            })
            .then(eventos => {
                const evento = eventos.find(item => item.id === idGuardado) || eventos[0];
                if (!evento) {
                    nombre.textContent = "Sin eventos registrados";
                    caja.textContent = "Caja cerrada";
                    return;
                }
                const estado = ({ pendiente: "Pendiente", activo: "Abierto", cerrado: "Cerrado", cancelado: "Cancelado" })[evento.estado];
                nombre.textContent = `${evento.nombre} (${estado})`;
                caja.textContent = `Caja ${evento.caja.toLowerCase()}`;
            })
            .catch(error => {
                console.error("No se pudo cargar el evento actual:", error);
                nombre.textContent = "No se pudo cargar el evento";
                caja.textContent = "";
            });
    }

    window.addEventListener("evento-seleccionado-cambio", mostrarEventoActual);
    fetch("/api/sesion", { headers: { Accept: "application/json" }, credentials: "same-origin" })
        .then(respuesta => {
            if (!respuesta.ok || !(respuesta.headers.get("content-type") || "").includes("json")) {
                throw new Error("No se pudo consultar la sesión.");
            }
            return respuesta.json();
        })
        .then(sesion => { document.getElementById("usuario-actual").textContent = sesion.nombre; })
        .catch(error => {
            console.error("No se pudo consultar la sesión:", error);
            document.getElementById("usuario-actual").textContent = "Sesión no disponible";
        });
    mostrarEventoActual();
})();
