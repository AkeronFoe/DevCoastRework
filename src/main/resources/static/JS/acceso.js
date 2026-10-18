/* DevCoast · login y registro (docs/diseno/DEVCOAST-LOGIN-MOTION.md y DEVCOAST-REGISTER-MOTION.md).
   Solo interfaz: gaviota, validación de ayuda visual, medidor de contraseña y la animación de zarpar.
   El servidor valida todo otra vez; aquí el envío es un POST normal (form.submit()), nada se simula. Vanilla. */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var clamp = function (v, a, b) { return v < a ? a : (v > b ? b : v); };
    // textos de ayuda pasan por el i18n del proyecto (si no hay traducción, quedan en español)
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var rmq = window.matchMedia('(prefers-reduced-motion: reduce)');

    var pass = $('#pass'), zone = $('#zone'), go = $('#go'), goTxt = $('#goTxt'), formErr = $('#formErr'),
        boatWrap = $('#boatWrap'), gull = boatWrap && $('.gull', boatWrap), say = $('#say'), okBack = $('#okBack'), ok = $('#ok');
    var forms = $$('form.fields, form.pass-main');
    if (!pass || !forms.length || !gull) return;
    var look = $('.gl-look', gull);
    var registro = document.body.classList.contains('registro');
    var busy = false, timers = [], sayT = 0, afterT = 0, my = window.innerHeight * 0.5, pend = false, level = 0;

    function later(fn, ms) { timers.push(setTimeout(fn, ms)); }
    function clearTimers() { timers.forEach(clearTimeout); timers = []; }
    function activeForm() { return forms.filter(function (f) { return !f.hidden; })[0] || forms[0]; }
    function pwFields(f) { return $$('[data-pw]', f || activeForm()); }

    /* ── la gaviota mira al campo activo o al cursor; con la contraseña se tapa los ojos ── */
    function aim() {
        pend = false;
        var a = 0;
        if (gull.classList.contains('shy')) a = -14;
        else {
            var ay = my, f = document.activeElement;
            if (f && f.matches && f.matches('form input')) { var r = f.getBoundingClientRect(); ay = r.top + r.height / 2; }
            var b = look.getBoundingClientRect();
            if (b.width) a = clamp(((b.top + b.height / 2) - ay) / (window.innerHeight * 0.5), -1, 1) * 8;
        }
        look.style.setProperty('--look', a.toFixed(1));
    }
    function queue() { if (!pend) { pend = true; requestAnimationFrame(aim); } }
    window.addEventListener('pointermove', function (e) { my = e.clientY; queue(); }, { passive: true });
    window.addEventListener('scroll', queue, { passive: true });

    function sayNow(txt, ms) {
        clearTimeout(sayT); clearTimeout(afterT);
        say.textContent = T(txt); say.classList.add('on');
        if (ms) sayT = setTimeout(function () { say.classList.remove('on'); }, ms);
    }
    function sayOff() { clearTimeout(sayT); clearTimeout(afterT); say.classList.remove('on'); }
    // frase corta que vuelve al estado anterior ("no estoy mirando" si sigue en la contraseña)
    function sayThenShy(txt, ms) {
        sayNow(txt, ms);
        afterT = setTimeout(function () { if (gull.classList.contains('shy')) sayNow('no estoy mirando'); }, ms + 60);
    }
    function syncShy() {
        var a = document.activeElement;
        var shy = !!a && a.classList && (a.classList.contains('eye') || a.hasAttribute('data-pw'));
        var was = gull.classList.contains('shy');
        gull.classList.toggle('shy', shy);
        if (shy && !was) sayNow('no estoy mirando');
        if (!shy && was && say.textContent === T('no estoy mirando')) sayOff();
        queue();
    }
    document.addEventListener('focusin', syncShy);
    document.addEventListener('focusout', function () { setTimeout(syncShy, 0); });

    function hop(g) {
        if (rmq.matches) return;
        g.classList.remove('hop'); void g.getBoundingClientRect(); g.classList.add('hop');
        later(function () { g.classList.remove('hop'); }, 950);
    }

    /* ── ver / ocultar contraseña(s): cada formulario trae su botón .eye ── */
    $$('.eye').forEach(function (eye) {
        eye.addEventListener('click', function () {
            var fields = pwFields(eye.form), show = fields[0] && fields[0].type === 'password';
            fields.forEach(function (i) { i.type = show ? 'text' : 'password'; });
            var plural = fields.length > 1;
            eye.textContent = T(show ? 'ocultar' : 'ver');
            eye.setAttribute('aria-pressed', show ? 'true' : 'false');
            eye.setAttribute('aria-label', T(show ? (plural ? 'Ocultar contraseñas' : 'Ocultar contraseña') : (plural ? 'Mostrar contraseñas' : 'Mostrar contraseña')));
            if (fields[0]) fields[0].focus();
            syncShy();
        });
    });

    /* ── errores bajo cada campo (aria-describedby + aria-invalid) ── */
    function errEl(control) {
        var id = (control.getAttribute('aria-describedby') || '').split(' ')[0];
        return id ? document.getElementById(id) : null;
    }
    function setErr(control, msg) {
        var p = errEl(control);
        if (p) { p.textContent = msg ? T(msg) : ''; p.hidden = !msg; }
        if (control.getAttribute('role') !== 'radiogroup') control.setAttribute('aria-invalid', msg ? 'true' : 'false');
        return !!msg;
    }
    var EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    // reglas declaradas en el HTML: data-req, data-email, data-min (+data-min-msg), data-same (+data-same-msg), grupos con data-group-req
    function validate(f) {
        var first = null;
        function mark(el, bad) { if (bad && !first) first = el; }
        $$('[data-req]', f).forEach(function (c) {
            var v = c.value.trim(), msg = '';
            if (!v) msg = c.getAttribute('data-req');
            else if (c.hasAttribute('data-email') && !EMAIL.test(v)) msg = 'Revisa tu correo: debe verse como nombre@dominio.com.';
            else if (c.hasAttribute('data-min') && c.value.length < +c.getAttribute('data-min')) msg = c.getAttribute('data-min-msg');
            else if (c.hasAttribute('data-same') && c.value !== $(c.getAttribute('data-same'), f).value) msg = c.getAttribute('data-same-msg');
            mark(c, setErr(c, msg));
        });
        $$('[data-group-req]', f).forEach(function (g) {
            var chosen = $('input:checked', g);
            mark($('input', g), setErr(g, chosen ? '' : g.getAttribute('data-group-req')));
        });
        return first;
    }
    forms.forEach(function (f) {
        $$('input', f).forEach(function (c) {
            c.addEventListener('input', function () {
                if (c.getAttribute('aria-invalid') === 'true') { setErr(c, ''); formErr.textContent = ''; }
            });
        });
        $$('[data-group-req]', f).forEach(function (g) {
            $$('input', g).forEach(function (i) { i.addEventListener('change', function () { setErr(g, ''); }); });
        });
        $$('[data-same]', f).forEach(function (c) {
            c.addEventListener('blur', function () {
                var other = $(c.getAttribute('data-same'), f);
                if (c.value && c.value !== other.value) setErr(c, c.getAttribute('data-same-msg'));
            });
        });
    });

    function nope() {
        if (!rmq.matches) {
            pass.classList.remove('shake'); void pass.offsetWidth; pass.classList.add('shake');
            gull.classList.remove('nope'); void gull.getBoundingClientRect(); gull.classList.add('nope');
            later(function () { pass.classList.remove('shake'); gull.classList.remove('nope'); }, 700);
        }
        sayNow('así no te dejo pasar', 2600);
    }

    /* ── registro: talento o empresa ── */
    var passTitle = $('#passTitle');
    function setRole(r) {
        pass.setAttribute('data-role', r);
        forms.forEach(function (f) { f.hidden = f.getAttribute('data-role') !== r; });
        var f = activeForm();
        if (passTitle) passTitle.textContent = T(r === 'talento' ? 'PASE · TALENTO' : 'PASE · EMPRESA');
        go.setAttribute('form', f.id);
        // las contraseñas vuelven a estar ocultas al cambiar de formulario
        $$('.eye').forEach(function (e) { e.textContent = T('ver'); e.setAttribute('aria-pressed', 'false'); e.setAttribute('aria-label', T('Mostrar contraseñas')); });
        $$('[data-pw]').forEach(function (i) { i.type = 'password'; });
        formErr.textContent = '';
    }
    $$('input[name="role"]').forEach(function (i) {
        i.addEventListener('change', function () {
            setRole(i.value); hop(gull);
            sayNow(i.value === 'talento' ? 'talento a bordo.' : 'empresa a la vista.', 2200);
        });
    });

    /* ── medidor de seguridad (solo ayuda visual) ── */
    var labels = ['fuerza de la contraseña', 'débil', 'regular', 'buena', 'fuerte'];
    var quips = ['', 'no miro, pero suena débil.', 'va mejorando.', 'esa ya aguanta oleaje.', 'esa aguanta temporal.'];
    function strength(v) {
        if (!v) return 0;
        var s = (v.length >= 8 ? 1 : 0) + (v.length >= 12 ? 1 : 0) + (/[a-z]/.test(v) && /[A-Z]/.test(v) ? 1 : 0) + (/\d/.test(v) || /[^A-Za-z0-9]/.test(v) ? 1 : 0);
        return Math.max(1, s);
    }
    $$('[data-meter]').forEach(function (input) {
        var meter = $('.meter', input.form), cubes = $$('i', meter), txt = $('.meter-txt', meter);
        input.addEventListener('input', function () {
            var l = strength(input.value);
            cubes.forEach(function (c, i) { c.className = i < l ? (l >= 3 ? 't' : 'o') : ''; });
            txt.textContent = T(labels[l]);
            if (l !== level && l > 0) sayThenShy(quips[l], 2300);
            level = l;
        });
    });

    /* ── envío: validar y, si todo está bien, zarpar ──
       El POST es el normal del formulario: se lanza de inmediato y la animación corre mientras el servidor responde.
       Si el servidor devuelve error, la página se recarga con el mensaje (formErr) y la animación de error. */
    function board(f) {
        busy = true; formErr.textContent = '';
        go.disabled = true; goTxt.textContent = T(go.getAttribute('data-busy') || 'Zarpando…');
        $$('input', f).forEach(function (i) { i.readOnly = true; });
        if (rmq.matches) { f.submit(); return; }
        hop(gull); sayNow('¡a bordo!');
        later(function () { zone.classList.add('torn'); }, 350);
        later(function () { zone.classList.add('gone'); boatWrap.classList.add('away'); }, 950);
        // si el servidor tarda, no se queda la pantalla vacía: aparece un aviso y una salida
        later(function () { if (ok) { zone.classList.add('done'); if (okBack) okBack.focus({ preventScroll: true }); } }, 4200);
        f.submit();
    }
    forms.forEach(function (f) {
        f.addEventListener('submit', function (ev) {
            ev.preventDefault(); if (busy) return;
            var first = validate(f);
            if (first) { formErr.textContent = T('Revisa los campos marcados para poder subir a bordo.'); nope(); first.focus(); return; }
            board(f);
        });
    });

    /* vuelve al formulario (aviso de "entrando" o regreso con el botón Atrás del navegador) */
    function reset() {
        clearTimers(); sayOff(); busy = false;
        zone.classList.remove('torn', 'gone', 'done');
        boatWrap.style.transition = 'none'; boatWrap.classList.remove('away');
        void boatWrap.offsetWidth; boatWrap.style.transition = '';
        go.disabled = false; goTxt.textContent = T(go.getAttribute('data-label') || 'Zarpar');
        $$('input', activeForm()).forEach(function (i) { i.readOnly = false; });
        var f = $('input:not([type=hidden])', activeForm()); if (f) f.focus({ preventScroll: true });
    }
    if (okBack) okBack.addEventListener('click', reset);
    window.addEventListener('pageshow', function (e) { if (e.persisted) reset(); });

    /* ── frases al azar de la gaviota (y al hacerle clic) ── */
    var lines = registro
        ? ['¿tienes pan? es por curiosidad.', 'sin prisa, el barco espera.', 'mejor una contraseña larga que una rara.', '¿talento o empresa? los dos caben a bordo.', 'psst… revisa bien tu correo.', 'yo vigilo el muelle.', 'no me mires tanto, que me da pena.', 'cuac. no, espera, ese es el pato.', '¿ya pensaste tu contraseña?']
        : ['¿tienes pan? es por curiosidad.', 'hoy hay buen viento.', 'yo vigilo el muelle.', 'psst… revisa bien tu correo.', 'yo no me sé tu contraseña, lo juro.', '¿primera vez por aquí? crea tu cuenta arriba.', 'no me mires tanto, que me da pena.', 'sube, que la marea no espera.', 'esa vela es un cursor, ¿ya lo notaste?', 'cuac. no, espera, ese es el pato.', '¿ya desayunaste?'];
    var lastLine = -1;
    function randomLine() { var i; do { i = Math.floor(Math.random() * lines.length); } while (i === lastLine); lastLine = i; return lines[i]; }
    function canChat() { return !busy && !gull.classList.contains('shy') && !say.classList.contains('on'); }
    function chat() {
        if (canChat()) { sayNow(randomLine(), 4200); if (Math.random() < 0.35) hop(gull); }
        setTimeout(chat, 7000 + Math.random() * 6000);
    }
    setTimeout(chat, 3500 + Math.random() * 2000);
    gull.addEventListener('click', function () {
        if (busy || gull.classList.contains('shy')) return;
        sayNow(randomLine(), 4200); hop(gull);
    });

    /* ── si el servidor devolvió un error, se anima igual que un error del cliente ── */
    if (pass.hasAttribute('data-error')) {
        later(function () {
            nope();
            var bad = $('[aria-invalid="true"]', activeForm());
            if (bad) bad.focus({ preventScroll: true });
        }, rmq.matches ? 0 : 900);
    }
    queue();
})();
