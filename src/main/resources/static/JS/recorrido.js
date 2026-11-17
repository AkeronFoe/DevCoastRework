// recorrido guiado de la primera vez: globos que señalan lo importante del menú. Se puede saltar y se vuelve a ver desde Ajustes
(function () {
    'use strict';

    var CLAVE = 'dc_recorrido_v1';
    var $ = function (s, r) { return (r || document).querySelector(s); };
    var esEmpresa = document.body.classList.contains('empresa');

    var PASOS = esEmpresa ? [
        { sel: '.menu a[href$="/misEmpleos"]', t: 'Tus ofertas', d: 'Aquí ves todas tus vacantes: publícalas, edítalas, ciérralas o destácalas.' },
        { sel: '.menu a[href$="/candidatos"]', t: 'Postulaciones', d: 'Las personas que se postulan llegan aquí. Acepta o rechaza y mueve cada oferta entre fases.' },
        { sel: '.menu a[href$="/crearEmpleo"]', t: 'Crear una oferta', d: 'Publica una vacante en cinco pasos cortos. El match con el talento se calcula solo.' },
        { sel: '.menu a[href$="/talentos"]', t: 'Directorio de talento', d: 'Explora perfiles y proyectos, y envía una invitación a quien te interese.' },
        { sel: '#bellBtn', t: 'La campana', d: 'Cada postulación nueva, mensaje o respuesta aparece aquí al instante.' },
        { sel: '.menu a[href$="/mensajes"]', t: 'Mensajes', d: 'Conversa con los candidatos aceptados sin salir de la plataforma.' }
    ] : [
        { sel: '.menu a[href$="/empleos"]', t: 'Busca empleos', d: 'Cada oferta muestra qué tanto encaja con tu perfil. Filtra, guarda las que te gusten y postúlate.' },
        { sel: '.menu a[href$="/solicitudes"]', t: 'Tus solicitudes', d: 'Sigue cada postulación paso a paso y responde las invitaciones de las empresas.' },
        { sel: '.menu a[href$="/proyectos"]', t: 'Tus proyectos', d: 'Sube tu portafolio. Las empresas lo ven antes de escribirte.' },
        { sel: '.menu a[href$="/talentos"]', t: 'Directorio', d: 'Descubre a otras personas de la comunidad y sus proyectos.' },
        { sel: '#bellBtn', t: 'La campana', d: 'Aquí te avisamos cuando te aceptan, te invitan o te escriben.' },
        { sel: '.menu a[href$="/perfil"]', t: 'Tu perfil', d: 'Mientras más completo, mejor match y más visibilidad. Empieza por aquí.' }
    ];

    var paso = 0, capa, tarjeta, previo, activa = false, resaltado = null;

    function visible(el) {
        if (!el) return false;
        var r = el.getBoundingClientRect();
        if (r.width === 0 || r.height === 0) return false;
        var s = $('#side');
        return !(s && s.hasAttribute('inert') && s.contains(el));
    }

    function marcarVisto() {
        try { localStorage.setItem(CLAVE, '1'); } catch (e) { }
    }

    function cerrar() {
        if (!activa) return;
        activa = false;
        marcarVisto();
        if (resaltado) resaltado.classList.remove('tour-on');
        if (capa) capa.remove();
        if (tarjeta) tarjeta.remove();
        document.removeEventListener('keydown', teclas, true);
        window.removeEventListener('resize', pintar);
        if (previo && previo.focus) previo.focus();
    }

    function teclas(e) {
        if (e.key === 'Escape') { e.preventDefault(); cerrar(); }
        else if (e.key === 'ArrowRight') { e.preventDefault(); ir(1); }
        else if (e.key === 'ArrowLeft') { e.preventDefault(); ir(-1); }
        else if (e.key === 'Tab' && tarjeta) {
            var bs = tarjeta.querySelectorAll('button');
            if (!bs.length) return;
            var primero = bs[0], ultimo = bs[bs.length - 1];
            if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus(); }
            else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus(); }
            else if (!tarjeta.contains(document.activeElement)) { e.preventDefault(); primero.focus(); }
        }
    }

    function ir(d) {
        var n = paso + d;
        if (n < 0) return;
        if (n >= PASOS.length) { cerrar(); return; }
        paso = n;
        pintar();
    }

    function pintar() {
        if (!activa) return;
        var p = PASOS[paso];
        var el = $(p.sel);
        if (resaltado) resaltado.classList.remove('tour-on');
        resaltado = null;
        var ancla = visible(el) ? el : null;
        capa.classList.toggle('dim', !ancla);
        if (ancla) {
            ancla.classList.add('tour-on');
            resaltado = ancla;
        }
        tarjeta.innerHTML = '';
        var n = document.createElement('small');
        n.textContent = 'PASO ' + (paso + 1) + ' DE ' + PASOS.length;
        var h = document.createElement('b');
        h.id = 'tourT';
        h.textContent = p.t;
        var txt = document.createElement('p');
        txt.textContent = p.d;
        var fila = document.createElement('div');
        fila.className = 'tour-acts';
        var saltar = document.createElement('button');
        saltar.type = 'button';
        saltar.className = 'tour-skip';
        saltar.textContent = paso === PASOS.length - 1 ? 'Cerrar' : 'Saltar';
        saltar.addEventListener('click', cerrar);
        fila.appendChild(saltar);
        if (paso > 0) {
            var atras = document.createElement('button');
            atras.type = 'button';
            atras.className = 'tour-back';
            atras.textContent = 'Anterior';
            atras.addEventListener('click', function () { ir(-1); });
            fila.appendChild(atras);
        }
        var sig = document.createElement('button');
        sig.type = 'button';
        sig.className = 'tour-next';
        sig.textContent = paso === PASOS.length - 1 ? 'Listo' : 'Siguiente';
        sig.addEventListener('click', function () { ir(1); });
        fila.appendChild(sig);
        tarjeta.appendChild(n);
        tarjeta.appendChild(h);
        tarjeta.appendChild(txt);
        tarjeta.appendChild(fila);

        tarjeta.style.left = tarjeta.style.top = tarjeta.style.transform = '';
        if (ancla) {
            var r = ancla.getBoundingClientRect();
            var ancho = Math.min(320, window.innerWidth - 24);
            tarjeta.style.width = ancho + 'px';
            var izq = r.right + 16;
            if (izq + ancho > window.innerWidth - 12) izq = Math.max(12, window.innerWidth - ancho - 12);
            var arr = Math.max(12, Math.min(r.top - 8, window.innerHeight - 220));
            if (r.right + 16 + ancho > window.innerWidth - 12) arr = r.bottom + 14;
            tarjeta.style.left = izq + 'px';
            tarjeta.style.top = arr + 'px';
            tarjeta.classList.remove('centro');
        } else {
            tarjeta.classList.add('centro');
        }
        sig.focus();
    }

    function iniciar() {
        if (activa) return;
        activa = true;
        paso = 0;
        previo = document.activeElement;
        capa = document.createElement('div');
        capa.className = 'tour-capa';
        capa.addEventListener('click', cerrar);
        tarjeta = document.createElement('div');
        tarjeta.className = 'tour-card';
        tarjeta.setAttribute('role', 'dialog');
        tarjeta.setAttribute('aria-modal', 'true');
        tarjeta.setAttribute('aria-labelledby', 'tourT');
        document.body.appendChild(capa);
        document.body.appendChild(tarjeta);
        document.addEventListener('keydown', teclas, true);
        window.addEventListener('resize', pintar);
        pintar();
    }

    document.addEventListener('click', function (e) {
        var b = e.target.closest ? e.target.closest('[data-tour-start]') : null;
        if (!b) return;
        e.preventDefault();
        iniciar();
    });

    function yaVisto() {
        try { return localStorage.getItem(CLAVE) === '1'; } catch (e) { return false; }
    }

    var side = $('#side');
    var auto = side && side.getAttribute('data-tour-auto') === '1';
    var enDashboard = /^\/dashboard\/?$/.test(location.pathname);
    // entrada a la demo: siempre; personas con cuenta propia: una sola vez, y sin pisar el diálogo de bienvenida del perfil
    if (side && ((auto) || (enDashboard && !yaVisto() && !$('#ob') && side.getAttribute('data-tour-auto') !== '0'))) {
        window.setTimeout(iniciar, 700);
    }
})();
