(function () {
    'use strict';

    function initTooltips() {
        var tip       = null;
        var currentEl = null;

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

            if (el.hasAttribute('data-dc-tip')) {
                el.setAttribute('title', el.getAttribute('data-dc-tip'));
                el.removeAttribute('data-dc-tip');
            }

            if (tip) {
                tip.style.opacity   = '0';
                tip.style.transform = 'translateY(4px)';
                setTimeout(function () {
                    if (!currentEl && tip) tip.style.display = 'none';
                }, 150);
            }
        }

        document.addEventListener('mouseover', function (e) {
            var el = e.target.closest('[title]');
            if (el) { showTip(el); return; }

        });

        document.addEventListener('mouseout', function (e) {
            var el = e.target.closest('[data-dc-tip]');
            if (!el) return;

            if (el.contains(e.relatedTarget)) return;

            hideTip(el);
        });
    }

    function initCounters() {
        var sel = '.empresa-stat-num, .postul-stat-num, .dc-counter';
        var els = document.querySelectorAll(sel);
        if (!els.length) return;

        function easeOut(t) {
            return 1 - Math.pow(1 - t, 3);
        }

        function animate(el, target, duration) {
            if (target === 0) return;
            var start = null;
            el.textContent = '0';

            function step(ts) {
                if (!start) start = ts;
                var progress = Math.min((ts - start) / duration, 1);
                el.textContent = Math.round(easeOut(progress) * target);
                if (progress < 1) {
                    requestAnimationFrame(step);
                } else {
                    el.textContent = target;
                }
            }
            requestAnimationFrame(step);
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) return;
                var el = entry.target;
                observer.unobserve(el);

                var raw    = parseInt(el.textContent.replace(/\D/g, ''), 10);
                var target = isNaN(raw) ? 0 : raw;
                animate(el, target, 650);
            });
        }, { threshold: 0.3 });

        els.forEach(function (el) { observer.observe(el); });
    }

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

            if (!el.hasAttribute('title') && !el.hasAttribute('data-dc-tip')) {
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
        setInterval(updateTimestamps, 60 * 1000);
    }

    function initFormLoading() {
        document.addEventListener('submit', function (e) {
            var form = e.target;
            if (!(form instanceof HTMLFormElement)) return;
            if (e.defaultPrevented) return;
            if (form.dataset.noLoading !== undefined) return;

            var btn = (e.submitter && e.submitter.type !== 'reset')
                ? e.submitter
                : form.querySelector('button[type="submit"], input[type="submit"]');
            if (!btn || btn.disabled) return;

            var original = btn.innerHTML;
            btn.dataset.dcOriginalHtml = original;
            btn.disabled = true;
            btn.classList.add('dc-btn-loading');
            btn.innerHTML = '<span class="dc-btn-dots" aria-hidden="true"><span></span><span></span><span></span></span>' +
                '<span>' + (btn.dataset.loadingText || 'Un momento') + '</span>';

            setTimeout(function () {
                if (btn.dataset.dcOriginalHtml) {
                    btn.disabled = false;
                    btn.classList.remove('dc-btn-loading');
                    btn.innerHTML = btn.dataset.dcOriginalHtml;
                    delete btn.dataset.dcOriginalHtml;
                }
            }, 15000);
        });
    }

    function initTopbarTab() {
        var titleBox = document.querySelector('.topbar .topbar-title');
        if (!titleBox || titleBox.querySelector('.topbar-tab')) return;

        var path = window.location.pathname.replace(/^\/+|\/+$/g, '');
        var segments = path.length ? path.split('/') : ['dashboard'];
        var last = segments[segments.length - 1];
        var head = segments.slice(0, -1);

        var wrap = document.createElement('div');
        wrap.className = 'topbar-tab';
        wrap.setAttribute('aria-hidden', 'true');

        var dots = document.createElement('span');
        dots.className = 'win-chrome';
        dots.innerHTML = '<span></span><span></span><span></span>';

        var pathEl = document.createElement('span');
        pathEl.className = 'topbar-tab-path';
        var prefix = '$ cd ~/' + (head.length ? head.join('/') + '/' : '');
        pathEl.innerHTML = prefix + '<b>' + last + '</b>';

        var cursor = document.createElement('span');
        cursor.className = 'topbar-tab-cursor';

        wrap.appendChild(dots);
        wrap.appendChild(pathEl);
        wrap.appendChild(cursor);
        titleBox.insertBefore(wrap, titleBox.firstChild);
    }

    function initEmptyStateChrome() {
        document.querySelectorAll('.dc-empty').forEach(function (el) {
            if (el.querySelector('.win-chrome')) return;
            var dots = document.createElement('span');
            dots.className = 'win-chrome';
            dots.setAttribute('aria-hidden', 'true');
            dots.innerHTML = '<span></span><span></span><span></span>';
            el.insertBefore(dots, el.firstChild);
        });
    }

    function initSearchShortcut() {
        var search = document.querySelector('.topbar .search');
        if (!search) return;
        var input = search.querySelector('input');
        if (!input) return;

        var isMac = /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent || '');
        var kbd = document.createElement('kbd');
        kbd.className = 'dc-kbd';
        kbd.setAttribute('aria-hidden', 'true');
        kbd.textContent = isMac ? '⌘K' : 'Ctrl K';
        search.appendChild(kbd);

        document.addEventListener('keydown', function (e) {
            var key = e.key ? e.key.toLowerCase() : '';
            if ((e.metaKey || e.ctrlKey) && key === 'k') {
                e.preventDefault();
                input.focus();
                if (input.select) input.select();
            }
        });
    }

    function injectStyles() {
        if (document.getElementById('dc-polish-style')) return;
        var s = document.createElement('style');
        s.id  = 'dc-polish-style';
        s.textContent = [
            '.dc-tip{',
            '  position:fixed;',
            '  z-index:99990;',
            '  display:none;',
            '  padding:5px 10px;',
            '  background:var(--term-chrome-bg,#0b0f14);',
            '  color:var(--term-chrome-text,#f1f5f9);',
            '  border:1px solid var(--term-chrome-border,transparent);',
            '  font-family:var(--font-mono,monospace);',
            '  font-size:11px;',
            '  font-weight:500;',
            '  border-radius:7px;',
            '  pointer-events:none;',
            '  white-space:nowrap;',
            '  box-shadow:0 4px 12px rgba(0,0,0,.25);',
            '  transition:opacity .15s ease,transform .15s ease;',
            '}',
            '[data-ts]{',
            '  cursor:default;',
            '  border-bottom:1px dashed rgba(100,116,139,.4);',
            '  transition:border-color .15s;',
            '}',
            '[data-ts]:hover{',
            '  border-bottom-color:var(--accent);',
            '}',
            '.dc-btn-loading{',
            '  display:inline-flex;align-items:center;gap:8px;',
            '  opacity:.85;cursor:not-allowed;',
            '}',
            '.dc-btn-spinner{',
            '  width:14px;height:14px;border-radius:50%;',
            '  border:2px solid rgba(255,255,255,.4);',
            '  border-top-color:#fff;',
            '  display:inline-block;',
            '  animation:dcSpin .6s linear infinite;',
            '}',
            '.btn-outline .dc-btn-spinner,.btn-ghost .dc-btn-spinner{',
            '  border-color:rgba(100,116,139,.35);',
            '  border-top-color:var(--accent);',
            '}',
            '@keyframes dcSpin{to{transform:rotate(360deg)}}',
            '.dc-btn-dots{display:inline-flex;gap:3px;align-items:center;}',
            '.dc-btn-dots span{',
            '  width:4px;height:4px;border-radius:50%;',
            '  background:currentColor;opacity:.35;',
            '  animation:dcDotPulse 1s ease-in-out infinite;',
            '}',
            '.dc-btn-dots span:nth-child(2){animation-delay:.15s}',
            '.dc-btn-dots span:nth-child(3){animation-delay:.3s}',
            '@keyframes dcDotPulse{0%,60%,100%{opacity:.35;transform:scale(1)}30%{opacity:1;transform:scale(1.3)}}',
        ].join('');
        document.head.appendChild(s);
    }

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
        initFormLoading();
        initTopbarTab();
        initEmptyStateChrome();
        initSearchShortcut();
    });

})();