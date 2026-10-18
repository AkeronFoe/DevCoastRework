(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    var pdf = $('#btnPdf');
    if (pdf) pdf.addEventListener('click', function () {
        var antes = document.title;
        var nombre = ($('.cv-name') || {}).textContent;
        document.title = nombre && nombre.trim() ? nombre.trim() + ' – CV' : 'CV DevCoast';
        window.print();
        document.title = antes;
    });

    try {
        if (sessionStorage.getItem('dc-cv-guardado') === '1') {
            sessionStorage.removeItem('dc-cv-guardado');
            toast('Cambios guardados.');
        }
    } catch (e) { }

    var dlg = $('#dlgCv');
    if (!dlg) return;

    var tabs = $$('.cv-tab', dlg);
    function activar(nombre) {
        var existe = false;
        tabs.forEach(function (t) {
            var on = t.getAttribute('data-tab') === nombre;
            if (on) existe = true;
            t.setAttribute('aria-selected', on ? 'true' : 'false');
            t.tabIndex = on ? 0 : -1;
        });
        if (!existe) return;
        $$('.cv-panel', dlg).forEach(function (p) { p.hidden = p.id !== 'panel-' + nombre; });
    }
    tabs.forEach(function (t) {
        t.addEventListener('click', function () { activar(t.getAttribute('data-tab')); });
        t.addEventListener('keydown', function (e) {
            var i = tabs.indexOf(t), n = null;
            if (e.key === 'ArrowRight') n = tabs[(i + 1) % tabs.length];
            else if (e.key === 'ArrowLeft') n = tabs[(i - 1 + tabs.length) % tabs.length];
            if (n) { e.preventDefault(); activar(n.getAttribute('data-tab')); n.focus(); }
        });
    });

    function abrir(tab) {
        activar(tab || 'perfil');
        var err = $('#cvErr', dlg); if (err) err.hidden = true;
        if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', '');
    }
    function cerrar() { if (dlg.close) dlg.close(); else dlg.removeAttribute('open'); }

    var editar = $('#btnEditar');
    if (editar) editar.addEventListener('click', function () { abrir('perfil'); });
    document.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('[data-abrir]') : null;
        if (b) { e.preventDefault(); abrir(b.getAttribute('data-abrir')); }
    });
    $('#cvCerrar').addEventListener('click', cerrar);
    $('#cvCancelar').addEventListener('click', cerrar);
    dlg.addEventListener('click', function (e) { if (e.target === dlg) cerrar(); });

    var bio = $('#inBio'), bioCount = $('#bioCount'), BIO_MAX = 400;
    function contarBio() {
        if (!bio || !bioCount) return;
        bioCount.textContent = (BIO_MAX - bio.value.length) + ' caracteres restantes';
    }
    if (bio) { bio.addEventListener('input', contarBio); contarBio(); }

    var logros = $('#inBullets');
    if (logros) {
        var crudo = logros.getAttribute('data-lineas') || '';
        logros.value = crudo ? crudo.split('||').join('\n') : '';
    }

    function normalizar(v) {
        return (v || '').toString().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[-_]/g, ' ').trim();
    }
    function iniciarMulti(sel, inicial) {
        var ini = (inicial || '').split(',').map(function (v) { return normalizar(v); }).filter(Boolean);
        $$(sel + ' button', dlg).forEach(function (b) {
            b.setAttribute('aria-pressed', ini.indexOf(normalizar(b.getAttribute('data-val'))) >= 0 ? 'true' : 'false');
            b.addEventListener('click', function () {
                b.setAttribute('aria-pressed', b.getAttribute('aria-pressed') === 'true' ? 'false' : 'true');
            });
        });
    }
    function elegidos(sel) {
        return $$(sel + ' button[aria-pressed="true"]', dlg).map(function (b) { return b.getAttribute('data-val'); }).join(', ');
    }
    iniciarMulti('#prefMod', dlg.getAttribute('data-mod'));
    iniciarMulti('#prefCon', dlg.getAttribute('data-disp'));

    var nivel = { es: parseInt(dlg.getAttribute('data-es'), 10) || 5, en: parseInt(dlg.getAttribute('data-en'), 10) || 4 };
    function iniciarIdioma(sel, clave) {
        var botones = $$(sel + ' button', dlg);
        var pintar = function () {
            botones.forEach(function (b) { b.setAttribute('aria-pressed', parseInt(b.getAttribute('data-level'), 10) === nivel[clave] ? 'true' : 'false'); });
        };
        botones.forEach(function (b) {
            b.addEventListener('click', function () { nivel[clave] = parseInt(b.getAttribute('data-level'), 10); pintar(); });
        });
        pintar();
    }
    iniciarIdioma('#langEs', 'es');
    iniciarIdioma('#langEn', 'en');

    function renumerar(lista, etiqueta) {
        $$('.cv-item', lista).forEach(function (it, i) {
            var l = $('.cv-item-l', it); if (l) l.textContent = etiqueta + ' ' + (i + 1);
        });
    }
    function iniciarLista(listaSel, tplSel, btnSel, etiqueta) {
        var lista = $(listaSel, dlg), tpl = $(tplSel, dlg), btn = $(btnSel, dlg);
        renumerar(lista, etiqueta);
        btn.addEventListener('click', function () {
            lista.appendChild(tpl.content.cloneNode(true));
            renumerar(lista, etiqueta);
            var ult = lista.lastElementChild; var f = ult && $('input', ult); if (f) f.focus();
        });
        lista.addEventListener('click', function (e) {
            var q = e.target.closest ? e.target.closest('[data-quitar]') : null;
            if (!q) return;
            var it = q.closest('.cv-item'); if (it) it.remove();
            renumerar(lista, etiqueta);
        });
    }
    iniciarLista('#expLista', '#tplExp', '#expAgregar', 'Experiencia');
    iniciarLista('#eduLista', '#tplEdu', '#eduAgregar', 'Educación');

    var softWrap = $('#softWrap', dlg), softIn = $('#softIn', dlg);
    function valoresSoft() { return $$('.cv-chip', softWrap).map(function (c) { return c.getAttribute('data-v'); }); }
    function agregarSoft(v) {
        v = (v || '').trim().replace(/,$/, '');
        if (!v || valoresSoft().indexOf(v) >= 0) return;
        var chip = document.createElement('span'); chip.className = 'cv-chip'; chip.setAttribute('data-v', v);
        var t = document.createElement('span'); t.textContent = v;
        var x = document.createElement('button'); x.type = 'button'; x.textContent = '×'; x.setAttribute('aria-label', 'Quitar ' + v);
        chip.appendChild(t); chip.appendChild(x);
        softWrap.insertBefore(chip, softIn);
    }
    softIn.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); agregarSoft(softIn.value); softIn.value = ''; }
        else if (e.key === 'Backspace' && !softIn.value) { var ch = $$('.cv-chip', softWrap).pop(); if (ch) ch.remove(); }
    });
    softIn.addEventListener('blur', function () { if (softIn.value.trim()) { agregarSoft(softIn.value); softIn.value = ''; } });
    softWrap.addEventListener('click', function (e) {
        var x = e.target.closest ? e.target.closest('.cv-chip button') : null;
        if (x) { x.closest('.cv-chip').remove(); softIn.focus(); }
        else if (e.target === softWrap) softIn.focus();
    });

    function val(id) { var el = document.getElementById(id); return el ? el.value.trim() : ''; }
    function texto(it, c) { var el = $(c, it); return el ? el.value.trim() : ''; }

    $('#cvGuardar').addEventListener('click', function () {
        var btn = this, err = $('#cvErr', dlg);
        agregarSoft(softIn.value); softIn.value = '';

        var cvExperiencias = [];
        $$('#expLista .cv-item', dlg).forEach(function (it) {
            var cargo = texto(it, '.exp-cargo'), empresa = texto(it, '.exp-empresa');
            if (!cargo && !empresa) return;
            cvExperiencias.push({ cargo: cargo, empresa: empresa, periodo: texto(it, '.exp-periodo'),
                modalidad: texto(it, '.exp-modalidad'), descripcion: texto(it, '.exp-desc') });
        });
        var cvEducaciones = [];
        $$('#eduLista .cv-item', dlg).forEach(function (it) {
            var institucion = texto(it, '.edu-institucion'), titulo = texto(it, '.edu-titulo');
            if (!institucion && !titulo) return;
            cvEducaciones.push({ institucion: institucion, titulo: titulo, periodo: texto(it, '.edu-periodo'),
                descripcion: texto(it, '.edu-descripcion') });
        });
        var lineas = logros.value.trim();

        var payload = {
            nombre: val('inNombre'), rolTitulo: val('inRol'), bio: val('inBio'),
            cvBullets: lineas ? lineas.split('\n').map(function (l) { return l.trim(); }).filter(Boolean) : [],
            emailContacto: val('inEmail'), telefono: val('inTel'), portfolioUrl: val('inWeb'),
            linkedinUrl: val('inLi'), githubUrl: val('inGh'), ubicacion: val('inUbi'),
            modalidad: elegidos('#prefMod'), disponibilidad: elegidos('#prefCon'),
            salarioEsperado: val('inSal'), especialidad: val('inEsp'),
            nivelEspanol: nivel.es, nivelIngles: nivel.en,
            habilidadesBlandas: valoresSoft(), cvExperiencias: cvExperiencias, cvEducaciones: cvEducaciones
        };

        btn.disabled = true; btn.textContent = 'Guardando…';
        fetch('/curriculum/guardar', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.ok) {
                    try { sessionStorage.setItem('dc-cv-guardado', '1'); } catch (e) { }
                    window.location.reload();
                } else {
                    err.textContent = (d && d.error) || 'No se pudo guardar. Intenta de nuevo.'; err.hidden = false;
                    btn.disabled = false; btn.textContent = 'Guardar cambios';
                }
            })
            .catch(function () {
                err.textContent = 'Error de red. Intenta de nuevo.'; err.hidden = false;
                btn.disabled = false; btn.textContent = 'Guardar cambios';
            });
    });
})();
