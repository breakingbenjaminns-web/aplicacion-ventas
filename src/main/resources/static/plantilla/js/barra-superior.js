(function () {
    "use strict";

    document.currentScript.insertAdjacentHTML("beforebegin", `
        <nav class="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">
            <button id="sidebarToggleTop" class="btn btn-link d-md-none rounded-circle mr-3"
                type="button" aria-label="Alternar menú lateral">
                <i class="fa fa-bars"></i>
            </button>
            <a href="eventos.html" class="d-flex flex-column flex-sm-row align-items-sm-center small text-gray-600 overflow-hidden mr-2"
                style="min-width: 0" title="Ver eventos de demostración">
                <span id="evento-actual" class="text-truncate"></span>
                <span class="d-none d-sm-inline mx-2" aria-hidden="true">|</span>
                <span id="caja-actual" class="text-nowrap"></span>
            </a>
            <ul class="navbar-nav ml-auto">
                <li class="nav-item dropdown no-arrow">
                    <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button"
                        data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <span class="mr-2 text-gray-600 small">Usuario</span>
                        <img class="img-profile rounded-circle" src="img/undraw_profile.svg" alt="">
                    </a>
                    <div class="dropdown-menu dropdown-menu-right shadow animated--grow-in"
                        aria-labelledby="userDropdown">
                        <span class="dropdown-item disabled">
                            <i class="fas fa-cogs fa-sm fa-fw mr-2 text-gray-400"></i>
                            Configuración (próximamente)
                        </span>
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
        try {
            const estado = window.eventosSesion.leer();
            const evento = estado.eventos.find(item => item.id === estado.actualId);
            nombre.textContent = `Evento de ejemplo: ${evento.nombre} (${evento.estado})`;
            caja.textContent = `Caja ${evento.caja.toLowerCase()}`;
        } catch (error) {
            console.error("No se pudo leer el evento de demostración:", error);
            nombre.textContent = "No se pudo cargar el evento";
            caja.textContent = "";
        }
    }

    window.addEventListener("eventos-sesion-cambiaron", mostrarEventoActual);
    mostrarEventoActual();
})();
