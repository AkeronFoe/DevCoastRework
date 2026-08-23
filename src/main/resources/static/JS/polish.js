/**
 * polish.js — DevCoast
 * Tres micro-features de UI: tooltips personalizados,
 * contadores animados y timestamps relativos.
 *
 * Coloca en: src/main/resources/static/JS/polish.js
 * Añade al <head> de cada template (después de styles.css):
 *   <script th:src="@{/JS/polish.js}" defer></script>
 */
(function () {
    'use strict';

    /* ══════════════════════════════════════════════════════════
       1.  TOOLTIPS PERSONALIZADOS
           Lee el atributo [title], lo reemplaza con un tooltip
           animado acorde al diseño de DevCoast.
    ══════════════════════════════════════════════════════════ */
    function initTooltips() {
        var tip       = null;
        var currentEl = null;  /* elemento que tiene el tooltip abierto */

        function getTip() {
            if (!tip) {
                tip = document.createElement('div');
                tip.className = 'dc-tip';
                document.body.appendChild(tip);
            }
            return tip;
        }

        function place(el) {
            var r   = el.getBoundingClientRect();
            var t   = getTip();
            var tw  = t.offsetWidth;
            var th2 = t.offsetHeight;
            var gap = 8;
            var left = r.left + r.width / 2 - tw / 2;
            var top  = r.top - th2 - gap;
            if (top < 6) top = r.bottom + gap;
            left = Math.max(6, Math.min(left, window.innerWidth - tw - 6));
            t.style.left = left + 'px';
            t.style.top  = top  + 'px';
        }

        function showTip(el) {
            /* Evita redibujar si ya está abierto para este mismo elemento */
            if (currentEl === el) return;
            currentEl = el;

            var text = el.getAttribute('title');
            if (!text) return;

            el.setAttribute('data-dc-tip', text);
            el.removeAttribute('title');

            var t = getTip();
            t.textContent     = text;
            t.style.opacity   = '0';
            t.style.transform = 'translateY(4px)';
            t.style.display   = 'block';

            requestAnimationFrame(function () {
                place(el);
                t.style.opacity   = '1';
                t.style.transform = 'translateY(0)';
            });
        }

        function hideTip(el) {
            if (!el) return;
            currentEl = null;

            /* Restaura title para que la próxima vez vuelva a funcionar */
            if (el.hasAttribute('data-dc-tip')) {
                el.setAttribute('title', el.getAttribute('data-dc-tip'));
                el.removeAttribute('data-dc-tip');
            }

            if (tip) {
                tip.style.opacity   = '0';
                tip.style.transform = 'translateY(4px)';
                setTimeout(function () {
                    /* Solo oculta si nadie abrió otro tooltip mientras */
                    if (!currentEl && tip) tip.style.display = 'none';
                }, 150);
            }
        }

        /* mouseover: activa al entrar en un elemento con [title] */
        document.addEventListener('mouseover', function (e) {
            var el = e.target.closest('[title]');
            if (el) { showTip(el); return; }

            /* Si el mouse se movió a un hijo del elemento actual,
               el hijo no tiene [title] pero el padre sí (como data-dc-tip).
               En ese caso no hacemos nada — mouseout lo manejará. */
        });

        /* mouseout: oculta SOLO cuando el mouse sale del elemento
           (no cuando se mueve entre hijos del mismo botón/ícono) */
        document.addEventListener('mouseout', function (e) {
            var el = e.target.closest('[data-dc-tip]');
            if (!el) return;

            /* relatedTarget = elemento al que va el mouse.
               Si sigue dentro del mismo botón (un hijo), no ocultar. */
            if (el.contains(e.relatedTarget)) return;

            hideTip(el);
        });
    }

    /* ══════════════════════════════════════════════════════════
       2.  CONTADORES ANIMADOS
           Anima de 0 al valor final los elementos con clase
           .empresa-stat-num, .postul-stat-num o .dc-counter.
           Usa IntersectionObserver para activarse al entrar
           en el viewport (no antes).
    ══════════════════════════════════════════════════════════ */
    function initCounters() {
        var sel = '.empresa-stat-num, .postul-stat-num, .dc-counter';
        var els = document.querySelectorAll(sel);
        if (!els.length) return;

        function easeOut(t) {
            return 1 - Math.pow(1 - t, 3);   /* cubic ease-out */
        }

        function animate(el, target, duration) {
            if (target === 0) return;          /* no animar ceros */
            var start = null;
            el.textContent = '0';

            function step(ts) {
                if (!start) start = ts;
                var progress = Math.min((ts - start) / duration, 1);
                el.textContent = Math.round(easeOut(progress) * target);
                if (progress < 1) {
                    requestAnimationFrame(step);
                } else {
                    el.textContent = target;   /* asegura valor exacto */
                }
            }
            requestAnimationFrame(step);
        }

        /* IntersectionObserver: anima solo cuando el elemento
           es visible (evita animar elementos fuera del fold) */
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                var el = entry.target;
                observer.unobserve(el);        /* solo una vez */

                var raw    = parseInt(el.textContent.replace(/\D/g, ''), 10);
                var target = isNaN(raw) ? 0 : raw;
                animate(el, target, 650);
            });
        }, { threshold: 0.3 });

        els.forEach(function (el) { observer.observe(el); });
    }

    /* ══════════════════════════════════════════════════════════
       3.  TIMESTAMPS RELATIVOS
           Lee [data-ts] (epoch en ms), reemplaza el texto
           por "hace X", y deja el texto original como tooltip.
           Se refresca cada minuto.
    ══════════════════════════════════════════════════════════ */
    function relativeTime(ts) {
        var diff = Date.now() - ts;
        var s    = Math.floor(diff / 1000);
        var m    = Math.floor(s / 60);
        var h    = Math.floor(m / 60);
        var d    = Math.floor(h / 24);

        if (s  <  60)  return 'hace un momento';
        if (m  <  60)  return 'hace ' + m + (m === 1 ? ' min'     : ' min');
        if (h  <  24)  return 'hace ' + h + (h === 1 ? 'h'        : 'h');
        if (d  ===  1) return 'ayer';
        if (d  <    7) return 'hace ' + d + ' días';
        if (d  <   30) {
            var w = Math.floor(d / 7);
            return 'hace ' + w + (w === 1 ? ' semana'   : ' semanas');
        }
        if (d  <  365) {
            var mo = Math.floor(d / 30);
            return 'hace ' + mo + (mo === 1 ? ' mes'    : ' meses');
        }
        var y = Math.floor(d / 365);
        return 'hace ' + y + (y === 1 ? ' año'       : ' años');
    }

    function updateTimestamps() {
        document.querySelectorAll('[data-ts]').forEach(function (el) {
            var ts = parseInt(el.getAttribute('data-ts'), 10);
            if (isNaN(ts)) return;

            var rel = relativeTime(ts);

            /* Guarda la fecha exacta como tooltip (la mostrará initTooltips) */
            if (!el.hasAttribute('title') && !el.hasAttribute('data-dc-tip')) {
                /* Genera fecha formateada a partir del timestamp */
                var d = new Date(ts);
                var fmt = d.toLocaleDateString('es-CO', {
                    day:   '2-digit', month: '2-digit', year: 'numeric'
                }) + ' · ' + d.toLocaleTimeString('es-CO', {
                    hour: '2-digit', minute: '2-digit'
                });
                el.setAttribute('title', fmt);
            }

            el.textContent = rel;
        });
    }

    function initRelativeTime() {
        updateTimestamps();
        /* Refresca cada minuto para que los "hace X min" sean precisos */
        setInterval(updateTimestamps, 60 * 1000);
    }

    /* ══════════════════════════════════════════════════════════
       Estilos
    ══════════════════════════════════════════════════════════ */
    function injectStyles() {
        if (document.getElementById('dc-polish-style')) return;
        var s = document.createElement('style');
        s.id  = 'dc-polish-style';
        s.textContent = [
            /* Tooltip */
            '.dc-tip{',
            '  position:fixed;',
            '  z-index:99990;',
            '  display:none;',
            '  padding:5px 10px;',
            '  background:#1a1a2e;',
            '  color:#f1f5f9;',
            '  font-size:12px;',
            '  font-weight:500;',
            '  border-radius:7px;',
            '  pointer-events:none;',
            '  white-space:nowrap;',
            '  box-shadow:0 4px 12px rgba(0,0,0,.25);',
            '  transition:opacity .15s ease,transform .15s ease;',
            '}',
            /* En tema claro, tooltip un poco más suave */
            '[data-theme="light"] .dc-tip{',
            '  background:#1e2a3b;',
            '}',
            /* Timestamps relativos */
            '[data-ts]{',
            '  cursor:default;',
            '  border-bottom:1px dashed rgba(100,116,139,.4);',
            '  transition:border-color .15s;',
            '}',
            '[data-ts]:hover{',
            '  border-bottom-color:var(--accent);',
            '}',
        ].join('');
        document.head.appendChild(s);
    }

    /* ══════════════════════════════════════════════════════════
       Init
    ══════════════════════════════════════════════════════════ */
    function ready(fn) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', fn);
        } else {
            fn();
        }
    }

    ready(function () {
        injectStyles();
        initTooltips();
        initCounters();
        initRelativeTime();
    });

})();