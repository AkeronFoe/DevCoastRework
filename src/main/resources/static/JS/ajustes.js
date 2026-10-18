/* DevCoast · Ajustes (diseño náutico).
   Secciones como chips, preferencias de interfaz que se aplican y recuerdan al instante (tema, densidad, texto,
   contraste, movimiento: las gestiona preferencias.js), privacidad y plan contra los mismos endpoints de siempre,
   y los diálogos de correo y de eliminar cuenta. Vanilla, sin librerías. */
(function () {
    'use strict';

    var AJ = window.__AJUSTES__ || {};
    var prefs = window.DevCoastPrefs;

    function $(s, r) { return (r || document).querySelector(s); }
    function $$(s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); }
    function toast(msg, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(msg, opts); }
    function setSwitch(btn, on) { if (btn) btn.setAttribute('aria-checked', on ? 'true' : 'false'); }
    function isOn(btn) { return btn.getAttribute('aria-checked') === 'true'; }

    /* ── secciones: un solo panel visible, con #hash para llegar directo (p. ej. /ajustes#apariencia) ── */
    var tabs = $$('[data-sec]');
    var panels = $$('[data-panel]');

    function mostrar(sec, actualizarHash) {
        var hay = panels.some(function (p) { return p.getAttribute('data-panel') === sec; });
        if (!hay) return false;
        panels.forEach(function (p) { p.hidden = p.getAttribute('data-panel') !== sec; });
        tabs.forEach(function (t) { t.setAttribute('aria-pressed', t.getAttribute('data-sec') === sec ? 'true' : 'false'); });
        if (actualizarHash && window.history && history.replaceState) history.replaceState(null, '', '#' + sec);
        return true;
    }
    tabs.forEach(function (t) {
        t.addEventListener('click', function () { mostrar(t.getAttribute('data-sec'), true); });
    });
    if (!mostrar((location.hash || '').replace('#', ''), false)) mostrar('cuenta', false);
    window.addEventListener('hashchange', function () {
        var s = (location.hash || '').replace('#', '');
        if (s) mostrar(s, false);
    });

    /* ── preferencias de interfaz ── */
    function marcar(sel, attr, valor) {
        $$(sel).forEach(function (b) { b.setAttribute('aria-pressed', b.getAttribute(attr) === valor ? 'true' : 'false'); });
    }
    function pintarPrefs() {
        if (!prefs) return;
        marcar('[data-theme-opt]', 'data-theme-opt', prefs.get('theme'));
        marcar('[data-dens]', 'data-dens', prefs.get('density'));
        marcar('[data-text]', 'data-text', prefs.get('text'));
        setSwitch($('[data-pref="contrast"]'), prefs.get('contrast') === 'high');
        setSwitch($('[data-pref="motion"]'), prefs.get('motion') === true);
    }
    function cambiarPref(nombre, valor, aviso) {
        if (!prefs) return;
        var antes = prefs.get(nombre);
        if (antes === valor) return;
        prefs.set(nombre, valor);
        pintarPrefs();
        toast(aviso, { undo: function () { prefs.set(nombre, antes); pintarPrefs(); } });
    }

    var TEMAS = { dark: 'oscuro', light: 'claro', midnight: 'midnight' };
    var DENS = { compact: 'compacto', normal: 'normal', spacious: 'amplio' };
    var TXT = { small: 'pequeño', normal: 'normal', large: 'grande' };

    $$('[data-theme-opt]').forEach(function (b) {
        b.addEventListener('click', function () {
            var v = b.getAttribute('data-theme-opt');
            cambiarPref('theme', v, 'Tema ' + TEMAS[v]);
        });
    });
    $$('[data-dens]').forEach(function (b) {
        b.addEventListener('click', function () {
            var v = b.getAttribute('data-dens');
            cambiarPref('density', v, 'Espaciado ' + DENS[v]);
        });
    });
    $$('[data-text]').forEach(function (b) {
        b.addEventListener('click', function () {
            var v = b.getAttribute('data-text');
            cambiarPref('text', v, 'Texto ' + TXT[v]);
        });
    });
    var swContraste = $('[data-pref="contrast"]');
    if (swContraste) swContraste.addEventListener('click', function () {
        var on = !isOn(swContraste);
        cambiarPref('contrast', on ? 'high' : 'normal', on ? 'Contraste alto activado' : 'Contraste alto desactivado');
    });
    var swMovimiento = $('[data-pref="motion"]');
    if (swMovimiento) swMovimiento.addEventListener('click', function () {
        var on = !isOn(swMovimiento);
        cambiarPref('motion', on, on ? 'Movimiento reducido activado' : 'Animaciones activadas');
    });
    document.addEventListener('dc:prefs-changed', pintarPrefs);

    /* ── privacidad (solo cuentas de talento) ── */
    var priv = {
        perfilPublico: !!AJ.perfilPublico,
        mostrarCorreo: !!AJ.mostrarCorreo,
        permitirMensajes: AJ.permitirMensajes !== false
    };
    var MSG = {
        perfilPublico: ['Perfil oculto del directorio', 'Perfil público activado'],
        mostrarCorreo: ['Tu correo ya no es visible', 'Tu correo ahora es visible'],
        permitirMensajes: ['Mensajes directos desactivados', 'Las empresas pueden escribirte']
    };

    function pintarVisibilidad() {
        function fila(dot, val, on, si, no) {
            var d = $(dot), v = $(val);
            if (d) d.className = 'aj-dot' + (on ? ' on' : '');
            if (v) v.textContent = on ? si : no;
        }
        fila('#dotPerfil', '#valPerfil', priv.perfilPublico, 'visible en el directorio', 'oculto del directorio');
        fila('#dotCorreo', '#valCorreo', priv.mostrarCorreo, 'visible para empresas', 'oculto');
        fila('#dotMensajes', '#valMensajes', priv.permitirMensajes, 'abierto a invitaciones', 'cerrado a invitaciones');
        var idDir = $('#idDirectorio');
        if (idDir) idDir.textContent = priv.perfilPublico ? 'Sí, visible' : 'No, oculto';
    }

    function guardarPrivacidad(campo, valor) {
        var cuerpo = {}; cuerpo[campo] = valor;
        return fetch('/ajustes/privacidad', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cuerpo)
        }).then(function (r) { return r.json(); })
            .then(function (d) { return !!(d && d.ok); })
            .catch(function () { return false; });
    }

    function aplicarPrivacidad(btn, campo, nuevo, conDeshacer) {
        var antes = priv[campo];
        priv[campo] = nuevo; setSwitch(btn, nuevo); pintarVisibilidad(); btn.disabled = true;
        guardarPrivacidad(campo, nuevo).then(function (ok) {
            btn.disabled = false;
            if (!ok) {
                priv[campo] = antes; setSwitch(btn, antes); pintarVisibilidad();
                toast('No se pudo guardar el cambio. Intenta de nuevo.');
                return;
            }
            toast(MSG[campo][nuevo ? 1 : 0], conDeshacer ? { undo: function () { aplicarPrivacidad(btn, campo, antes, false); } } : undefined);
        });
    }

    $$('[data-priv]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            aplicarPrivacidad(btn, btn.getAttribute('data-priv'), !isOn(btn), true);
        });
    });
    pintarVisibilidad();

    /* ── notificaciones (solo en este navegador: no hay endpoint) ── */
    var NOTIF_KEY = 'dc-notif';
    function leerNotif() {
        try { return JSON.parse(localStorage.getItem(NOTIF_KEY) || '{}') || {}; } catch (e) { return {}; }
    }
    function guardarNotif(o) { try { localStorage.setItem(NOTIF_KEY, JSON.stringify(o)); } catch (e) { /* sin almacenamiento */ } }
    var NOTIF_DEF = { invitaciones: true, solicitudes: true, producto: false };
    var NOTIF_MSG = {
        invitaciones: ['Avisos de invitaciones desactivados', 'Avisos de invitaciones activados'],
        solicitudes: ['Avisos de solicitudes desactivados', 'Avisos de solicitudes activados'],
        producto: ['Novedades del producto desactivadas', 'Novedades del producto activadas']
    };
    var notif = leerNotif();
    $$('[data-notif]').forEach(function (btn) {
        var k = btn.getAttribute('data-notif');
        setSwitch(btn, k in notif ? !!notif[k] : NOTIF_DEF[k]);
        btn.addEventListener('click', function () {
            var antes = isOn(btn), nuevo = !antes;
            setSwitch(btn, nuevo);
            var actual = leerNotif(); actual[k] = nuevo; guardarNotif(actual);
            toast(NOTIF_MSG[k][nuevo ? 1 : 0], {
                undo: function () { setSwitch(btn, antes); var a = leerNotif(); a[k] = antes; guardarNotif(a); }
            });
        });
    });

    /* ── diálogos ── */
    function abrir(d) { if (d.showModal) d.showModal(); else d.setAttribute('open', ''); }
    function cerrar(d) { if (d.close) d.close(); else d.removeAttribute('open'); }
    function cerrarAlClicFuera(d) {
        d.addEventListener('click', function (e) { if (e.target === d) cerrar(d); });
    }

    /* cambiar correo */
    var dlgCorreo = $('#dlgCorreo');
    if (dlgCorreo) {
        var inCorreo = $('#inCorreo'), inPassCorreo = $('#inPassCorreo'), errCorreo = $('#errCorreo');
        var verErr = function (txt) {
            errCorreo.textContent = txt; errCorreo.hidden = !txt;
            inCorreo.setAttribute('aria-invalid', txt ? 'true' : 'false');
        };
        cerrarAlClicFuera(dlgCorreo);
        $('#btnCorreo').addEventListener('click', function () {
            inCorreo.value = ''; inPassCorreo.value = ''; verErr('');
            abrir(dlgCorreo);
        });
        $('#cancelCorreo').addEventListener('click', function () { cerrar(dlgCorreo); });
        $('#saveCorreo').addEventListener('click', function () {
            var nuevoCorreo = inCorreo.value.trim(), password = inPassCorreo.value;
            if (!nuevoCorreo || nuevoCorreo.indexOf('@') < 0) { verErr('Ingresa un correo válido.'); inCorreo.focus(); return; }
            if (!password) { verErr('Ingresa tu contraseña actual.'); inPassCorreo.focus(); return; }
            var btn = this;
            btn.textContent = 'Guardando…'; btn.disabled = true;
            fetch('/ajustes/cambiarCorreo', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nuevoCorreo: nuevoCorreo, password: password })
            }).then(function (r) { return r.json(); }).then(function (d) {
                if (d && d.ok) {
                    cerrar(dlgCorreo);
                    toast('Correo actualizado correctamente');
                    setTimeout(function () { window.location.reload(); }, 1200);
                } else {
                    verErr((d && d.mensaje) || 'Error al actualizar el correo.');
                }
            }).catch(function () {
                verErr('Error de conexión. Intenta de nuevo.');
            }).then(function () {
                btn.textContent = 'Guardar correo'; btn.disabled = false;
            });
        });
    }

    /* eliminar cuenta: el botón se activa solo con la palabra y la contraseña */
    var dlgDel = $('#dlgEliminar');
    if (dlgDel) {
        var inConf = $('#inConfirmar'), inPassDel = $('#inPassDel'), okDel = $('#okEliminar');
        var revisarDel = function () {
            okDel.disabled = !(inConf.value.trim() === 'ELIMINAR' && inPassDel.value.length > 0);
        };
        cerrarAlClicFuera(dlgDel);
        $('#btnEliminar').addEventListener('click', function () {
            inConf.value = ''; inPassDel.value = ''; okDel.disabled = true;
            abrir(dlgDel);
        });
        $('#cancelEliminar').addEventListener('click', function () { cerrar(dlgDel); });
        inConf.addEventListener('input', revisarDel);
        inPassDel.addEventListener('input', revisarDel);
    }

    /* ── plan: actualizar a Pro (cuentas de empresa) ── */
    var btnPro = $('#btnPro');
    if (btnPro) btnPro.addEventListener('click', function () {
        btnPro.textContent = 'Actualizando…'; btnPro.disabled = true;
        fetch('/ajustes/plan/actualizar', { method: 'POST' })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d && d.ok) {
                    toast('Cuenta actualizada a Pro');
                    setTimeout(function () { window.location.reload(); }, 1000);
                } else {
                    toast('No se pudo actualizar el plan');
                    btnPro.textContent = 'Actualizar a Pro'; btnPro.disabled = false;
                }
            })
            .catch(function () {
                toast('Error de conexión');
                btnPro.textContent = 'Actualizar a Pro'; btnPro.disabled = false;
            });
    });

    pintarPrefs();
})();
