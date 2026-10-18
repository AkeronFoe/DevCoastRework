/* DevCoast · Detalle de un proyecto. "Cancelar" de la confirmación para eliminar (la confirmación es un <details>). */
(function () {
    'use strict';
    document.addEventListener('click', function (e) {
        var c = e.target.closest ? e.target.closest('[data-cancelar-del]') : null;
        if (!c) return;
        var det = c.closest('details');
        if (det) { det.open = false; var s = det.querySelector('summary'); if (s) s.focus(); }
    });
})();
