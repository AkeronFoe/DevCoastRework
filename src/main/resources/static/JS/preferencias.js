// esto de acá pone el tema y las preferencias ANTES de que se pinte la página, para que no parpadee
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
        try { localStorage.setItem(key, value); } catch (e) { }
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
        get: function (name) { return valores()[name]; },
        set: function (name, value) {
            if (!KEYS[name]) return;
            write(KEYS[name], String(value));
            var v = apply();
            document.dispatchEvent(new CustomEvent('dc:prefs-changed', { detail: { name: name, value: v[name], all: v } }));
        },
        apply: apply
    };

    window.addEventListener('storage', function (e) {
        for (var k in KEYS) { if (KEYS[k] === e.key) { apply(); return; } }
    });
})();
