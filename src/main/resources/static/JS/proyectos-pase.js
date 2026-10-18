/* DevCoast · Proyectos (docs/diseno/DEVCOAST-PROYECTOS-MOTION.md).
   Filtros y búsqueda los resuelve el servidor (parámetros GET). Aquí queda lo de interfaz:
   el diálogo para añadir y editar, el interruptor PÚBLICO/OCULTO y el aviso con "Deshacer". */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    /* ── "Cancelar" de la confirmación para eliminar (la confirmación es un <details>) ── */
    document.addEventListener('click', function (e) {
        var c = e.target.closest ? e.target.closest('[data-cancelar-del]') : null;
        if (!c) return;
        var det = c.closest('details');
        if (det) { det.open = false; var s = $('summary', det); if (s) s.focus(); }
    });

    /* ── contador "N proyectos · M públicos" ── */
    function contar(deltaTotal, deltaPublicos) {
        var el = $('#conteo'); if (!el) return;
        var total = Math.max(0, +el.getAttribute('data-total') + deltaTotal), pub = Math.max(0, +el.getAttribute('data-publicos') + deltaPublicos);
        el.setAttribute('data-total', total); el.setAttribute('data-publicos', pub);
        el.textContent = (total === 1 ? '1 ' + T('proyecto') : total + ' ' + T('proyectos')) + ' · ' + pub + ' ' + (pub === 1 ? T('público') : T('públicos'));
    }

    /* ── interruptor de visibilidad: POST real (JSON); sin JavaScript se envía el formulario normal ── */
    function fijarVisibilidad(art, sw, publicado) {
        sw.setAttribute('aria-checked', publicado ? 'true' : 'false');
        $('span', sw).textContent = T(publicado ? 'PÚBLICO' : 'OCULTO');
        art.classList.toggle('off', !publicado);
        var h = $('input[name="publicado"]', sw.form); if (h) h.value = publicado ? 'false' : 'true';
    }
    function enviarVisibilidad(form, publicado) {
        return fetch(form.getAttribute('action'), {
            method: 'POST',
            headers: { 'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'publicado=' + (publicado ? 'true' : 'false')
        }).then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { if (!(r.ok && d.ok)) throw new Error(d.mensaje || 'fallo'); return d; }); });
    }
    document.addEventListener('submit', function (e) {
        var f = e.target.closest ? e.target.closest('form.swt-form') : null;
        if (!f) return;
        e.preventDefault();
        var sw = $('.swt', f), art = f.closest('.pj'), nombre = (art.getAttribute('data-nombre') || '');
        var antes = sw.getAttribute('aria-checked') === 'true', ahora = !antes;
        sw.disabled = true;
        enviarVisibilidad(f, ahora).then(function () {
            sw.disabled = false;
            fijarVisibilidad(art, sw, ahora); contar(0, ahora ? 1 : -1);
            toast(ahora ? T('Ahora') + ' “' + nombre + '” ' + T('es público.') : '“' + nombre + '” ' + T('quedó oculto.'), {
                undo: function () {
                    enviarVisibilidad(f, antes).then(function () { fijarVisibilidad(art, sw, antes); contar(0, antes ? 1 : -1); })
                        .catch(function () { toast(T('No se pudo deshacer. Inténtalo de nuevo.')); });
                }
            });
        }).catch(function () {
            sw.disabled = false;
            toast(T('No se pudo cambiar la visibilidad. Inténtalo de nuevo.'));
        });
    });

    /* ── diálogo: añadir y editar ── */
    var dlg = $('#dlg'), form = $('#pform');
    if (!dlg || !form) return;
    var campos = { nombre: $('#pNombre'), categoria: $('#pCategoria'), tipo: $('#pTipo'), descripcion: $('#pDesc'), link: $('#pLink') };
    var opener = null;

    function setErr(input, id, msg) {
        var p = document.getElementById(id); p.textContent = msg ? T(msg) : ''; p.hidden = !msg;
        input.setAttribute('aria-invalid', msg ? 'true' : 'false');
        return !!msg;
    }
    function limpiarErrores() {
        setErr(campos.nombre, 'eNombre', ''); setErr(campos.categoria, 'eCategoria', ''); setErr(campos.tipo, 'eTipo', '');
        setErr(campos.descripcion, 'eDesc', ''); setErr(campos.link, 'eLink', '');
    }
    // si el valor guardado no está entre las opciones (datos viejos), se agrega para no perderlo al editar
    function seleccionar(sel, valor) {
        if (valor && !$$('option', sel).some(function (o) { return o.value === valor; })) {
            var o = document.createElement('option'); o.value = valor; o.textContent = valor; sel.appendChild(o);
        }
        sel.value = valor || '';
    }
    function abrir(art, boton) {
        opener = boton || null;
        form.reset(); limpiarErrores();
        var editando = !!art;
        $('#dlgT').textContent = T(editando ? 'Editar proyecto' : 'Añadir proyecto');
        $('#dlgK').textContent = T(editando ? 'EN TU PORTAFOLIO' : 'NUEVO EN TU PORTAFOLIO');
        $('#pSave').textContent = T(editando ? 'Guardar cambios' : 'Guardar proyecto');
        var full = $('#pFull');
        if (editando) {
            var id = art.getAttribute('data-id');
            form.setAttribute('action', '/proyectos/' + encodeURIComponent(id) + '/actualizar');
            campos.nombre.value = art.getAttribute('data-nombre') || '';
            seleccionar(campos.categoria, art.getAttribute('data-categoria'));
            seleccionar(campos.tipo, art.getAttribute('data-tipo'));
            campos.descripcion.value = art.getAttribute('data-desc') || '';
            $('#pStack').value = art.getAttribute('data-stack') || '';
            campos.link.value = art.getAttribute('data-link') || '';
            if (art.getAttribute('data-anio')) seleccionar($('#pAnio'), art.getAttribute('data-anio'));
            full.href = '/subirProyecto?editar=' + encodeURIComponent(id); full.hidden = false;
        } else {
            form.setAttribute('action', form.getAttribute('data-crear'));
            full.hidden = true;
        }
        if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', '');
        campos.nombre.focus();
    }

    // el botón y el mosaico "Añadir" son enlaces al editor completo; con JavaScript abren el diálogo
    $$('#addTop, #add').forEach(function (a) { a.addEventListener('click', function (e) { e.preventDefault(); abrir(null, a); }); });
    $$('.pj .editar').forEach(function (a) { a.addEventListener('click', function (e) { e.preventDefault(); abrir(a.closest('.pj'), a); }); });
    $('#pCancel').addEventListener('click', function () { dlg.close(); });
    dlg.addEventListener('click', function (e) { if (e.target === dlg) dlg.close(); });
    dlg.addEventListener('close', function () { if (opener && opener.focus) opener.focus(); });

    ['nombre', 'categoria', 'tipo', 'descripcion', 'link'].forEach(function (k) {
        var ids = { nombre: 'eNombre', categoria: 'eCategoria', tipo: 'eTipo', descripcion: 'eDesc', link: 'eLink' };
        campos[k].addEventListener('input', function () { if (campos[k].getAttribute('aria-invalid') === 'true') setErr(campos[k], ids[k], ''); });
        campos[k].addEventListener('change', function () { if (campos[k].getAttribute('aria-invalid') === 'true') setErr(campos[k], ids[k], ''); });
    });

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var primero = null;
        function marca(el, malo) { if (malo && !primero) primero = el; }
        marca(campos.nombre, setErr(campos.nombre, 'eNombre', campos.nombre.value.trim() ? '' : 'Escribe un título.'));
        marca(campos.categoria, setErr(campos.categoria, 'eCategoria', campos.categoria.value ? '' : 'Elige una categoría.'));
        marca(campos.tipo, setErr(campos.tipo, 'eTipo', campos.tipo.value ? '' : 'Elige un tipo.'));
        marca(campos.descripcion, setErr(campos.descripcion, 'eDesc', campos.descripcion.value.trim() ? '' : 'Escribe una descripción breve.'));
        var link = campos.link.value.trim();
        marca(campos.link, setErr(campos.link, 'eLink', link && !/^https?:\/\/\S+$/i.test(link) ? 'El enlace debe empezar con http:// o https://.' : ''));
        if (primero) { primero.focus(); return; }
        $('#pSave').disabled = true;
        form.submit();
    });
})();
