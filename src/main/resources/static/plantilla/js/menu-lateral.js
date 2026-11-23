(function () {
    "use strict";

    const script = document.currentScript;
    script.insertAdjacentHTML("beforebegin", `
        <ul class="navbar-nav bg-gradient-primary sidebar sidebar-dark accordion" id="accordionSidebar">
            <a class="sidebar-brand d-flex align-items-center justify-content-center" href="index.html">
                <div class="sidebar-brand-icon rotate-n-15">
                    <i class="fas fa-laugh-wink"></i>
                </div>
                <div class="sidebar-brand-text mx-3">SB Admin <sup>2</sup></div>
            </a>

            <hr class="sidebar-divider my-0">
            <div class="sidebar-heading">Inicio</div>
            <li class="nav-item">
                <a class="nav-link" href="index.html">
                    <i class="fas fa-fw fa-tachometer-alt"></i>
                    <span>Resumen</span></a>
            </li>

            <hr class="sidebar-divider">
            <div class="sidebar-heading">Evento</div>
            <li class="nav-item">
                <a class="nav-link" href="eventos.html">
                    <i class="fas fa-fw fa-calendar-alt"></i>
                    <span>Eventos</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="productos.html">
                    <i class="fas fa-fw fa-box"></i>
                    <span>Productos</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="caja.html">
                    <i class="fas fa-fw fa-cash-register"></i>
                    <span>Caja</span></a>
            </li>

            <hr class="sidebar-divider">
            <div class="sidebar-heading">Ventas</div>
            <li class="nav-item">
                <a class="nav-link" href="ingresar-venta.html">
                    <i class="fas fa-fw fa-shopping-cart"></i>
                    <span>Nueva venta</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="tickets.html">
                    <i class="fas fa-fw fa-receipt"></i>
                    <span>Tickets</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="deuda.html">
                    <i class="fas fa-fw fa-hand-holding-usd"></i>
                    <span>Deudas</span></a>
            </li>

            <hr class="sidebar-divider">
            <div class="sidebar-heading">Informes</div>
            <li class="nav-item">
                <a class="nav-link" href="resumen-ventas.html">
                    <i class="fas fa-fw fa-chart-pie"></i>
                    <span>Resumen de ventas</span></a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="copias-seguridad.html">
                    <i class="fas fa-fw fa-database"></i>
                    <span>Copias de seguridad</span></a>
            </li>

            <hr class="sidebar-divider d-none d-md-block">
            <div class="text-center d-none d-md-inline">
                <button class="rounded-circle border-0" id="sidebarToggle" type="button" aria-label="Alternar menú lateral"></button>
            </div>
        </ul>
    `);

    const pagina = window.location.pathname.split("/").pop() || "index.html";
    const enlace = Array.from(document.querySelectorAll("#accordionSidebar .nav-link"))
        .find(function (opcion) { return opcion.getAttribute("href") === pagina; });
    if (enlace) {
        enlace.parentElement.classList.add("active");
        enlace.setAttribute("aria-current", "page");
    }
})();
