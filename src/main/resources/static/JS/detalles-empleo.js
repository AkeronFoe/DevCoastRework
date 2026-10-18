/* DevCoast · Ficha de una oferta.
   Diálogo de postulación (mensaje + hasta 3 proyectos del portafolio) y el marcador "Guardar oferta"
   (mismo endpoint que Empleos y Guardados: POST /empleos/favorito/{id} alterna el estado). */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    /* ── diálogo de postulación ── */
    var dlg = $('#dlgAplicar');
    var abrir = $('#btnAplicar');
    if (dlg && abrir) {
        var MAX = 3;
        var checks = $$('.ap-p input[type="checkbox"]', dlg);
        var cuenta = $('#apCount');
        var aviso = $('#apMax');

        var refrescar = function () {
            var n = checks.filter(function (c) { return c.checked; }).length;
            if (cuenta) cuenta.textContent = n === 0 ? '0 proyectos seleccionados'
                : n + (n === 1 ? ' proyecto seleccionado' : ' proyectos seleccionados');
            // al llegar al tope se bloquean las demás casillas; el aviso se oculta mientras haya cupo
            checks.forEach(function (c) { c.disabled = !c.checked && n >= MAX; });
            if (aviso) aviso.hidden = n < MAX;
        };
        checks.forEach(function (c) { c.addEventListener('change', refrescar); });

        var cerrar = function () { if (dlg.close) dlg.close(); else dlg.removeAttribute('open'); };
        abrir.addEventListener('click', function () {
            if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', '');
            var t = $('#apMsg'); if (t) t.focus();
        });
        $('#apCancel').addEventListener('click', cerrar);
        // clic en el fondo oscuro = cancelar
        dlg.addEventListener('click', function (e) { if (e.target === dlg) cerrar(); });
        refrescar();
    }

    /* ── guardar / quitar de guardados ── */
    var guardar = $('#btnGuardar');
    if (guardar) guardar.addEventListener('click', function () {
        if (guardar.disabled) return;
        var id = guardar.getAttribute('data-empleo-id');
        var etiqueta = $('span', guardar);
        var llamar = function () {
            return fetch('/empleos/favorito/' + encodeURIComponent(id), { method: 'POST' })
                .then(function (r) { if (!r.ok) throw new Error('http ' + r.status); return r.json(); });
        };
        var pintar = function (fav) {
            guardar.setAttribute('aria-pressed', fav ? 'true' : 'false');
            if (etiqueta) etiqueta.textContent = fav ? 'Guardada ✓' : 'Guardar oferta';
        };
        guardar.disabled = true;
        llamar().then(function (d) {
            guardar.disabled = false;
            if (!(d && d.ok)) throw new Error('respuesta inesperada');
            pintar(!!d.favorito);
            toast(d.favorito ? 'Oferta guardada.' : 'Oferta quitada de guardados.', {
                undo: function () {
                    llamar().then(function (r) { if (r && r.ok) pintar(!!r.favorito); })
                        .catch(function () { toast('No se pudo deshacer. Intenta de nuevo.'); });
                }
            });
        }).catch(function () {
            guardar.disabled = false;
            toast('No se pudo guardar la oferta. Intenta de nuevo.');
        });
    });
})();
