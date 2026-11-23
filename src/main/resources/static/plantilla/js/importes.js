(function () {
    "use strict";

    function leer(texto) {
        const partes = texto.trim().match(/^(\d+)(?:[.,](\d{1,2}))?$/);
        if (!partes) throw new Error("Ingresá un importe no negativo con hasta dos decimales, sin separadores de miles.");
        const enteros = partes[1].replace(/^0+/, "") || "0";
        if (enteros.length > 14) throw new Error("El importe supera el máximo permitido para la demostración.");
        const centavos = BigInt(enteros) * 100n + BigInt((partes[2] || "").padEnd(2, "0"));
        if (centavos > BigInt(Number.MAX_SAFE_INTEGER)) {
            throw new Error("El importe supera el máximo permitido para la demostración.");
        }
        return Number(centavos);
    }

    function texto(centavos) {
        const importe = BigInt(centavos);
        return `${importe / 100n},${String(importe % 100n).padStart(2, "0")}`;
    }

    function formato(centavos) {
        let importe = BigInt(centavos);
        const signo = importe < 0n ? "-" : "";
        if (importe < 0n) importe = -importe;
        const enteros = new Intl.NumberFormat("es-AR").format(importe / 100n);
        return `${signo}$ ${enteros},${String(importe % 100n).padStart(2, "0")}`;
    }

    window.importes = Object.freeze({ leer, texto, formato });
})();
