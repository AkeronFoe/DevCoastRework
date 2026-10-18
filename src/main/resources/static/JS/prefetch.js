(function () {
    const prefetched = new Set();

    function prefetch(url) {
        if (prefetched.has(url)) return;
        prefetched.add(url);

        const link = document.createElement('link');
        link.rel = 'prefetch';
        link.href = url;
        link.as = 'document';
        document.head.appendChild(link);
    }

    function esInterno(href) {
        if (!href) return false;
        if (href.startsWith('http') && !href.startsWith(window.location.origin)) return false;
        if (href.startsWith('#') || href.startsWith('javascript') || href.startsWith('mailto')) return false;
        return true;
    }

    function esSeguro(href) {
        try {
            const url = new URL(href, window.location.origin);
            const path = url.pathname;
            const bloqueados = ['/logout', '/eliminar', '/delete', '/cerrar', '/borrar',
                                '/notificaciones', '/chat'];
            return !bloqueados.some(b => path.includes(b));
        } catch (e) {
            return false;
        }
    }

    document.addEventListener('mouseover', function (e) {
        const anchor = e.target.closest('a');
        if (!anchor) return;
        const href = anchor.href;
        if (esInterno(href) && esSeguro(href) && href !== window.location.href) {
            prefetch(href);
        }
    });

    document.addEventListener('focusin', function (e) {
        const anchor = e.target.closest('a');
        if (!anchor) return;
        const href = anchor.href;
        if (esInterno(href) && esSeguro(href) && href !== window.location.href) {
            prefetch(href);
        }
    });
})();