// «Rellenar con un ejemplo» (solo en modo feria): llena los pasos de Publicar oferta con datos de muestra y deja el formulario en el resumen,
// listo para pulsar Publicar. Usa el formulario real: nada se envía hasta que alguien pulsa Publicar.
(function () {
    'use strict';
    var btn = document.getElementById('btn-ejemplo');
    if (!btn) return;

    var EJEMPLOS = [
        { titulo: 'Backend Developer (Kotlin)', categoria: 'Programacion · Backend', seniority: 'Mid (2 – 4 años)', modalidad: 'Remoto', min: '8000000', max: '11000000',
            descripcion: 'Construirás servicios de alto tráfico con Kotlin y Spring Boot, con revisión de código en pareja y despliegues continuos. Trabajarás de cerca con producto.',
            stack: 'Kotlin, Spring Boot, PostgreSQL, Docker', soft: 'Comunicación asertiva, Trabajo en equipo' },
        { titulo: 'Frontend Developer (React)', categoria: 'Programacion · Frontend', seniority: 'Junior (0 – 2 años)', modalidad: 'Hibrido', min: '3500000', max: '5000000',
            descripcion: 'Sumarás al equipo de producto para construir pantallas rápidas y accesibles. Tendrás una persona mentora asignada durante los primeros meses.',
            stack: 'React, TypeScript, Tailwind CSS, Jest', soft: 'Curiosidad, Adaptabilidad' },
        { titulo: 'Diseñador UX/UI', categoria: 'Diseño · UI/UX', seniority: 'Mid (2 – 4 años)', modalidad: 'Remoto', min: '5500000', max: '8000000',
            descripcion: 'Diseñarás flujos completos, desde la investigación con usuarios hasta la entrega a desarrollo, y cuidarás el sistema de diseño del producto.',
            stack: 'Figma, Design Systems, Prototipado, User Research', soft: 'Empatía, Pensamiento crítico' },
        { titulo: 'Data Analyst', categoria: 'Data / Analytics', seniority: 'Junior (0 – 2 años)', modalidad: 'Remoto', min: '4000000', max: '6000000',
            descripcion: 'Convertirás datos de uso en decisiones: tableros, análisis de embudos y experimentos para producto y crecimiento.',
            stack: 'SQL, Python, Power BI, Excel', soft: 'Pensamiento crítico, Comunicación asertiva' }
    ];

    function porId(id) { return document.getElementById(id); }
    function poner(el, valor) {
        if (!el) return;
        el.value = valor;
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
    }
    function siVacio(id, valor) {
        var el = porId(id);
        if (el && !el.value) poner(el, valor);
    }
    function elegir(select, texto) {
        if (!select) return;
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].text.trim() === texto || select.options[i].value === texto) { poner(select, select.options[i].value); return; }
        }
    }
    function clic(id) { var b = porId(id); if (b) b.click(); }

    function siguienteEjemplo() {
        var n = 0;
        try { n = parseInt(localStorage.getItem('dc_ejemplo_oferta') || '0', 10) || 0; localStorage.setItem('dc_ejemplo_oferta', String((n + 1) % EJEMPLOS.length)); } catch (e) { n = Math.floor(Date.now() / 1000); }
        return EJEMPLOS[n % EJEMPLOS.length];
    }

    function rellenar() {
        var e = siguienteEjemplo();
        // paso 1: la empresa (solo lo que esté vacío)
        siVacio('empresa-nombre', 'DevCoast Labs');
        if (!porId('empresa-sector').value) elegir(porId('empresa-sector'), 'SaaS / Software');
        var tam = porId('empresa-tamaño');
        if (tam && !tam.value && tam.options.length > 3) poner(tam, tam.options[3].value);
        siVacio('empresa-desc', 'Plataformas de talento tech con operación en Colombia y México.');
        clic('btn-siguiente');

        // paso 2: el rol
        poner(porId('rol-titulo'), e.titulo);
        elegir(porId('rol-categoria'), e.categoria);
        elegir(porId('rol-seniority'), e.seniority);
        elegir(porId('rol-modalidad'), e.modalidad);
        elegir(porId('rol-contrato'), 'Tiempo completo');
        poner(porId('rol-vacantes'), '1');
        var cop = document.querySelector('#currency-group [data-currency="COP"]');
        if (cop) cop.click();
        poner(porId('salario-min'), e.min);
        poner(porId('salario-max'), e.max);
        clic('btn-siguiente-1');

        // paso 3: requisitos (las etiquetas escritas se convierten al pulsar Siguiente)
        poner(porId('req-descripcion'), e.descripcion);
        poner(porId('stack-input'), e.stack);
        poner(porId('soft-input'), e.soft);
        clic('btn-siguiente-2');

        // paso 4: resumen, listo para publicar
        var publicar = porId('btn-publicar');
        if (publicar) publicar.focus();
    }

    btn.addEventListener('click', rellenar);
})();
