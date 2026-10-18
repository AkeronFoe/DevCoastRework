(function () {
    function ready(fn) {
        if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', fn); } else { fn(); }
    }

    var TOAST_ICONS = {
        success: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
        error: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
        warning: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
        info: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
    };
    var TOAST_TITLES = { success: '¡Listo!', error: 'Error', warning: 'Atención', info: 'Información' };
    var TOAST_TAGS = { success: 'OK', error: 'ERR', warning: 'WARN', info: 'i' };

    function getContainer() {
        var c = document.getElementById('toast-system');
        var isNew = !c;
        if (isNew) {
            c = document.createElement('div');
            c.id = 'toast-system';
            document.body.appendChild(c);
        }
        c.setAttribute('aria-live', 'polite');
        c.setAttribute('aria-atomic', 'true');
        c.style.cssText = 'position:fixed;bottom:24px;right:24px;display:flex;flex-direction:column-reverse;gap:10px;z-index:5000;pointer-events:none;';
        if (!document.getElementById('toast-sys-styles')) {
                var s = document.createElement('style');
                s.id = 'toast-sys-styles';
                s.textContent = [
                    '.ts-item{pointer-events:all;display:flex;align-items:flex-start;gap:12px;min-width:300px;max-width:400px;',
                    'background:var(--surface,#fff);border:1px solid var(--border,#e2e8f0);border-radius:16px;',
                    'padding:14px 16px;box-shadow:0 20px 48px rgba(15,23,42,.14),0 4px 12px rgba(15,23,42,.06);',
                    'opacity:0;transform:translateX(24px);transition:opacity .25s ease,transform .25s ease;position:relative;}',
                    '.ts-item.show{opacity:1;transform:translateX(0);}',
                    '.ts-item.hide{opacity:0;transform:translateX(24px);}',
                    '.ts-item::before{content:"";position:absolute;left:0;top:12%;bottom:12%;width:4px;border-radius:0 4px 4px 0;}',
                    '.ts-item.success::before{background:#16a34a;}',
                    '.ts-item.error::before{background:#dc2626;}',
                    '.ts-item.warning::before{background:#d97706;}',
                    '.ts-item.info::before{background:var(--accent,#1d7fd0);}',
                    '.ts-iw{width:36px;height:36px;border-radius:10px;display:grid;place-items:center;flex-shrink:0;}',
                    '.ts-iw svg{width:18px;height:18px;}',
                    '.ts-iw.success{background:#dcfce7;color:#16a34a;}',
                    '.ts-iw.error{background:#fef2f2;color:#dc2626;}',
                    '.ts-iw.warning{background:#fef9c3;color:#b45309;}',
                    '.ts-iw.info{background:var(--accent-soft,#e0f0fb);color:var(--accent,#1d7fd0);}',
                    '.ts-ct{flex:1;min-width:0;}',
                    '.ts-ttl{font-size:.88rem;font-weight:700;color:var(--text,#0f172a);}',
                    '.ts-tag{font-family:var(--font-mono,monospace);margin-right:5px;}',
                    '.ts-item.success .ts-tag{color:#16a34a;}',
                    '.ts-item.error .ts-tag{color:#dc2626;}',
                    '.ts-item.warning .ts-tag{color:#d97706;}',
                    '.ts-item.info .ts-tag{color:var(--accent,#1d7fd0);}',
                    '.ts-msg{font-size:.78rem;color:var(--muted,#64748b);margin-top:2px;line-height:1.45;}',
                    '.ts-cls{background:none;border:none;cursor:pointer;color:var(--muted,#64748b);padding:2px;font-size:1.1rem;flex-shrink:0;border-radius:6px;line-height:1;transition:color .15s;}',
                    '.ts-cls:hover{color:var(--text,#0f172a);}',
                    '.ts-prog{position:absolute;bottom:0;left:4px;right:0;height:2px;border-radius:0 0 16px 0;background:var(--border,#e2e8f0);overflow:hidden;}',
                    '.ts-fill{height:100%;transition:width linear;}',
                    '.ts-item.success .ts-fill{background:#16a34a;}',
                    '.ts-item.error .ts-fill{background:#dc2626;}',
                    '.ts-item.warning .ts-fill{background:#d97706;}',
                    '.ts-item.info .ts-fill{background:var(--accent,#1d7fd0);}'
                ].join('');
                document.head.appendChild(s);
        }
        return c;
    }

    function normalizeType(t) {
        if (!t) return 'info';
        var v = String(t).toLowerCase();
        if (v === 'ok' || v === 'success') return 'success';
        if (v === 'err' || v === 'error' || v === 'danger') return 'error';
        if (v === 'warn' || v === 'warning') return 'warning';
        return 'info';
    }

    function showToast(message, type, title) {
        if (!message) return;
        var t = normalizeType(type);
        var ttl = title || TOAST_TITLES[t] || 'Notificación';
        var container = getContainer();
        var duration = t === 'error' ? 6000 : 4500;

        var toast = document.createElement('div');
        toast.className = 'ts-item ' + t;
        toast.setAttribute('role', 'status');
        toast.innerHTML = [
            '<div class="ts-iw ' + t + '">' + (TOAST_ICONS[t] || TOAST_ICONS.info) + '</div>',
            '<div class="ts-ct">',
            '<div class="ts-ttl"><span class="ts-tag">[' + (TOAST_TAGS[t] || 'i') + ']</span>' + ttl + '</div>',
            '<div class="ts-msg">' + message + '</div>',
            '</div>',
            '<button class="ts-cls" aria-label="Cerrar">×</button>',
            '<div class="ts-prog"><div class="ts-fill" style="width:100%;transition-duration:' + duration + 'ms;"></div></div>'
        ].join('');

        container.appendChild(toast);

        var dismissed = false;
        function dismiss() {
            if (dismissed) return; dismissed = true;
            toast.classList.add('hide'); toast.classList.remove('show');
            setTimeout(function () { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 300);
        }

        toast.querySelector('.ts-cls').addEventListener('click', dismiss);

        requestAnimationFrame(function () {
            toast.classList.add('show');
            setTimeout(function () {
                var fill = toast.querySelector('.ts-fill');
                if (fill) fill.style.width = '0%';
            }, 60);
        });

        setTimeout(dismiss, duration);
    }

    function ensureConfirmModal() {
        var overlay = document.getElementById('confirm-overlay');
        if (overlay) return overlay;
        overlay = document.createElement('div');
        overlay.id = 'confirm-overlay';
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = [
            '<div class="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="confirm-title">',
            '<h3 class="confirm-title" id="confirm-title">Confirmar acción</h3>',
            '<p class="confirm-message" id="confirm-message"></p>',
            '<div class="confirm-actions">',
            '<button type="button" class="btn btn-ghost" id="confirm-cancel">Cancelar</button>',
            '<button type="button" class="btn btn-primary" id="confirm-ok">Confirmar</button>',
            '</div></div>'
        ].join('');
        document.body.appendChild(overlay);
        return overlay;
    }

    function initConfirmHandlers() {
        var overlay = ensureConfirmModal();
        var titleEl = overlay.querySelector('#confirm-title');
        var messageEl = overlay.querySelector('#confirm-message');
        var cancelBtn = overlay.querySelector('#confirm-cancel');
        var okBtn = overlay.querySelector('#confirm-ok');
        var cb = null;

        function close() { overlay.classList.remove('is-open'); overlay.setAttribute('aria-hidden', 'true'); cb = null; }
        function open(opts) {
            titleEl.textContent = opts.title || 'Confirmar acción';
            messageEl.textContent = opts.message || '¿Deseas continuar?';
            cancelBtn.textContent = opts.cancelLabel || 'Cancelar';
            okBtn.textContent = opts.okLabel || 'Confirmar';
            okBtn.className = opts.variant === 'danger' ? 'btn btn-danger' : (opts.variant === 'warning' ? 'btn btn-outline' : 'btn btn-primary');
            overlay.classList.add('is-open'); overlay.setAttribute('aria-hidden', 'false');
            cb = opts.onConfirm || null;
        }

        cancelBtn.addEventListener('click', close);
        overlay.addEventListener('click', function (e) { if (e.target === overlay) close(); });
        document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && overlay.classList.contains('is-open')) close(); });
        okBtn.addEventListener('click', function () { var f = cb; close(); if (f) f(); });

        document.addEventListener('submit', function (e) {
            var form = e.target;
            if (!form || !form.getAttribute) return;
            var msg = form.getAttribute('data-confirm-message');
            if (!msg) return;
            e.preventDefault();
            open({
                title: form.getAttribute('data-confirm-title'),
                message: msg,
                okLabel: form.getAttribute('data-confirm-ok'),
                cancelLabel: form.getAttribute('data-confirm-cancel'),
                variant: form.getAttribute('data-confirm-variant'),
                onConfirm: function () { form.submit(); }
            });
        }, true);

        return { open: open };
    }

    if (!window.UIFeedback) window.UIFeedback = {};
    window.UIFeedback.toast = showToast;

    ready(function () {
        var flash = document.getElementById('flash-data');
        if (flash) {
            var msg = flash.getAttribute('data-message');
            var type = flash.getAttribute('data-type') || 'info';
            if (msg) showToast(msg, type);
        }
        var confirmApi = initConfirmHandlers();
        if (confirmApi && window.UIFeedback) window.UIFeedback.confirm = confirmApi.open;
    });
})();