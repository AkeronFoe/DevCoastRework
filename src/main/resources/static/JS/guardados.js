/* DevCoast · Guardados.
   Filtros y orden los resuelve el servidor (parámetros GET); aquí queda solo lo de interfaz:
   enviar el orden al cambiar el selector y quitar una oferta de los marcadores (con "Deshacer").
   El "Postularme" de cada pase lo maneja empleos-pase.js. */
(function () {
    'use strict';

    var $ = function (s, r) { return (r || document).querySelector(s); };
    var toast = function (txt, opts) { if (window.DevCoast && window.DevCoast.toast) window.DevCoast.toast(txt, opts); };

    var lista = $('#guardadosLista');
    var sub = $('#gdSub');
    var removidas = 0;

    /* el contador del encabezado se recalcula con lo que queda visible en la lista */
    function actualizarResumen() {
        if (!lista || !sub) return;
        var vivas = Array.prototype.filter.call(lista.querySelectorAll('.jt'), function (a) { return !a.hidden; });
        var alta = vivas.filter(function (a) { return a.querySelector('.jt-stub.hi') && a.querySelector('.mt-n'); }).length;
        var sol = vivas.filter(function (a) { return a.classList.contains('sent'); }).length;
        var total = Math.max(0, parseInt(sub.getAttribute('data-total') || '0', 10) - removidas);
        var conMatch = sub.getAttribute('data-match') === 'true';
        var txt = total === 1 ? '1 oferta guardada' : total + ' ofertas guardadas';
        if (conMatch) txt += ' · ' + alta + ' con match alto';
        if (sol > 0) txt += ' · ' + sol + ' ya solicitadas';
        sub.textContent = txt;
        if (vivas.length === 0) {
            lista.hidden = true;
            var vacio = $('#guardadosVacio');
            if (vacio) vacio.hidden = false;
        }
    }

    function llamarFavorito(id) {
        return fetch('/empleos/favorito/' + encodeURIComponent(id), { method: 'POST' })
            .then(function (r) { if (!r.ok) throw new Error('http ' + r.status); return r.json(); });
    }

    /* quitar de guardados: POST al mismo endpoint que el marcador de Empleos (alterna el estado) */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest ? e.target.closest('.unsave[data-empleo-id]') : null;
        if (!btn || btn.disabled) return;
        var art = btn.closest('.jt');
        var id = btn.getAttribute('data-empleo-id');
        var titulo = ($('.jt-t', art) || {}).textContent || 'La oferta';
        btn.disabled = true;

        llamarFavorito(id).then(function (d) {
            if (!(d && d.ok && !d.favorito)) throw new Error('respuesta inesperada');
            art.classList.add('quitando');
            setTimeout(function () { art.hidden = true; removidas++; actualizarResumen(); }, 230);
            toast(titulo.trim() + ' ya no está en guardados.', {
                undo: function () {
                    llamarFavorito(id).then(function (r) {
                        if (r && r.ok && r.favorito) {
                            art.hidden = false;
                            art.classList.remove('quitando');
                            btn.disabled = false;
                            removidas = Math.max(0, removidas - 1);
                            if (lista) lista.hidden = false;
                            var vacio = $('#guardadosVacio'); if (vacio) vacio.hidden = true;
                            actualizarResumen();
                        } else {
                            toast('No se pudo deshacer. Vuelve a guardarla desde Empleos.');
                        }
                    }).catch(function () { toast('No se pudo deshacer. Vuelve a guardarla desde Empleos.'); });
                }
            });
        }).catch(function () {
            btn.disabled = false;
            toast('No se pudo quitar la oferta. Intenta de nuevo.');
        });
    });
})();
