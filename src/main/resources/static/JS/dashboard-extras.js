(function () {
    function initCharCounters() {
        document.querySelectorAll('[data-maxlength]').forEach(function (el) {
            var max = parseInt(el.getAttribute('data-maxlength'), 10);
            if (!max) return;
            var counter = document.createElement('span');
            counter.className = 'char-counter';
            counter.style.cssText = 'display:block;text-align:right;font-size:.72rem;margin-top:3px;';
            // en el workspace oscuro el color lo pone la clase CSS, no un
            // inline style (si no, gana siempre y queda gris claro sobre fondo oscuro)
            var enWorkspaceOscuro = !!el.closest('.wsjobs');
            function update() {
                var len = el.value ? el.value.length : 0;
                var cercaDelLimite = len > max * 0.9;
                counter.textContent = len + ' / ' + max;
                if (enWorkspaceOscuro) {
                    counter.classList.toggle('is-near-limit', cercaDelLimite);
                } else {
                    counter.style.color = cercaDelLimite ? 'var(--accent)' : 'var(--muted)';
                }
            }
            update();
            el.addEventListener('input', update);
            el.parentNode.insertBefore(counter, el.nextSibling);
        });
    }

    function initFavoritos(root) {
        (root || document).querySelectorAll('[data-favorito-id]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                var empleoId = btn.getAttribute('data-favorito-id');
                fetch('/empleos/favorito/' + empleoId, { method: 'POST', credentials: 'same-origin' })
                    .then(function (r) { return r.json(); })
                    .then(function (data) {
                        if (data.ok) {
                            var esFav = data.favorito;
                            btn.setAttribute('data-is-favorito', esFav ? 'true' : 'false');
                            btn.title = esFav ? 'Quitar de favoritos' : 'Guardar empleo';
                            var path = btn.querySelector('path, polygon, polyline');
                            if (path) path.style.fill = esFav ? 'currentColor' : 'none';
                            if (window.UIFeedback && window.UIFeedback.toast) {
                                window.UIFeedback.toast(
                                    esFav ? 'Empleo guardado en favoritos' : 'Empleo eliminado de favoritos',
                                    esFav ? 'success' : 'info'
                                );
                            }
                        }
                    })
                    .catch(function () {
                        if (window.UIFeedback && window.UIFeedback.toast) {
                            window.UIFeedback.toast('Error al guardar favorito', 'error');
                        }
                    });
            });
        });
    }

    function initSkeletons() {
        var skeletons = document.querySelectorAll('.skeleton-section');
        var real = document.querySelectorAll('.real-section');
        document.documentElement.classList.add('content-loaded');
    }

    function ready(fn) {
        if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', fn); }
        else { fn(); }
    }

    // la expongo para que empleos.js la vuelva a llamar después de traer
    // la grilla por fetch, si no los botones nuevos quedan sin listener
    window.initFavoritos = initFavoritos;

    ready(function () {
        initCharCounters();
        initFavoritos();
        initSkeletons();
    });
})();
