// MODO PRESENTACIÓN (solo en la demo de la feria): cuenta la historia completa usando la app real, paso a paso:
// un talento se postula, la empresa lo acepta y publica una oferta, el talento ve el aviso y escribe en el chat, la empresa lo recibe.
// «Manual»: avanzas tú con «Siguiente». «Automático»: avanza solo (con Pausa). Cada paso hace clics reales sobre la pantalla real.
(function () {
    'use strict';

    var CLAVE = 'dc_presentacion';
    var CLAVE_RITMO = 'dc_presentacion_ritmo';
    var $ = function (s, r) { return (r || document).querySelector(s); };
    var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
    var pausa = function (ms) { return new Promise(function (ok) { window.setTimeout(ok, ms); }); };

    function leer() { try { return JSON.parse(sessionStorage.getItem(CLAVE) || 'null'); } catch (e) { return null; } }
    function guardar(s) { try { sessionStorage.setItem(CLAVE, JSON.stringify(s)); } catch (e) { } }
    function limpiar() { try { sessionStorage.removeItem(CLAVE); } catch (e) { } }
    // multiplicador de los tiempos (las pruebas automáticas lo bajan para no esperar)
    function ritmo() { try { var r = parseFloat(sessionStorage.getItem(CLAVE_RITMO)); return r > 0 ? r : 1; } catch (e) { return 1; } }

    function token() { var m = $('meta[name="_csrf"]'); return m ? m.getAttribute('content') : ''; }
    function enviarForm(accion, campos) {
        var f = document.createElement('form');
        f.method = 'post';
        f.action = accion;
        Object.keys(campos || {}).concat(['_csrf']).forEach(function (k) {
            var i = document.createElement('input');
            i.type = 'hidden';
            i.name = k;
            i.value = k === '_csrf' ? token() : campos[k];
            f.appendChild(i);
        });
        document.body.appendChild(f);
        f.submit();
    }
    function cambiarRol(rol) { enviarForm('/demo/' + rol, { presentacion: '1' }); }
    function esEmpresa() { return document.body.classList.contains('empresa'); }

    // ------------------------------------------------------------------ pasos
    function porTexto(selector, texto) {
        return $$(selector).filter(function (e) { return (e.textContent || '').trim().indexOf(texto) !== -1; })[0] || null;
    }
    function tarjetaDeCarlosDeHoy() {
        return $$('article.rq').filter(function (a) {
            var cuando = $('.when', a);
            return /Carlos/.test(a.getAttribute('data-nombre') || '') && cuando && /hoy/.test(cuando.textContent) && a.getAttribute('data-estado') === 'revision';
        })[0] || null;
    }
    function escribir(el, texto) {
        var i = 0;
        return new Promise(function (ok) {
            (function paso() {
                if (i > texto.length) { ok(); return; }
                el.value = texto.slice(0, i++);
                el.dispatchEvent(new Event('input', { bubbles: true }));
                window.setTimeout(paso, 28 * ritmo());
            })();
        });
    }
    function reiniciarDemo() {
        return fetch('/demo/reiniciar', { method: 'POST', redirect: 'manual' }).catch(function () { });
    }

    var PASOS = [
        { rol: 'talento', ir: '/dashboard', sel: '.hello', lectura: 2200,
            t: 'Preparando la demo…', d: 'Dejamos todo como el primer día para contar la historia.',
            accion: function () { return reiniciarDemo().then(function () { return 'recargar'; }); } },
        { rol: 'talento', ir: '/dashboard', sel: '.hello', lectura: 5200,
            t: 'Carlos busca trabajo', d: 'Carlos es desarrollador full stack. Aquí ve el estado de cada una de sus postulaciones y sus avisos.' },
        { rol: 'talento', ir: '/empleos', lectura: 5200,
            sel: function () { var b = porTexto('button', 'Postularme'); return b ? (b.closest('article') || b) : null; },
            t: 'Una oferta que encaja al 100 %', d: 'DevCoast calcula cuánto de lo que pide la oferta ya está en su perfil. Esta es su mejor opción.' },
        { rol: 'talento', ir: '/empleos', lectura: 3000, opcional: true,
            sel: function () { return porTexto('button', 'Postularme'); },
            t: 'Se postula con un clic', d: 'Un clic y la empresa ya tiene su postulación.',
            accion: function (el) { el.click(); return pausa(1400 * ritmo()); } },

        { rol: 'empresa', ir: '/candidatos', lectura: 5200, opcional: true, sel: tarjetaDeCarlosDeHoy,
            t: 'La empresa la ve llegar', d: 'Del otro lado, DevCoast Labs recibe la postulación al instante, con el match de la persona.' },
        { rol: 'empresa', ir: '/candidatos', lectura: 3200, opcional: true, sel: tarjetaDeCarlosDeHoy,
            t: 'La acepta', d: 'Con un clic acepta a Carlos. Él recibirá un aviso.',
            accion: function (art) {
                var resumen = $('summary', art);
                if (resumen) resumen.click();
                return pausa(700 * ritmo()).then(function () {
                    var ok = $('button[aria-label^="Sí, aceptar"]', art);
                    if (ok) ok.click();
                    return pausa(1500 * ritmo());
                });
            } },
        { rol: 'empresa', ir: '/crearEmpleo', lectura: 3800, sel: '#btn-ejemplo', opcional: true,
            t: 'Y publica una oferta nueva', d: 'Publicar una oferta toma cuatro pasos. Aquí los llenamos con un ejemplo para que lo veas rápido.',
            accion: function (el) { el.click(); return pausa(1200 * ritmo()); } },
        { rol: 'empresa', ir: '/crearEmpleo', lectura: 3800, sel: '#btn-publicar', opcional: true,
            t: 'Revisa y publica', d: 'Antes de publicar se ve un resumen, con una vista previa de cómo la verá el talento.',
            accion: function (el) { el.click(); return pausa(1800 * ritmo()); } },
        { rol: 'empresa', donde: /^\/misEmpleos/, lectura: 4200, sel: function () { return $('main article') || $('main'); },
            t: 'Ya está publicada', d: 'La oferta aparece en «Mis ofertas», lista para recibir postulaciones.' },

        { rol: 'talento', ir: '/dashboard', lectura: 3200, sel: '#bellBtn',
            t: 'Carlos recibe el aviso', d: 'La campana le avisa que la empresa aceptó su postulación.',
            accion: function (el) { el.click(); return pausa(900 * ritmo()); } },
        { rol: 'talento', ir: '/dashboard', lectura: 3200, opcional: true,
            sel: function () { return porTexto('#bellPanel a', 'aceptó tu postulación'); },
            t: 'Abre la solicitud', d: 'Desde el aviso llega directo a su solicitud.',
            accion: function (el) { el.click(); return pausa(1500 * ritmo()); } },
        { rol: 'talento', donde: /^\/solicitudes\/[0-9a-f]{24}$/, lectura: 3600, opcional: true,
            sel: function () { return $('main a[href^="/chat/"]'); },
            t: 'Escribe a la empresa', d: 'La solicitud aceptada abre el chat con la empresa.',
            accion: function (el) { el.click(); return pausa(1500 * ritmo()); } },
        { rol: 'talento', donde: /^\/chat\/[0-9a-f]{24}$/, lectura: 2600, opcional: true, sel: '#mxTexto',
            t: 'Conversan sin salir de la plataforma', d: 'Carlos le escribe a la empresa.',
            accion: function (campo) {
                return escribir(campo, '¡Hola! Muchas gracias por aceptarme, quedo atento a los siguientes pasos.').then(function () {
                    return pausa(500 * ritmo());
                }).then(function () {
                    var enviar = $('#mxSend');
                    if (enviar) enviar.click();
                    return pausa(1400 * ritmo());
                });
            } },

        { rol: 'empresa', ir: '/mensajes', lectura: 4200, sel: function () { return $('#mxItems li') || $('main'); },
            t: 'La empresa recibe el mensaje', d: 'El mensaje de Carlos ya está en la bandeja de DevCoast Labs.' },
        { rol: 'empresa', ir: '/talentos', lectura: 4200, sel: function () { return $('main a[href^="/talentos/"]') || $('main'); },
            t: 'Y también puede buscar talento', d: 'En el directorio la empresa explora perfiles y proyectos, e invita a quien le interese.' }
    ];

    // ------------------------------------------------------------------ panel
    var panel, temporizador, generacion = 0, resaltado = null, ocupado = false;

    function quitarResaltado() {
        if (resaltado) resaltado.classList.remove('pres-on');
        resaltado = null;
    }
    function crearPanel() {
        if (panel) return panel;
        panel = document.createElement('div');
        panel.className = 'pres-panel';
        panel.setAttribute('role', 'region');
        panel.setAttribute('aria-label', 'Modo presentación');
        panel.setAttribute('aria-live', 'polite');
        document.body.appendChild(panel);
        return panel;
    }
    function boton(texto, clase, accion, etiqueta) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = clase;
        b.textContent = texto;
        if (etiqueta) b.setAttribute('aria-label', etiqueta);
        b.addEventListener('click', accion);
        return b;
    }
    function pintar(titulo, texto, s, opciones) {
        opciones = opciones || {};
        var p = crearPanel();
        p.innerHTML = '';
        var fila = document.createElement('div');
        fila.className = 'pres-top';
        var etiqueta = document.createElement('small');
        var actual = PASOS[s.paso];
        etiqueta.textContent = 'MODO PRESENTACIÓN' + (opciones.fin ? ' · FIN' : ' · PASO ' + Math.min(s.paso + 1, PASOS.length) + ' DE ' + PASOS.length);
        var rol = document.createElement('span');
        rol.className = 'pres-rol ' + (esEmpresa() ? 'emp' : 'tal');
        rol.textContent = esEmpresa() ? 'EMPRESA' : 'TALENTO';
        fila.appendChild(etiqueta);
        if (!opciones.mensaje) fila.appendChild(rol);
        var h = document.createElement('b');
        h.textContent = titulo;
        var d = document.createElement('p');
        d.textContent = texto;
        var barra = document.createElement('div');
        barra.className = 'pres-acts';
        if (opciones.fin) {
            barra.appendChild(boton('Repetir', 'pres-next', repetir));
            barra.appendChild(boton('Salir', 'pres-sec', salir));
        } else if (!opciones.mensaje) {
            barra.appendChild(boton(s.modo === 'auto' ? 'Manual' : 'Automático', 'pres-sec', function () { alternarModo(); },
                s.modo === 'auto' ? 'Cambiar a modo manual' : 'Cambiar a modo automático'));
            if (s.modo === 'auto')
                barra.appendChild(boton(s.pausado ? 'Seguir' : 'Pausa', 'pres-sec', alternarPausa));
            barra.appendChild(boton('Siguiente', 'pres-next', function () { hacer(); }));
            barra.appendChild(boton('✕', 'pres-x', salir, 'Salir del modo presentación'));
        } else {
            barra.appendChild(boton('✕', 'pres-x', salir, 'Salir del modo presentación'));
        }
        p.appendChild(fila);
        p.appendChild(h);
        p.appendChild(d);
        p.appendChild(barra);
        if (actual && !opciones.mensaje && !opciones.fin && s.modo === 'auto') {
            var progreso = document.createElement('i');
            progreso.className = 'pres-progreso';
            progreso.style.animationDuration = Math.round((actual.lectura || 4000) * ritmo()) + 'ms';
            if (s.pausado) progreso.style.animationPlayState = 'paused';
            p.appendChild(progreso);
        }
    }

    // ------------------------------------------------------------------ motor
    function buscar(sel) {
        if (!sel) return Promise.resolve(null);
        var intentos = 0;
        return new Promise(function (ok) {
            (function probar() {
                var el = typeof sel === 'function' ? sel() : $(sel);
                if (el || ++intentos > 25) { ok(el || null); return; }
                window.setTimeout(probar, 200);
            })();
        });
    }

    function ejecutar() {
        var s = leer();
        if (!s || !s.activo) return;
        if (!$('.demo-card')) { limpiar(); return; } // no es una sesión de demo: nada que presentar
        var p = PASOS[s.paso];
        if (!p) { terminar(s); return; }

        if (p.rol && (p.rol === 'empresa') !== esEmpresa()) {
            s.cambios = (s.cambios || 0) + 1;
            if (s.cambios > 3) { pintar('No pude cambiar de cuenta', 'Sal del modo presentación y vuelve a empezar desde la pantalla de la demo.', s, { mensaje: true }); return; }
            guardar(s);
            pintar('Cambiando a la cuenta de ' + (p.rol === 'empresa' ? 'la empresa' : 'Carlos') + '…', 'Un momento.', s, { mensaje: true });
            cambiarRol(p.rol);
            return;
        }
        if (s.cambios) { s.cambios = 0; guardar(s); }
        if (p.ir && location.pathname !== p.ir) { location.href = p.ir; return; }
        if (p.donde && !p.donde.test(location.pathname)) {
            s.esperas = (s.esperas || 0) + 1;
            guardar(s);
            if (s.esperas > 6) { s.esperas = 0; guardar(s); pintar('Me perdí', 'Esta pantalla no es la que esperaba. Puedes salir y volver a empezar.', s, { mensaje: true }); return; }
            window.setTimeout(ejecutar, 800);
            return;
        }
        if (s.esperas) { s.esperas = 0; guardar(s); }
        mostrar(p, s);
    }

    function mostrar(p, s) {
        var mia = ++generacion;
        clearTimeout(temporizador);
        quitarResaltado();
        pintar(p.t, p.d, s);
        if (panel) panel.classList.remove('pres-arriba');
        buscar(p.sel).then(function (el) {
            if (mia !== generacion) return;
            if (!el && p.opcional) { omitir(); return; }
            if (el) {
                el.classList.add('pres-on');
                resaltado = el;
                p.objetivo = el;
                try { el.scrollIntoView({ block: 'center', behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }); } catch (e) { }
                // si lo señalado queda en la parte baja de la pantalla, el panel sube para no taparlo
                window.setTimeout(function () {
                    if (mia !== generacion || !panel) return;
                    panel.classList.toggle('pres-arriba', el.getBoundingClientRect().top > window.innerHeight * 0.5);
                }, 600);
            }
            var actual = leer();
            if (actual && actual.modo === 'auto' && !actual.pausado)
                temporizador = window.setTimeout(hacer, (p.lectura || 4000) * ritmo());
        });
    }

    function omitir() {
        var s = leer();
        if (!s) return;
        s.paso++;
        guardar(s);
        ejecutar();
    }

    // hace la acción del paso actual (clics reales) y pasa al siguiente
    function hacer() {
        if (ocupado) return;
        var s = leer();
        if (!s || !s.activo) return;
        var p = PASOS[s.paso];
        if (!p) return;
        ocupado = true;
        clearTimeout(temporizador);
        generacion++;
        quitarResaltado();
        var el = p.objetivo || null;
        p.objetivo = null;
        s.paso++; // se guarda antes: si la acción cambia de página, la nueva página sigue desde el paso siguiente
        guardar(s);
        Promise.resolve().then(function () { return p.accion && (el || !p.sel) ? p.accion(el) : null; })
            .catch(function () { })
            .then(function (resultado) {
                ocupado = false;
                if (resultado === 'recargar') { location.reload(); return; }
                ejecutar();
            });
    }

    function terminar(s) {
        generacion++;
        quitarResaltado();
        pintar('¡Eso es DevCoast!', 'Un talento encontró una oferta y se postuló, la empresa lo aceptó y publicó otra, y conversaron sin salir de la plataforma.' +
            (s.loop ? ' Volvemos a empezar en unos segundos.' : ''), s, { fin: true });
        if (s.loop) temporizador = window.setTimeout(repetir, 9000 * ritmo());
    }

    function repetir() {
        clearTimeout(temporizador);
        var s = leer();
        if (!s) return;
        s.paso = 0;
        s.cambios = 0;
        s.esperas = 0;
        guardar(s);
        ejecutar();
    }

    function salir() {
        clearTimeout(temporizador);
        generacion++;
        quitarResaltado();
        limpiar();
        if (panel) { panel.remove(); panel = null; }
    }

    function alternarModo() {
        var s = leer();
        if (!s) return;
        s.modo = s.modo === 'auto' ? 'manual' : 'auto';
        s.pausado = false;
        guardar(s);
        mostrar(PASOS[s.paso] || PASOS[PASOS.length - 1], s);
    }

    function alternarPausa() {
        var s = leer();
        if (!s) return;
        s.pausado = !s.pausado;
        guardar(s);
        mostrar(PASOS[s.paso], s);
    }

    document.addEventListener('keydown', function (e) {
        var s = leer();
        if (!s || !s.activo || !panel) return;
        var escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test((document.activeElement || {}).tagName || '');
        if (e.key === 'Escape') salir();
        else if (e.key === 'ArrowRight' && !escribiendo && s.modo !== 'auto') hacer();
    });

    // ------------------------------------------------------------------ entradas
    // desde el menú de la demo (manual) o desde la pantalla /demo (automático, en bucle)
    function empezar(modo, bucle) {
        guardar({ activo: true, paso: 0, modo: modo, loop: !!bucle, pausado: false, cambios: 0, esperas: 0 });
        if ($('.demo-card')) ejecutar();
        else cambiarRol('talento');
    }

    document.addEventListener('click', function (e) {
        var m = e.target.closest ? e.target.closest('[data-presentar]') : null;
        if (!m) return;
        e.preventDefault();
        empezar(m.getAttribute('data-presentar') === 'auto' ? 'auto' : 'manual', m.getAttribute('data-presentar') === 'auto');
    });

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ejecutar);
    else ejecutar();
})();
