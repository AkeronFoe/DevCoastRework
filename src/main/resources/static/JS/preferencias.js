/* DevCoast · preferencias de interfaz (tema, tamaño de texto, densidad, contraste, movimiento).
   Se carga en el <head>, sin defer, para fijar los atributos de <html> antes del primer pintado y que la
   página no parpadee. Las claves de localStorage son las mismas que usaba themes.js (theme, ui-density,
   ui-text-size, ui-contrast, dc-reduce-motion), así las preferencias ya guardadas siguen valiendo. */
(function () {
    'use strict';

    var KEYS = {
        theme: 'theme',
        density: 'ui-density',
        text: 'ui-text-size',
        contrast: 'ui-contrast',
        motion: 'dc-reduce-motion'
    };
    var THEMES = ['light', 'dark', 'midnight'];

    function read(key, fallback) {
        try { return localStorage.getItem(key) || fallback; } catch (e) { return fallback; }
    }
    function write(key, value) {
        try { localStorage.setItem(key, value); } catch (e) { /* modo privado: la preferencia vale solo en esta página */ }
    }

    function valores() {
        var theme = read(KEYS.theme, 'dark');
        if (THEMES.indexOf(theme) < 0) theme = 'dark';
        var text = read(KEYS.text, 'normal');
        var density = read(KEYS.density, 'normal');
        return {
            theme: theme,
            text: (text === 'small' || text === 'large') ? text : 'normal',
            density: (density === 'compact' || density === 'spacious') ? density : 'normal',
            contrast: read(KEYS.contrast, 'normal') === 'high' ? 'high' : 'normal',
            motion: read(KEYS.motion, 'false') === 'true'
        };
    }

    function apply() {
        var v = valores();
        var h = document.documentElement;
        h.setAttribute('data-theme', v.theme);
        h.setAttribute('data-text-size', v.text);
        h.setAttribute('data-density', v.density);
        h.setAttribute('data-contrast', v.contrast);
        h.setAttribute('data-reduce-motion', v.motion ? 'true' : 'false');
        var meta = document.querySelector('meta[name="color-scheme"]');
        if (meta) meta.setAttribute('content', v.theme === 'light' ? 'light' : 'dark');
        return v;
    }

    apply();

    window.DevCoastPrefs = {
        /* name: theme | density | text | contrast | motion */
        get: function (name) { return valores()[name]; },
        set: function (name, value) {
            if (!KEYS[name]) return;
            write(KEYS[name], String(value));
            var v = apply();
            document.dispatchEvent(new CustomEvent('dc:prefs-changed', { detail: { name: name, value: v[name], all: v } }));
        },
        apply: apply
    };

    // si el usuario cambia una preferencia en otra pestaña, esta se pone al día
    window.addEventListener('storage', function (e) {
        for (var k in KEYS) { if (KEYS[k] === e.key) { apply(); return; } }
    });
})();
