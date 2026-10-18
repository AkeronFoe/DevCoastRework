(function () {
    'use strict';

    var grupo = document.querySelector('.fase');
    if (!grupo) return;
    var oferta = grupo.getAttribute('data-oferta');
    var botones = Array.prototype.slice.call(grupo.querySelectorAll('button[data-fase]'));
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt); };

    grupo.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('button[data-fase]') : null;
        if (!b || b.disabled || b.getAttribute('aria-pressed') === 'true') return;
        botones.forEach(function (x) { x.disabled = true; });
        fetch('/empleos/' + encodeURIComponent(oferta) + '/fase?fase=' + encodeURIComponent(b.getAttribute('data-fase')),
            { method: 'POST', headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' } })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { return { ok: r.ok, d: d }; }); })
            .then(function (res) {
                if (!(res.ok && res.d.ok)) { toast(T('No se pudo cambiar la fase. Inténtalo de nuevo.')); return; }
                botones.forEach(function (x) { x.setAttribute('aria-pressed', x === b ? 'true' : 'false'); });
                toast(T('Fase actualizada:') + ' ' + b.textContent.trim() + '.');
            })
            .catch(function () { toast(T('Sin conexión. Inténtalo de nuevo.')); })
            .then(function () { botones.forEach(function (x) { x.disabled = false; }); });
    });
})();
