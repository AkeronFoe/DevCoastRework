(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };
    var rmq = window.matchMedia('(prefers-reduced-motion: reduce)');


    function restar(el, delta) {
        if (!el) return;
        var n = parseInt(el.textContent, 10);
        if (!isNaN(n)) el.textContent = Math.max(0, n + delta);
    }

    document.addEventListener('submit', function (e) {
        var f = e.target.closest ? e.target.closest('form.retirar') : null;
        if (!f) return;
        e.preventDefault();
        var art = f.closest('.jt'), btn = $('.apply[type=submit]', f);
        if (!art || (btn && btn.disabled)) return;
        if (btn) btn.disabled = true;
        var cargo = f.getAttribute('data-cargo') || '';

        fetch(f.getAttribute('action'), { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('fallo');
                art.classList.remove('in');
                art.classList.add('sent');
                var chips = document.querySelectorAll('.tabs .ch b');
                restar(chips[0], -1); restar(chips[1], -1);
                var conteo = $('#conteo');
                if (conteo) {
                    var todas = Math.max(0, +conteo.getAttribute('data-todas') - 1), proc = Math.max(0, +conteo.getAttribute('data-proceso') - 1);
                    conteo.setAttribute('data-todas', todas); conteo.setAttribute('data-proceso', proc);
                    conteo.textContent = (todas === 1 ? '1 ' + T('solicitud') : todas + ' ' + T('solicitudes')) + ' · ' + proc + ' ' + T('en proceso');
                }
                toast(T('Solicitud retirada:') + ' ' + cargo + '.');
                setTimeout(function () { if (art.parentNode) art.remove(); }, rmq.matches ? 400 : 2200);
            })
            .catch(function () {
                if (btn) btn.disabled = false;
                toast(T('No se pudo retirar la solicitud. Inténtalo de nuevo.'));
            });
    });
})();
