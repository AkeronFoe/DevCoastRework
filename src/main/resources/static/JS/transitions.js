(function () {
    'use strict';

    function isNavLink(a, e) {
        if (!a) return false;
        if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return false;
        if (a.target === '_blank') return false;
        var href = a.getAttribute('href');
        if (!href || href === '#' || href.startsWith('javascript') || href.startsWith('mailto')) return false;
        if (href.startsWith('http') && !href.startsWith(location.origin)) return false;
        if (a.dataset.noSkeleton !== undefined) return false;
        return true;
    }

    var navBar;

    function initNavProgress() {
        navBar = document.createElement('div');
        navBar.id = 'nav-progress';
        navBar.style.cssText = [
            'position:fixed;top:0;left:0;height:3px;width:0;',
            'background:var(--accent);z-index:99999;opacity:0;',
            'border-radius:0 2px 2px 0;pointer-events:none;transition:none;',
            'box-shadow:0 0 8px var(--accent);'
        ].join('');
        document.body.appendChild(navBar);
    }

    function startBar() {
        if (!navBar) return;
        navBar.style.transition = 'none';
        navBar.style.opacity   = '1';
        navBar.style.width     = '0%';
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                navBar.style.transition = 'width 0.4s cubic-bezier(0.4,0,0.2,1)';
                navBar.style.width      = '70%';
                setTimeout(function () {
                    navBar.style.transition = 'width 8s linear';
                    navBar.style.width      = '92%';
                }, 400);
            });
        });
    }

    function completeBar() {
        if (!navBar) return;
        navBar.style.transition = 'width 0.15s ease';
        navBar.style.width      = '100%';
        setTimeout(function () {
            navBar.style.opacity = '0';
            setTimeout(function () { navBar.style.width = '0%'; }, 350);
        }, 150);
    }

    function skel(w, h, extra) {
        return '<div class="dc-sk" style="width:' + w + ';height:' + (h||12) + 'px;'
            + 'border-radius:6px;' + (extra||'') + '"></div>';
    }

    function buildSkelHTML() {
        var s = '';

        s += '<div style="display:flex;align-items:center;gap:12px;'
           + 'height:60px;border-bottom:1px solid var(--border);'
           + 'margin:-24px -28px 20px;padding:0 24px 0 28px;">';
        s += skel('160px', 18);
        s += '<div style="flex:1"></div>';
        s += skel('90px', 32, 'border-radius:8px;');
        s += skel('32px', 32, 'border-radius:50%;');
        s += '</div>';

        s += '<div class="dc-sk-card" style="display:flex;gap:16px;align-items:flex-start;">';
        s += skel('48px', 48, 'border-radius:10px;flex-shrink:0;');
        s += '<div style="flex:1;">';
        s += skel('55%', 18, 'margin-bottom:10px;');
        s += skel('80%', 11);
        s += skel('40%', 10, 'margin-top:4px;');
        s += '</div>';
        s += '</div>';

        s += '<div class="dc-sk-card">';
        s += skel('38%', 15, 'margin-bottom:18px;');
        s += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:16px 24px;">';
        for (var i = 0; i < 6; i++) {
            s += '<div>';
            s += skel('45%', 9, 'margin-bottom:7px;');
            s += skel('70%', 13);
            s += '</div>';
        }
        s += '</div>';
        s += '</div>';

        s += '<div class="dc-sk-card">';
        s += skel('42%', 15, 'margin-bottom:18px;');
        for (var j = 0; j < 3; j++) {
            s += '<div style="display:flex;align-items:center;gap:12px;padding:8px 0;'
               + (j < 2 ? 'border-bottom:1px solid var(--border);' : '') + '">';
            s += skel('36px', 36, 'border-radius:8px;flex-shrink:0;');
            s += '<div style="flex:1;">';
            s += skel('60%', 12, 'margin-bottom:6px;');
            s += skel('40%', 10);
            s += '</div>';
            s += skel('56px', 24, 'border-radius:20px;');
            s += '</div>';
        }
        s += '</div>';

        return s;
    }

    var skelTimeout;

    function showSkeleton() {
        var main = document.querySelector('.main');
        if (!main || document.getElementById('dc-skeleton')) return;

        var bg = getComputedStyle(main).backgroundColor;
        if (!bg || bg === 'rgba(0, 0, 0, 0)' || bg === 'transparent') {
            bg = getComputedStyle(document.body).backgroundColor || '#0f1117';
        }

        if (getComputedStyle(main).position === 'static') {
            main.style.position = 'relative';
        }

        var overlay = document.createElement('div');
        overlay.id = 'dc-skeleton';
        overlay.style.cssText = [
            'position:absolute;inset:0;z-index:50;',
            'background:' + bg + ';',
            'padding:24px 28px;',
            'overflow:hidden;',
            'pointer-events:none;',
            'animation:dcSkelIn .12s ease both;'
        ].join('');
        overlay.innerHTML = buildSkelHTML();
        main.appendChild(overlay);

        clearTimeout(skelTimeout);
        skelTimeout = setTimeout(function () {
            var el = document.getElementById('dc-skeleton');
            if (el) el.parentNode.removeChild(el);
        }, 8000);
    }

    function initSkeletonLoader() {
        document.addEventListener('click', function (e) {
            var a = e.target.closest('a[href]');
            if (!isNavLink(a, e)) return;
            showSkeleton();
        }, true);
    }

    function initPageFade() {
        if (document.getElementById('dc-transitions-style')) return;

        var style = document.createElement('style');
        style.id  = 'dc-transitions-style';
        style.textContent = [
            '@keyframes dcFadeUp{',
            '  from{opacity:0;transform:translateY(10px)}',
            '  to  {opacity:1;transform:translateY(0)}',
            '}',
            '.main>*:not(script):not(style):not(#dc-skeleton){',
            '  animation:dcFadeUp .25s cubic-bezier(.4,0,.2,1) both;',
            '}',
            '.main>*:nth-child(2){animation-delay:.04s}',
            '.main>*:nth-child(3){animation-delay:.08s}',
            '.main>*:nth-child(4){animation-delay:.12s}',
            '.main>*:nth-child(5){animation-delay:.16s}',

            '@keyframes dcSkelIn{',
            '  from{opacity:0} to{opacity:1}',
            '}',

            '@keyframes dcSkelPulse{',
            '  0%,100%{opacity:1} 50%{opacity:.35}',
            '}',
            '.dc-sk{',
            '  background:var(--border);',
            '  display:block;',
            '  animation:dcSkelPulse 1.5s ease-in-out infinite;',
            '}',
            '.dc-sk:nth-child(2){animation-delay:.15s}',
            '.dc-sk:nth-child(3){animation-delay:.3s}',
            '.dc-sk:nth-child(4){animation-delay:.45s}',

            '.dc-sk-card{',
            '  background:var(--surface-2,rgba(255,255,255,.03));',
            '  border:1px solid var(--border);',
            '  border-radius:12px;',
            '  padding:20px;',
            '  margin-bottom:14px;',
            '}',
        ].join('');
        document.head.appendChild(style);
    }

    function initBackToTop() {
        var btn = document.getElementById('back-to-top');
        if (!btn || btn.dataset.bttInit) return;
        btn.dataset.bttInit = '1';

        window.addEventListener('scroll', function () {
            var v = window.scrollY > 280;
            btn.style.opacity       = v ? '1' : '0';
            btn.style.pointerEvents = v ? 'auto' : 'none';
            btn.style.transform     = v ? 'translateY(0)' : 'translateY(10px)';
        }, { passive: true });

        btn.addEventListener('click', function () {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    function ready(fn) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', fn);
        } else {
            fn();
        }
    }

    window.addEventListener('pageshow', completeBar);

    ready(function () {
        initNavProgress();
        initPageFade();
        initSkeletonLoader();
        initBackToTop();
        completeBar();
    });

})();