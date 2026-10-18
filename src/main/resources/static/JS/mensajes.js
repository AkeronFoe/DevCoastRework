// oigan, esto de acá hace funcionar la bandeja y el chat: filtra la lista, manda los mensajes sin recargar y trae los nuevos cada pocos segundos
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt); };
    var DIA = 86400000;

    function inicioDelDia(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime(); }
    function hora(t) { return new Date(t).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }); }

    function fechaCorta(t) {
        var d = new Date(t), hoy = inicioDelDia(new Date()), dia = inicioDelDia(d);
        if (dia === hoy) return hora(t);
        if (dia === hoy - DIA) return T('Ayer');
        if (hoy - dia < 7 * DIA) return d.toLocaleDateString('es-CO', { weekday: 'short' });
        return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
    }

    function etiquetaDia(t) {
        var d = new Date(t), hoy = inicioDelDia(new Date()), dia = inicioDelDia(d);
        if (dia === hoy) return T('Hoy');
        if (dia === hoy - DIA) return T('Ayer');
        var s = d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    $$('.mx-when[data-t]').forEach(function (el) { el.textContent = fechaCorta(+el.getAttribute('data-t')); });

    var chips = $$('.mx-tabs .ch');
    var items = $('#mxItems');
    if (chips.length && items) {
        var vacio = $('#mxNone');
        chips.forEach(function (c) {
            c.addEventListener('click', function () {
                var f = c.getAttribute('data-filtro'), visibles = 0;
                chips.forEach(function (o) { o.setAttribute('aria-pressed', o === c ? 'true' : 'false'); });
                $$('li', items).forEach(function (li) {
                    var ver = f === 'todos' || li.getAttribute('data-sin-leer') === 'true';
                    li.hidden = !ver;
                    if (ver) visibles++;
                });
                if (vacio) vacio.hidden = visibles > 0;
            });
        });
    }

    var hilo = $('#mxThread');
    var flow = $('#mxFlow');
    if (!hilo || !flow) return;

    var sol = hilo.getAttribute('data-sol');
    var yo = hilo.getAttribute('data-yo');
    var ultima = +hilo.getAttribute('data-ultima') || Date.now();
    var primera = +hilo.getAttribute('data-primera') || 0;
    var form = $('#mxForm'), area = $('#mxTexto'), enviarBtn = $('#mxSend'), cuenta = $('#mxCuenta');
    var jump = $('#mxJump'), vivo = $('#mxLive');
    var tactil = window.matchMedia && window.matchMedia('(pointer: coarse)').matches;
    var vistos = {};
    var cola = Promise.resolve();
    var pendiente = 0;

    function filas() { return $$('.mx-m', flow); }
    filas().forEach(function (f) { if (f.getAttribute('data-id')) vistos[f.getAttribute('data-id')] = true; });

    function cercaDelFondo() { return flow.scrollHeight - flow.scrollTop - flow.clientHeight < 140; }
    function alFondo() { flow.scrollTop = flow.scrollHeight; if (jump) jump.hidden = true; }

    // pone la hora local de cada mensaje, agrupa los seguidos de la misma persona y mete los separadores de día
    function decorar() {
        $$('.mx-day', flow).forEach(function (d) { d.remove(); });
        var lista = filas(), previo = null;
        lista.forEach(function (f, i) {
            var t = +f.getAttribute('data-t') || Date.now();
            var tm = $('time', f);
            if (tm) tm.textContent = hora(t);
            var propio = f.classList.contains('own');
            var dia = inicioDelDia(new Date(t));
            if (!previo || previo.dia !== dia) {
                var sep = document.createElement('div');
                sep.className = 'mx-day';
                sep.setAttribute('role', 'separator');
                sep.textContent = etiquetaDia(t);
                f.parentNode.insertBefore(sep, f);
            }
            var junto = !!previo && previo.dia === dia && previo.propio === propio && t - previo.t < 300000;
            f.classList.toggle('sg', junto);
            if (previo) previo.fila.classList.toggle('eg', !junto);
            previo = { dia: dia, propio: propio, t: t, fila: f };
            if (i === lista.length - 1) f.classList.add('eg');
        });
        flow.classList.add('ready');
    }

    var TICK = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12.5l4.5 4.5L16 8"/><path class="d" d="M11 14.5l2.5 2.5L22 8"/></svg>';

    function crear(m, nuevo) {
        var propio = m.remitenteId === yo;
        var f = document.createElement('div');
        f.className = 'mx-m ' + (propio ? 'own' : 'other') + (nuevo ? ' is-new' : '');
        if (m.id) f.setAttribute('data-id', m.id);
        f.setAttribute('data-t', m.t || Date.now());
        var b = document.createElement('p');
        b.className = 'mx-b';
        b.textContent = m.texto;
        var mm = document.createElement('span');
        mm.className = 'mx-mm';
        mm.appendChild(document.createElement('time'));
        if (propio) {
            var tick = document.createElement('span');
            tick.className = 'mx-tick' + (m.leido ? ' read' : '');
            tick.setAttribute('role', 'img');
            tick.setAttribute('aria-label', m.leido ? T('Leído') : T('Enviado'));
            tick.innerHTML = TICK;
            mm.appendChild(tick);
        }
        f.appendChild(b);
        f.appendChild(mm);
        return f;
    }

    function quitarVacio() { var v = $('#mxEmpty'); if (v) v.remove(); }

    // si la conversación todavía no estaba en la lista (primer mensaje), le armamos su fila
    function filaNueva() {
        var ul = $('#mxItems');
        if (!ul) {
            var vacio = $('.mx-vacio');
            if (!vacio) return null;
            ul = document.createElement('ul');
            ul.className = 'mx-items';
            ul.id = 'mxItems';
            vacio.parentNode.replaceChild(ul, vacio);
        }
        var li = document.createElement('li');
        li.setAttribute('data-sin-leer', 'false');
        var a = document.createElement('a');
        a.className = 'mx-it';
        a.href = '/chat/' + sol;
        a.setAttribute('aria-current', 'page');
        var av = document.createElement('span');
        av.className = 'mx-av t' + (hilo.getAttribute('data-tono') || '0');
        av.setAttribute('aria-hidden', 'true');
        av.textContent = hilo.getAttribute('data-inicial') || '?';
        var b = document.createElement('span');
        b.className = 'mx-it-b';
        var top = document.createElement('span');
        top.className = 'mx-it-top';
        var nombre = document.createElement('b');
        nombre.className = 'mx-name';
        nombre.textContent = hilo.getAttribute('data-otro') || '';
        var cuando = document.createElement('time');
        cuando.className = 'mx-when';
        top.appendChild(nombre);
        top.appendChild(cuando);
        var job = document.createElement('span');
        job.className = 'mx-job';
        job.textContent = hilo.getAttribute('data-cargo') || '';
        var prev = document.createElement('span');
        prev.className = 'mx-prev';
        b.appendChild(top);
        b.appendChild(job);
        b.appendChild(prev);
        a.appendChild(av);
        a.appendChild(b);
        li.appendChild(a);
        ul.insertBefore(li, ul.firstChild);
        return a;
    }

    function tocarLista(m, propio) {
        var it = $('.mx-it[aria-current="page"]') || filaNueva();
        if (!it) return;
        var prev = $('.mx-prev', it), cuando = $('.mx-when', it);
        if (prev) {
            prev.textContent = '';
            if (propio) { var i = document.createElement('i'); i.textContent = T('Tú') + ': '; prev.appendChild(i); }
            var s = document.createElement('span');
            s.textContent = m.texto;
            prev.appendChild(s);
        }
        if (cuando) { cuando.setAttribute('data-t', m.t); cuando.textContent = fechaCorta(m.t); }
        var li = it.parentNode;
        if (li && li.parentNode && li.parentNode.firstChild !== li) li.parentNode.insertBefore(li, li.parentNode.firstChild);
    }

    function recibir(m) {
        if (!m || (m.id && vistos[m.id])) return;
        if (m.id) vistos[m.id] = true;
        var estabaAbajo = cercaDelFondo();
        quitarVacio();
        flow.appendChild(crear(m, true));
        decorar();
        var propio = m.remitenteId === yo;
        if (propio || estabaAbajo) alFondo();
        else if (jump) jump.hidden = false;
        if (m.t > ultima) ultima = m.t;
        tocarLista(m, propio);
    }

    if (jump) jump.addEventListener('click', alFondo);
    flow.addEventListener('scroll', function () { if (jump && !jump.hidden && cercaDelFondo()) jump.hidden = true; });

    var mas = $('#mxMore'), masBtn = $('#mxMoreBtn');
    if (mas && masBtn) masBtn.addEventListener('click', function () {
        if (!primera) return;
        masBtn.disabled = true;
        fetch('/chat/' + sol + '/mas-mensajes?antes=' + primera, { headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error('http'); return r.json(); })
            .then(function (d) {
                var altoAntes = flow.scrollHeight, tope = flow.scrollTop;
                var ref = filas()[0] || null;
                (d.mensajes || []).forEach(function (m) {
                    if (m.id && vistos[m.id]) return;
                    if (m.id) vistos[m.id] = true;
                    flow.insertBefore(crear(m, false), ref);
                });
                decorar();
                flow.scrollTop = tope + (flow.scrollHeight - altoAntes);
                if (d.primera) primera = d.primera;
                if (!d.hayMas) mas.remove(); else masBtn.disabled = false;
            })
            .catch(function () { masBtn.disabled = false; toast(T('No se pudieron cargar los mensajes anteriores.')); });
    });

    function marcarLeidos(sinLeer) {
        var pend = {};
        (sinLeer || []).forEach(function (id) { pend[id] = true; });
        $$('.mx-m.own[data-id]', flow).forEach(function (f) {
            if (pend[f.getAttribute('data-id')]) return;
            var tk = $('.mx-tick', f);
            if (tk && !tk.classList.contains('read')) {
                tk.classList.add('read');
                tk.setAttribute('aria-label', T('Leído'));
                tk.title = T('Leído');
            }
        });
    }

    function enviarFila(fila, texto) {
        fila.classList.remove('fail');
        fila.classList.add('pend');
        var viejo = $('.mx-retry', fila);
        if (viejo) viejo.remove();
        pendiente++;
        return fetch('/chat/' + sol + '/enviar', { method: 'POST', body: new URLSearchParams({ texto: texto }) })
            .then(function (r) {
                if (r.status === 401) { location.href = '/login'; throw new Error('sesion'); }
                if (r.status === 429) { toast(T('Vas muy rápido. Espera un momento y vuelve a intentar.')); throw new Error('limite'); }
                if (!r.ok) throw new Error('http');
                return r.json();
            })
            .then(function (d) {
                if (!d || !d.ok) throw new Error('respuesta');
                var m = d.mensaje;
                vistos[m.id] = true;
                fila.classList.remove('pend');
                fila.setAttribute('data-id', m.id);
                fila.setAttribute('data-t', m.t);
                if (m.t > ultima) ultima = m.t;
                decorar();
                tocarLista(m, true);
            })
            .catch(function () {
                fila.classList.remove('pend');
                fila.classList.add('fail');
                var r = document.createElement('button');
                r.type = 'button';
                r.className = 'mx-retry';
                r.textContent = T('No se envió · Reintentar');
                r.addEventListener('click', function () { cola = cola.then(function () { return enviarFila(fila, texto); }); });
                $('.mx-mm', fila).appendChild(r);
            })
            .then(function () { pendiente--; });
    }

    function mandar() {
        var texto = area.value.trim();
        if (!texto) return;
        quitarVacio();
        var fila = crear({ remitenteId: yo, texto: texto, t: Date.now(), leido: false }, true);
        flow.appendChild(fila);
        decorar();
        alFondo();
        area.value = '';
        ajustar();
        area.focus();
        cola = cola.then(function () { return enviarFila(fila, texto); });
    }

    function ajustar() {
        area.style.height = 'auto';
        area.style.height = Math.min(area.scrollHeight + 2, 140) + 'px';
        var n = area.value.length;
        enviarBtn.disabled = !area.value.trim();
        if (cuenta) {
            cuenta.hidden = n < 850;
            cuenta.textContent = n + ' / 1000';
            if (n >= 1000) cuenta.setAttribute('data-cerca', ''); else cuenta.removeAttribute('data-cerca');
        }
    }

    if (form && area) {
        form.addEventListener('submit', function (e) { e.preventDefault(); mandar(); });
        area.addEventListener('input', ajustar);
        area.addEventListener('keydown', function (e) {
            if (e.key !== 'Enter' || e.isComposing) return;
            if ((e.ctrlKey || e.metaKey) || (!e.shiftKey && !tactil)) { e.preventDefault(); mandar(); }
        });
        $$('[data-sug]').forEach(function (b) {
            b.addEventListener('click', function () { area.value = b.getAttribute('data-sug'); ajustar(); area.focus(); });
        });
        ajustar();
        if (!tactil) area.focus();
    }

    var intervalo = 4000, ultimoMovimiento = Date.now(), timer = 0, fallos = 0, consultando = false;

    function enLinea(ok) {
        if (!vivo) return;
        vivo.classList.toggle('off', !ok);
        var s = $('span', vivo);
        if (s) s.textContent = ok ? T('En vivo') : T('Reconectando…');
    }

    function consultar() {
        if (document.visibilityState !== 'visible' || consultando) return;
        consultando = true;
        fetch('/chat/' + sol + '/nuevos?despues=' + ultima, { headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (r.status === 401) { location.href = '/login'; throw new Error('sesion'); }
                if (!r.ok) throw new Error('http');
                return r.json();
            })
            .then(function (d) {
                if (fallos) { fallos = 0; enLinea(true); }
                var lista = d.mensajes || [];
                lista.forEach(recibir);
                if (lista.length) ultimoMovimiento = Date.now();
                if (!pendiente) marcarLeidos(d.sinLeer);
            })
            .catch(function () { fallos++; if (fallos >= 2) enLinea(false); })
            .then(function () { consultando = false; });
    }

    function tic() {
        consultar();
        intervalo = Date.now() - ultimoMovimiento > 120000 ? 9000 : 4000;
        timer = setTimeout(tic, intervalo);
    }

    document.addEventListener('visibilitychange', function () {
        clearTimeout(timer);
        if (document.visibilityState === 'visible') { ultimoMovimiento = Date.now(); tic(); }
    });

    decorar();
    alFondo();
    timer = setTimeout(tic, intervalo);
})();
