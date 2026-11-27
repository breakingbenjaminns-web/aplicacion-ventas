(function () {
    "use strict";

    async function solicitar(url, opciones = {}) {
        const headers = new Headers(opciones.headers || {});
        headers.set("Accept", "application/json");
        if (opciones.body) headers.set("Content-Type", "application/json");
        if (opciones.method && opciones.method !== "GET") {
            const cookie = document.cookie.split("; ").find(parte => parte.startsWith("XSRF-TOKEN="));
            if (!cookie) throw new Error("Falta el token de seguridad. Recargá la página e intentá nuevamente.");
            headers.set("X-XSRF-TOKEN", decodeURIComponent(cookie.substring("XSRF-TOKEN=".length)));
        }
        const respuesta = await fetch(url, { ...opciones, headers, credentials: "same-origin" });
        const tipo = respuesta.headers.get("content-type") || "";
        if (!tipo.includes("json")) {
            if (respuesta.redirected) throw new Error("La sesión expiró. Iniciá sesión y volvé a cargar la página.");
            throw new Error(`El servidor devolvió una respuesta inesperada (HTTP ${respuesta.status}).`);
        }
        const contenido = await respuesta.json();
        if (!respuesta.ok) {
            throw new Error(contenido.detail || contenido.message || contenido.title ||
                `No se pudo completar la operación (HTTP ${respuesta.status}).`);
        }
        return contenido;
    }

    window.api = Object.freeze({ solicitar });
})();
