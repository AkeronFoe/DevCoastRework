/* DevCoast · Dashboard (docs/diseno/DEVCOAST-DASHBOARD-MOTION.md).
   Solo interfaz: el conteo animado de los números y el diálogo de bienvenida. Los datos los arma el servidor. */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    /* ── los números suben desde 0 (800 ms, un requestAnimationFrame por cuadro); sin animación con "reducir movimiento" ── */
    var nums = $$('.nums b[data-n]');
    if (nums.length && !reduce) {
        var objetivos = nums.map(function (b) {
            var span = $('span', b), destino = parseInt(b.getAttribute('data-n'), 10) || 0;
            return { nodo: span || b, destino: destino, suf: span ? '' : '' };
        });
        objetivos.forEach(function (o) { o.nodo.textContent = '0'; });
        var t0 = null;
        (function paso(t) {
            if (t0 === null) t0 = t;
            var k = Math.min(1, (t - t0) / 800), e = 1 - Math.pow(1 - k, 3);
            objetivos.forEach(function (o) {
                var v = String(Math.round(o.destino * e));
                if (o.nodo.textContent !== v) o.nodo.textContent = v;   // solo escribe si el valor cambió
            });
            if (k < 1) requestAnimationFrame(paso);
        })(performance.now());
    }

    /* ── diálogo de bienvenida (antes el onboarding de 3 pasos): mismo endpoint, mismos campos ── */
    var ob = $('#ob');
    if (!ob) return;
    var form = $('#obForm'), err = $('#obErr'), guardar = $('#obSave');
    var chips = $$('#obTechs input'), custom = $('#obCustom');

    // lo que ya hay en el stack: lo conocido marca su chip y el resto va al campo de texto
    var actual = (ob.getAttribute('data-stack') || '').split('|').map(function (s) { return s.trim(); }).filter(Boolean);
    var extra = [];
    actual.forEach(function (t) {
        var c = chips.filter(function (i) { return i.value.toLowerCase() === t.toLowerCase(); })[0];
        if (c) c.checked = true; else extra.push(t);
    });
    custom.value = extra.join(', ');

    if (ob.showModal) ob.showModal(); else ob.setAttribute('open', '');
    var primero = $('#obRol'); if (primero) primero.focus();

    $('#obSkip').addEventListener('click', function () { ob.close(); });

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        err.hidden = true;
        var stack = chips.filter(function (i) { return i.checked; }).map(function (i) { return i.value; });
        custom.value.split(',').map(function (s) { return s.trim(); }).filter(Boolean).forEach(function (t) {
            if (stack.map(function (s) { return s.toLowerCase(); }).indexOf(t.toLowerCase()) === -1) stack.push(t);
        });
        var payload = {
            rolTitulo: $('#obRol').value.trim(), ubicacion: $('#obUbi').value.trim(), bio: $('#obBio').value.trim(),
            modalidad: $('#obMod').value, disponibilidad: $('#obDisp').value, salarioEsperado: $('#obSal').value.trim(), stack: stack
        };
        guardar.disabled = true;
        fetch('/onboarding/completar', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { if (!(r.ok && d.ok)) throw new Error('fallo'); }); })
            .then(function () { window.location.reload(); })   // recarga para que el perfil, el match y las ofertas ya lo reflejen
            .catch(function () {
                guardar.disabled = false;
                err.textContent = T('No se pudo guardar. Inténtalo de nuevo.');
                err.hidden = false;
            });
    });
})();
