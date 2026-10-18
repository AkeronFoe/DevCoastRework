// esto de acá maneja lo que se mueve en todas las pantallas: el menú del celular, la campana de avisos y los mensajitos que aparecen abajo
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    (function () {
        var tokenMeta = $('meta[name="_csrf"]');
        var headerMeta = $('meta[name="_csrf_header"]');
        if (!tokenMeta || !headerMeta || !window.fetch) return;
        var token = tokenMeta.getAttribute('content');
        var headerName = headerMeta.getAttribute('content');
        var protegidos = ['POST', 'PUT', 'PATCH', 'DELETE'];
        var original = window.fetch;
        window.fetch = function (input, init) {
            init = init || {};
            var metodo = (init.method || 'GET').toUpperCase();
            if (protegidos.indexOf(metodo) !== -1) {
                var headers = new Headers(init.headers || {});
                if (!headers.has(headerName)) headers.set(headerName, token);
                init = Object.assign({}, init, { headers: headers });
            }
            return original(input, init);
        };
    })();

    var sortSel = $('#sort');
    if (sortSel && sortSel.form) sortSel.addEventListener('change', function () { sortSel.form.submit(); });

    document.addEventListener('click', function (e) {
        var c = e.target.closest ? e.target.closest('[data-cancelar]') : null;
        if (!c) return;
        var det = c.closest('details');
        if (det) { det.open = false; var s = det.querySelector('summary'); if (s) s.focus(); }
    });

    var side = $('#side'), scrim = $('#scrim'), menuBtn = $('#menuBtn');
    if (side && scrim && menuBtn) {
        var mq = window.matchMedia('(max-width:900px)');
        var setMenu = function (open) {
            side.classList.toggle('open', open);
            scrim.hidden = !open;
            menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (open) {
                var f = side.querySelector('.mi');
                if (f) f.focus();
            } else if (document.activeElement && side.contains(document.activeElement)) {
                menuBtn.focus();
            }
        };
        var sideA11y = function () {
            if (mq.matches && !side.classList.contains('open')) side.setAttribute('inert', '');
            else side.removeAttribute('inert');
        };
        menuBtn.addEventListener('click', function () { setMenu(!side.classList.contains('open')); });
        scrim.addEventListener('click', function () { setMenu(false); });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && side.classList.contains('open')) setMenu(false);
        });
        new MutationObserver(sideA11y).observe(side, { attributes: true, attributeFilter: ['class'] });
        if (mq.addEventListener) mq.addEventListener('change', function () {
            if (!mq.matches) { side.classList.remove('open'); scrim.hidden = true; menuBtn.setAttribute('aria-expanded', 'false'); }
            sideA11y();
        });
        sideA11y();
    }

    var bellBtn = $('#bellBtn'), bellPanel = $('#bellPanel');
    if (bellBtn && bellPanel) {
        var setBell = function (open) {
            bellPanel.hidden = !open;
            bellBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (!open && bellPanel.contains(document.activeElement)) bellBtn.focus();
        };
        bellBtn.addEventListener('click', function () { setBell(bellPanel.hidden); });
        document.addEventListener('click', function (e) {
            if (!bellPanel.hidden && !bellPanel.contains(e.target) && !bellBtn.contains(e.target)) setBell(false);
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !bellPanel.hidden) setBell(false);
        });
        var all = $('#bellAll');
        if (all) all.addEventListener('click', function () {
            fetch('/notificaciones/leer-todas', { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (r) { return r.json(); })
                .then(function (d) {
                    if (!d || !d.ok) return;
                    var n = $('.bell-n'); if (n) n.remove();
                    bellPanel.querySelectorAll('a.new').forEach(function (a) { a.classList.remove('new'); });
                    bellBtn.setAttribute('aria-label', 'Notificaciones');
                    all.remove();
                })
                .catch(function () { });
        });
    }

    var toastEl = $('#toast'), toastTxt = $('#toastTxt'), toastUndo = $('#toastUndo'), toastT = 0, undoFn = null;
    function hideToast() { if (toastEl) toastEl.hidden = true; undoFn = null; }
    function toast(txt, opts) {
        if (!toastEl) return;
        opts = opts || {};
        toastTxt.textContent = txt;
        undoFn = typeof opts.undo === 'function' ? opts.undo : null;
        toastUndo.hidden = !undoFn;
        toastEl.hidden = true; void toastEl.offsetWidth; toastEl.hidden = false;
        clearTimeout(toastT);
        toastT = setTimeout(hideToast, opts.ms || 5000);
    }
    if (toastUndo) toastUndo.addEventListener('click', function () {
        var fn = undoFn; hideToast(); clearTimeout(toastT);
        if (fn) fn();
    });
    window.DevCoast = window.DevCoast || {};
    window.DevCoast.toast = toast;

    var gulls = document.querySelectorAll('.gull');
    if (gulls.length) {
        var raf = 0, lastLook = null;
        var looks = [];
        gulls.forEach(function (g) { var l = g.querySelector('.gl-look'); if (l) looks.push(l); });
        if (!reduce) {
            window.addEventListener('pointermove', function (e) {
                if (raf) return;
                raf = requestAnimationFrame(function () {
                    raf = 0;
                    var v = Math.round((e.clientY / window.innerHeight - 0.5) * 16);
                    v = Math.max(-8, Math.min(8, v));
                    if (v === lastLook) return;
                    lastLook = v;
                    looks.forEach(function (l) { l.style.setProperty('--look', v); });
                });
            }, { passive: true });
        }
        gulls.forEach(function (g) {
            g.addEventListener('click', function () {
                if (reduce) return;
                g.classList.remove('hop'); void g.getBoundingClientRect(); g.classList.add('hop');
                setTimeout(function () { g.classList.remove('hop'); }, 900);
            });
        });
    }
})();
