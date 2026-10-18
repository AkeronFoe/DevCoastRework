/* DevCoast · Mi perfil (talento y empresa). Pase vertical con el formulario de siempre.
   Guarda con POST /perfil/guardar (JSON), sube la foto a /perfil/foto y el interruptor "Destacar" usa /ajustes/privacidad.
   Después de guardar, trae /perfil otra vez y reemplaza las partes calculadas por el servidor (vista previa, pendientes, logros). */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    var P = window.PERFIL || {};
    var form = $('#pfForm');
    if (!form) return;
    var esEmpresa = P.tipo === 'empresa';
    var status = $('#pfStatus'), saveBtn = $('#pfSave');

    /* ── chips de stack y habilidades (solo talento) ── */
    var listas = { stack: (P.stack || []).slice(), hab: (P.hab || []).slice() };
    var maximos = { stack: 50, hab: 30 };
    function pintar(clave) {
        var box = $('[data-lista="' + clave + '"]');
        if (!box) return;
        $$('.pill', box).forEach(function (p) { p.remove(); });
        var input = $('input', box);
        listas[clave].forEach(function (v, i) {
            var s = document.createElement('span');
            s.className = 'pill';
            s.appendChild(document.createTextNode(v));
            var b = document.createElement('button');
            b.type = 'button';
            b.setAttribute('aria-label', T('Quitar') + ' ' + v);
            b.textContent = '✕';
            b.addEventListener('click', function () { listas[clave].splice(i, 1); pintar(clave); input.focus(); marcar(); });
            s.appendChild(b);
            box.insertBefore(s, input);
        });
    }
    function agregar(clave, texto) {
        var v = texto.replace(/,+$/, '').trim();
        if (!v) return false;
        var l = listas[clave];
        if (l.length >= maximos[clave]) return false;
        if (l.some(function (x) { return x.toLowerCase() === v.toLowerCase(); })) return false;
        l.push(v);
        pintar(clave);
        return true;
    }
    $$('[data-lista]').forEach(function (box) {
        var clave = box.getAttribute('data-lista'), input = $('input', box);
        pintar(clave);
        box.addEventListener('click', function (e) { if (e.target === box) input.focus(); });
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ',') {
                e.preventDefault();
                if (agregar(clave, input.value)) marcar();
                input.value = '';
            } else if (e.key === 'Backspace' && !input.value && listas[clave].length) {
                listas[clave].pop(); pintar(clave); marcar();
            }
        });
        // lo que quedó escrito sin Enter también cuenta al salir del campo
        input.addEventListener('blur', function () { if (input.value.trim() && agregar(clave, input.value)) { input.value = ''; marcar(); } });
    });

    /* ── contador de la biografía ── */
    var bio = $('#pfBio'), cnt = $('#pfBioC');
    if (bio && cnt) bio.addEventListener('input', function () { cnt.textContent = bio.value.length; });

    /* ── enlaces: solo http(s) ── */
    function urlValida(input) {
        var v = input.value.trim();
        var ok = v === '' || /^https?:\/\/\S+$/i.test(v);
        var err = document.getElementById(input.getAttribute('aria-describedby'));
        input.setAttribute('aria-invalid', ok ? 'false' : 'true');
        if (err) err.hidden = ok;
        return ok;
    }
    // el error se muestra al enviar y se quita mientras se corrige (nunca al salir del campo: un cambio de altura
    // justo al pulsar "Guardar" movería el botón y se perdería el clic)
    $$('[data-url]').forEach(function (i) {
        i.addEventListener('input', function () { if (i.getAttribute('aria-invalid') === 'true') urlValida(i); });
    });
    var nombreIn = $('#pfNombre');
    if (nombreIn) nombreIn.addEventListener('input', function () {
        if (nombreIn.value.trim()) { nombreIn.setAttribute('aria-invalid', 'false'); var e = $('#pfNombreE'); if (e) e.hidden = true; }
    });

    /* ── carga útil según el tipo de cuenta ── */
    function val(id) { var e = document.getElementById(id); return e ? e.value.trim() : ''; }
    function carga() {
        if (esEmpresa) return {
            nombre: val('pfNombre'), rolTitulo: val('pfSector'), bio: val('pfBio'), ubicacion: val('pfUbi'),
            portfolioUrl: val('pfWeb'), tamano: val('pfTam'), telefono: val('pfTel'),
            nombreRepresentante: val('pfRep'), cargoRepresentante: val('pfCargo')
        };
        return {
            nombre: val('pfNombre'), rolTitulo: val('pfRol'), bio: val('pfBio'), emailContacto: val('pfEmail'),
            telefono: val('pfTel'), portfolioUrl: val('pfWeb'), linkedinUrl: val('pfLi'), githubUrl: val('pfGh'),
            ubicacion: val('pfUbi'), modalidad: val('pfMod'), disponibilidad: val('pfDisp'),
            salarioEsperado: val('pfSal'), especialidad: val('pfEsp'),
            stack: listas.stack.slice(), habilidadesBlandas: listas.hab.slice()
        };
    }
    var base = JSON.stringify(carga());
    var sucio = false;
    function marcar() {
        var ahora = JSON.stringify(carga()) !== base;
        if (ahora === sucio) return;
        sucio = ahora;
        form.classList.toggle('dirty', sucio);
        if (status && !status.classList.contains('err')) status.textContent = sucio ? T('Cambios sin guardar') : '';
    }
    form.addEventListener('input', marcar);
    form.addEventListener('change', marcar);
    window.addEventListener('beforeunload', function (e) { if (sucio) { e.preventDefault(); e.returnValue = ''; } });

    function aviso(txt, error) {
        if (!status) return;
        status.textContent = txt || '';
        status.classList.toggle('err', !!error);
    }

    /* ── guardar ── */
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        var nombre = $('#pfNombre'), eNombre = $('#pfNombreE');
        var ok = true;
        if (!nombre.value.trim()) {
            nombre.setAttribute('aria-invalid', 'true'); eNombre.hidden = false; nombre.focus(); ok = false;
        } else { nombre.setAttribute('aria-invalid', 'false'); eNombre.hidden = true; }
        $$('[data-url]').forEach(function (i) { if (!urlValida(i) && ok) { i.focus(); ok = false; } else if (!urlValida(i)) ok = false; });
        if (!ok) { aviso(T('Revisa los campos marcados.'), true); return; }

        var datos = carga();
        saveBtn.disabled = true;
        saveBtn.textContent = T('Guardando…');
        aviso('');
        fetch('/perfil/guardar', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, body: JSON.stringify(datos) })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { return { ok: r.ok, d: d }; }); })
            .then(function (res) {
                if (res.ok && res.d.ok) {
                    base = JSON.stringify(datos); sucio = false; form.classList.remove('dirty');
                    aviso(T('Guardado.'));
                    toast(T('Perfil guardado.'));
                    refrescar();
                } else {
                    aviso(T('No se pudo guardar') + (res.d && res.d.error ? ': ' + res.d.error : '.'), true);
                }
            })
            .catch(function () { aviso(T('Sin conexión. Inténtalo de nuevo.'), true); })
            .then(function () { saveBtn.disabled = false; saveBtn.textContent = T('Guardar cambios'); });
    });

    /* ── trae /perfil y reemplaza lo que calcula el servidor ── */
    function refrescar() {
        fetch('/perfil', { headers: { 'Accept': 'text/html' } })
            .then(function (r) { return r.ok ? r.text() : Promise.reject(); })
            .then(function (html) {
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var lado = $('.pf-side'), nuevoLado = $('.pf-side', doc);
                if (lado && nuevoLado) lado.innerHTML = nuevoLado.innerHTML;
                ['#pfBar', '#pfPct', '#pfSub', '.prog', '.who b'].forEach(function (sel) {
                    var a = $(sel), b = $(sel, doc);
                    if (a && b) a.innerHTML = b.innerHTML;
                    if (a && b && sel === '#pfBar') a.setAttribute('aria-valuenow', b.getAttribute('aria-valuenow'));
                });
                var av = $('.user .av'), avN = $('.user .av', doc);
                if (av && avN) av.replaceWith(document.importNode(avN, true));
            })
            .catch(function () { /* si falla, la página sigue siendo válida: se actualiza al recargar */ });
    }

    /* ── foto (talento y empresa) ── */
    var file = $('#pfFile'), avatar = $('#pfAvatar'), quitar = $('#pfQuitar');
    function abrirFoto() { if (file) file.click(); }
    if (avatar) avatar.addEventListener('click', abrirFoto);
    var cambiar = $('#pfFoto'); if (cambiar) cambiar.addEventListener('click', abrirFoto);
    if (file) file.addEventListener('change', function () {
        var f = file.files[0];
        if (!f) return;
        var fd = new FormData(); fd.append('file', f);
        fetch('/perfil/foto', { method: 'POST', body: fd })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.ok) {
                    avatar.innerHTML = '';
                    var img = document.createElement('img'); img.src = d.url; img.alt = '';
                    avatar.appendChild(img);
                    if (quitar) quitar.hidden = false;
                    toast(T('Foto actualizada.'));
                    refrescar();
                } else toast(T('No se pudo subir la foto.'));
            })
            .catch(function () { toast(T('No se pudo subir la foto.')); })
            .then(function () { file.value = ''; });
    });
    if (quitar) quitar.addEventListener('click', function () {
        fetch('/perfil/foto/quitar', { method: 'POST' })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.ok) {
                    avatar.innerHTML = '<b></b>';
                    $('b', avatar).textContent = avatar.getAttribute('data-inicial') || 'D';
                    quitar.hidden = true;
                    toast(T('Foto eliminada.'));
                    refrescar();
                } else toast(T('No se pudo quitar la foto.'));
            })
            .catch(function () { toast(T('No se pudo quitar la foto.')); });
    });

    /* ── "Destacar mi perfil" (se guarda al instante, como los interruptores de Ajustes) ── */
    document.addEventListener('click', function (e) {
        var sw = e.target.closest ? e.target.closest('#pfDestacar') : null;
        if (!sw) return;
        var nuevo = sw.getAttribute('aria-checked') !== 'true';
        sw.disabled = true;
        fetch('/ajustes/privacidad', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' }, body: JSON.stringify({ destacar: nuevo }) })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.ok) {
                    sw.setAttribute('aria-checked', nuevo ? 'true' : 'false');
                    $('span', sw).textContent = nuevo ? T('SÍ') : T('NO');
                    toast(nuevo ? T('Tu perfil aparecerá primero en el Directorio.') : T('Tu perfil ya no está destacado.'));
                    refrescar();
                } else toast(T('No se pudo cambiar. Inténtalo de nuevo.'));
            })
            .catch(function () { toast(T('No se pudo cambiar. Inténtalo de nuevo.')); })
            .then(function () { sw.disabled = false; });
    });
})();
