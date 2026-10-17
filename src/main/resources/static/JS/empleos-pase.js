/* DevCoast · Empleos (docs/diseno/DEVCOAST-EMPLEOS-MOTION.md).
   Filtros, búsqueda y orden los resuelve el servidor (parámetros GET); aquí queda solo lo de interfaz:
   enviar el orden al cambiar el selector y el "Postularme" del ticket (se rasga el talón, sello y aviso). */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var T = function (t) { return typeof window.translateText === 'function' ? window.translateText(t) : t; };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    /* orden: el selector envía el formulario (sin JS queda el botón "aplicar") */
    var sort = $('#sort');
    if (sort) sort.addEventListener('change', function () { sort.form.submit(); });

    function marcarSolicitada(art, btn) {
        art.classList.remove('in');
        art.classList.add('sent');
        btn.disabled = true;
        btn.textContent = T('Solicitada');
    }
    function restaurar(art, btn) {
        art.classList.remove('sent');
        btn.disabled = false;
        btn.textContent = T('Postularme');
    }

    /* postular: POST real (JSON); el talón se rasga y sale el aviso con "Deshacer" (cancela la solicitud) */
    document.addEventListener('submit', function (e) {
        var f = e.target.closest ? e.target.closest('form.postular') : null;
        if (!f) return;
        e.preventDefault();
        var btn = $('.apply', f), art = f.closest('.jt');
        if (!btn || btn.disabled || !art) return;
        var cargo = f.getAttribute('data-cargo') || '', empresa = f.getAttribute('data-empresa') || '';
        btn.disabled = true;

        fetch(f.getAttribute('data-postular'), { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function (r) { return r.json().catch(function () { return {}; }).then(function (d) { return { ok: r.ok, d: d }; }); })
            .then(function (res) {
                var d = res.d || {};
                if (res.ok && d.ok) {
                    marcarSolicitada(art, btn);
                    if (d.descartado) { toast(d.mensaje); return; }
                    toast(T('Solicitud enviada a') + ' ' + (empresa || T('la empresa')) + ' (' + cargo + ').', {
                        undo: function () {
                            // la solicitud se cancela con el mismo endpoint que usa "Mis solicitudes"; responde con una redirección
                            fetch('/solicitudes/' + encodeURIComponent(d.solicitudId) + '/delete', { method: 'POST', redirect: 'manual' })
                                .then(function (r) {
                                    if (r.ok || r.type === 'opaqueredirect') { restaurar(art, btn); toast(T('Solicitud cancelada.')); }
                                    else toast(T('No se pudo cancelar. Hazlo desde Solicitudes.'));
                                })
                                .catch(function () { toast(T('No se pudo cancelar. Hazlo desde Solicitudes.')); });
                        }
                    });
                } else if (d.yaAplicaste) {
                    marcarSolicitada(art, btn);
                    toast(d.mensaje);
                } else {
                    btn.disabled = false;
                    toast(d.mensaje || T('No se pudo enviar la solicitud.'));
                }
            })
            .catch(function () {
                btn.disabled = false;
                toast(T('No se pudo enviar. Revisa tu conexión.'));
            });
    });
})();
