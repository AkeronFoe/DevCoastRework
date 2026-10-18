(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    var CLAVE = { revision: 'revision', aprobada: 'aceptadas', rechazada: 'noav' };
    var STUB = { revision: ['s-rev', 'En revisión'], aprobada: ['s-ace', 'Aceptada'], rechazada: ['s-rec', 'No avanzó'] };
    var tabs = $('.tabs');
    var filtro = tabs ? tabs.getAttribute('data-estado') || '' : '';
    var FILTRO_DE = { revision: 'revision', aceptadas: 'aprobada', rechazadas: 'rechazada' };

    function sumar(id, delta) {
        var el = document.getElementById(id);
        if (!el) return;
        var n = parseInt(el.textContent, 10);
        if (!isNaN(n)) el.textContent = Math.max(0, n + delta);
    }
    function plural(n, uno, varios) { return n + ' ' + (n === 1 ? uno : varios); }

    function ajustarContadores(sec, desde, hasta) {
        var ids = { revision: ['cRevision', 'tRevision'], aprobada: ['cAceptadas', 'tAceptadas'], rechazada: [null, 'tNoAvanzaron'] };
        ids[desde].forEach(function (id) { if (id) sumar(id, -1); });
        ids[hasta].forEach(function (id) { if (id) sumar(id, 1); });
        var ac = $('#cAceptadas');
        if (ac && ac.nextElementSibling) ac.nextElementSibling.textContent = parseInt(ac.textContent, 10) === 1 ? T('aceptada') : T('aceptadas');
        if (!sec) return;
        var attr = { revision: 'data-rev', aprobada: 'data-ace', rechazada: 'data-no' };
        sec.setAttribute(attr[desde], Math.max(0, parseInt(sec.getAttribute(attr[desde]), 10) - 1));
        sec.setAttribute(attr[hasta], parseInt(sec.getAttribute(attr[hasta]), 10) + 1);
        var r = parseInt(sec.getAttribute('data-rev'), 10), a = parseInt(sec.getAttribute('data-ace'), 10), n = parseInt(sec.getAttribute('data-no'), 10);
        var c = $('.ofg-c', sec);
        if (c) c.textContent = r + ' ' + T('en revisión') + ' · ' + plural(a, T('aceptada'), T('aceptadas')) + ' · ' + plural(n, T('no avanzó'), T('no avanzaron'));
        if (sec.getAttribute('data-activa') === 'true' && (desde === 'revision' || hasta === 'revision')) {
            var link = $('.menu a[href$="/candidatos"]');
            if (link) {
                var b = $('.badge', link), delta = desde === 'revision' ? -1 : 1;
                var actual = b ? parseInt(b.textContent, 10) || 0 : 0, nuevo = Math.max(0, actual + delta);
                if (nuevo === 0) { if (b) b.remove(); }
                else {
                    if (!b) { b = document.createElement('span'); b.className = 'badge'; link.appendChild(b); }
                    b.textContent = nuevo;
                    b.setAttribute('aria-label', plural(nuevo, T('postulación'), T('postulaciones')) + ' ' + T('en revisión'));
                }
            }
        }
    }

    function mover(art, nuevo, deshacer) {
        var desde = art.getAttribute('data-estado');
        if (desde === nuevo) return;
        var form = $('.mover[data-nuevo="' + nuevo + '"]', art);
        var url = form ? form.getAttribute('data-url') : null;
        if (!url) return;
        var nombre = art.getAttribute('data-nombre') || T('La persona');
        $$('button, summary', art).forEach(function (b) { b.disabled = true; });
        fetch(url + '?nuevoEstado=' + encodeURIComponent(nuevo), { method: 'POST', headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' } })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { return { ok: r.ok, d: d }; }); })
            .then(function (res) {
                if (!(res.ok && res.d.ok)) { toast(T('No se pudo actualizar. Inténtalo de nuevo.')); return; }
                if (deshacer === false && !document.body.contains(art)) { window.location.reload(); return; }
                var sec = art.closest('.ofg');
                art.setAttribute('data-estado', nuevo);
                var stub = $('.jt-stub', art);
                if (stub) {
                    stub.className = 'jt-stub ' + STUB[nuevo][0];
                    var v = $('.st-v', stub); if (v) v.textContent = T(STUB[nuevo][1]);
                }
                $$('details[open]', art).forEach(function (d) { d.open = false; });
                ajustarContadores(sec, desde, nuevo);

                if (filtro && FILTRO_DE[filtro] !== nuevo) {
                    art.classList.add('saliendo');
                    setTimeout(function () {
                        art.remove();
                        if (sec && !$('.cd', sec)) sec.remove();
                    }, 260);
                }
                var msg = nuevo === 'aprobada' ? T('Aceptaste a') + ' ' + nombre + '.'
                    : nuevo === 'rechazada' ? nombre + ' ' + T('no avanza.') : nombre + ' ' + T('volvió a revisión.');
                toast(msg, nuevo === 'aprobada' || deshacer === false ? undefined : { undo: function () { mover(art, desde, false); } });
            })
            .catch(function () { toast(T('Sin conexión. Inténtalo de nuevo.')); })
            .then(function () { $$('button, summary', art).forEach(function (b) { b.disabled = false; }); });
    }

    document.addEventListener('submit', function (e) {
        var f = e.target.closest ? e.target.closest('form.mover') : null;
        if (!f) return;
        e.preventDefault();
        var art = f.closest('.cd');
        if (art) mover(art, f.getAttribute('data-nuevo'));
    });
})();
