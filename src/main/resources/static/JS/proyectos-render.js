/* DevCoast — motor de presentación del dibujito de card compartido.
   Convierte datos reales de un proyecto (categoria, colorIcono, el Estilo
   elegido) en el canvas de la vitrina — código, diseño, señal, vacío,
   onda o mesa — usado tanto por la grilla real (proyectos-vitrina.js)
   como por la vista previa en vivo del formulario de subir/editar
   proyecto (s-proyecto.js), para que ambos muestren exactamente el mismo
   componente en vez de dos previews distintas que puedan
   desincronizarse entre sí. */
(function (global) {

    function escapeHtml(s) {
        return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
    }

    // hash determinístico simple (djb2) — mismo proyecto, mismo dibujo siempre
    function hashStr(s) {
        let h = 5381
        for (let i = 0; i < s.length; i++) h = ((h << 5) + h + s.charCodeAt(i)) | 0
        return Math.abs(h)
    }
    function mulberry32(seed) {
        let a = seed
        return function () {
            a |= 0; a = (a + 0x6D2B79F5) | 0
            let t = Math.imul(a ^ (a >>> 15), 1 | a)
            t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296
        }
    }

    function hexToRgb(hex) {
        const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex || '')
        return m ? { r: parseInt(m[1], 16), g: parseInt(m[2], 16), b: parseInt(m[3], 16) } : { r: 79, g: 160, b: 232 }
    }
    function rgbToHex(r, g, b) {
        const c = v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')
        return `#${c(r)}${c(g)}${c(b)}`
    }
    // mezcla hacia blanco (amt>0) o negro (amt<0) — variantes de un único
    // color real (colorIcono) en vez de inventar una paleta de 4 colores
    function shade(hex, amt) {
        const { r, g, b } = hexToRgb(hex)
        const t = amt > 0 ? 255 : 0
        const k = Math.abs(amt)
        return rgbToHex(r + (t - r) * k, g + (t - g) * k, b + (t - b) * k)
    }
    // color de texto legible sobre cualquier acento real, por luminancia
    function inkFor(hex) {
        const { r, g, b } = hexToRgb(hex)
        const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return lum > 0.6 ? '#0a1826' : '#f3ede2'
    }

    const CODE_CATEGORIAS = ['frontend', 'backend', 'full stack', 'dashboard saas', 'app movil', 'app móvil']
    function classifyMode(categoria) {
        const c = (categoria || '').toLowerCase()
        return CODE_CATEGORIAS.some(k => c.includes(k)) ? 'code' : 'design'
    }

    const EXT_RULES = [
        [/react|typescript|next\.?js/i, '.tsx'], [/vue/i, '.vue'], [/angular/i, '.ts'],
        [/javascript|node/i, '.js'], [/python|django|flask/i, '.py'], [/java(?!script)/i, '.java'],
        [/php|laravel/i, '.php'], [/figma/i, '.fig'], [/illustrator/i, '.ai'], [/photoshop/i, '.psd'],
        [/after ?effects/i, '.aep'], [/blender|cinema ?4d|3ds ?max/i, '.blend'], [/procreate/i, '.pro'],
        [/indesign/i, '.indd'], [/webflow/i, '.wf'], [/sketch/i, '.sketch']
    ]
    function guessExt(stackText) {
        for (const [re, ext] of EXT_RULES) if (re.test(stackText)) return ext
        return '.proj'
    }

    const WATERMARKS = [
        '<path d="M3 3v18h18"/><rect x="7" y="12" width="3" height="6"/><rect x="12" y="8" width="3" height="10"/><rect x="17" y="5" width="3" height="13"/>',
        '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>',
        '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/><path d="M3.27 6.96 12 12.01l8.73-5.05"/><path d="M12 22.08V12"/>',
        '<path d="M12 2 2 7l10 5 10-5-10-5Z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>',
        '<circle cx="12" cy="12" r="10"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/><line x1="2" y1="12" x2="22" y2="12"/>'
    ]

    function slugify(nombre) {
        const acentos = { 'á': 'a', 'é': 'e', 'í': 'i', 'ó': 'o', 'ú': 'u', 'ñ': 'n', 'ü': 'u' }
        return (nombre || '').toLowerCase().replace(/[áéíóúñü]/g, c => acentos[c] || c)
            .replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '') || 'proyecto'
    }

    function buildCodeCanvas({ nombre, categoria, tipo, stack, seed }) {
        stack = stack || []
        const names = escapeHtml(stack.length ? stack.slice(0, 2).join(', ') : (categoria || 'stack'))
        const slug = slugify(nombre)
        const ext = guessExt(stack.join(' '))
        const wm = WATERMARKS[seed % WATERMARKS.length]

        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><span class="proj-dot r"></span><span class="proj-dot y"></span><span class="proj-dot g"></span></div>
                <div class="bar-right">
                    <span class="proj-dims-inline">${escapeHtml(categoria || '')}</span>
                    <span class="proj-ext">${slug}${ext}</span>
                </div>
            </div>`
        const stage = `
            <div class="proj-canvas-stage">
                <div class="proj-code-block">
                    <span class="ln">1</span><span class="cl"><span class="kw">import</span> { <span class="fn">${names}</span> } <span class="kw">from</span> <span class="str">'./${slug}'</span></span>
                    <span class="ln">2</span><span class="cl cm">// ${escapeHtml(categoria || '')}${tipo ? ' · ' + escapeHtml(tipo) : ''}</span>
                </div>
                <span class="proj-watermark"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${wm}</svg></span>
            </div>`
        return { modeClass: 'mode-code', shapeClass: 'shape-square', discLabel: 'código', bar, stage }
    }

    function buildDesignCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const c1 = shade(accent, -0.35 - rnd() * 0.15)
        const c2 = shade(accent, 0.4 + rnd() * 0.2)
        const w = 240, h = 90
        const p1 = { x: 18 + rnd() * 20, y: 55 + rnd() * 20 }
        const p2 = { x: 100 + rnd() * 40, y: 15 + rnd() * 35 }
        const p3 = { x: 190 + rnd() * 30, y: 25 + rnd() * 40 }
        const hx = p2.x + (rnd() - 0.5) * 50, hy = p2.y + (rnd() - 0.5) * 40
        const path = `M${p1.x.toFixed(0)},${p1.y.toFixed(0)} C${(p1.x + 25).toFixed(0)},${p1.y.toFixed(0)} ${(p2.x - 25).toFixed(0)},${p2.y.toFixed(0)} ${p2.x.toFixed(0)},${p2.y.toFixed(0)} S${(p3.x - 30).toFixed(0)},${p3.y.toFixed(0)} ${p3.x.toFixed(0)},${p3.y.toFixed(0)}`

        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 3v18M3 12h18"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `
            <div class="proj-canvas-stage">
                <span class="proj-vector">
                    <svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet" style="width:100%;height:100%;">
                        <path class="pv-path" d="${path}" stroke="rgba(255,255,255,.55)"/>
                        <line class="pv-handle" x1="${p2.x.toFixed(0)}" y1="${p2.y.toFixed(0)}" x2="${hx.toFixed(0)}" y2="${hy.toFixed(0)}" stroke="rgba(255,255,255,.55)"/>
                        <circle class="pv-anchor" cx="${p1.x.toFixed(0)}" cy="${p1.y.toFixed(0)}" r="4.5" fill="${c1}" stroke="${shade(accent, .8)}"/>
                        <circle class="pv-anchor" cx="${p2.x.toFixed(0)}" cy="${p2.y.toFixed(0)}" r="4.5" fill="${c2}" stroke="${shade(accent, .8)}"/>
                        <circle class="pv-anchor" cx="${p3.x.toFixed(0)}" cy="${p3.y.toFixed(0)}" r="4.5" fill="${shade(accent, .8)}" stroke="${c1}"/>
                        <circle cx="${hx.toFixed(0)}" cy="${hy.toFixed(0)}" r="2.6" fill="${shade(accent, .8)}" stroke="rgba(255,255,255,.6)"/>
                    </svg>
                </span>
            </div>`
        return { modeClass: 'mode-design', shapeClass: '', discLabel: 'diseño', bar, stage }
    }

    // pulsos concéntricos tipo radar/señal, emitiendo desde un punto — la
    // única de las seis con movimiento real (ver @keyframes projSignalPing)
    function buildSignalCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const cx = (28 + rnd() * 44).toFixed(1)
        const cy = (32 + rnd() * 34).toFixed(1)
        const dotColor = shade(accent, .85)
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 0 1 0 20"/><path d="M12 6a6 6 0 0 1 0 12"/><path d="M12 10a2 2 0 0 1 0 4"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const rings = [0, 1, 2].map(i => `<span class="proj-signal-ring" style="left:${cx}%;top:${cy}%;border-color:${dotColor};animation-delay:${(i * 0.8).toFixed(1)}s;"></span>`).join('')
        const stage = `
            <div class="proj-canvas-stage">
                <span class="proj-signal-dot" style="left:${cx}%;top:${cy}%;background:${dotColor};"></span>
                ${rings}
            </div>`
        return { modeClass: 'mode-signal', shapeClass: '', discLabel: 'señal', bar, stage }
    }

    // puntos dispersos tipo starfield — algunos titilan lento, la mayoría
    // quieta (ver @keyframes projVoidTwinkle)
    function buildVoidCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const tone = shade(accent, .85)
        let dots = ''
        for (let i = 0; i < 16; i++) {
            const x = (rnd() * 94 + 3).toFixed(1)
            const y = (rnd() * 80 + 8).toFixed(1)
            const s = (rnd() * 3 + 1.5).toFixed(1)
            const op = (rnd() * 0.5 + 0.25).toFixed(2)
            const twinkle = i % 4 === 0
            dots += `<span class="proj-void-dot${twinkle ? ' proj-void-twinkle' : ''}" style="left:${x}%;top:${y}%;width:${s}px;height:${s}px;opacity:${op};background:${tone};${twinkle ? 'animation-delay:' + (rnd() * 3).toFixed(1) + 's;' : ''}"></span>`
        }
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="6" cy="8" r="1"/><circle cx="18" cy="16" r="1"/><circle cx="9" cy="17" r="1"/><circle cx="16" cy="7" r="1"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `<div class="proj-canvas-stage">${dots}</div>`
        return { modeClass: 'mode-void', shapeClass: '', discLabel: 'vacío', bar, stage }
    }

    // barras de ecualizador — reutiliza .proj-eq/.proj-eq-bar, que ya
    // existían en el CSS de una versión anterior de la vitrina
    function buildWaveCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        let bars = ''
        for (let i = 0; i < 14; i++) {
            const h = Math.round(18 + rnd() * 62)
            const tone = shade(accent, -0.25 + rnd() * 0.55)
            bars += `<span class="proj-eq-bar" style="height:${h}%;background:${tone};"></span>`
        }
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12h3l3-8 4 16 3-8h5"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `<div class="proj-canvas-stage"><div class="proj-eq">${bars}</div></div>`
        return { modeClass: 'mode-wave', shapeClass: '', discLabel: 'onda', bar, stage }
    }

    // grilla + marcas de esquina tipo mesa de trabajo (Figma/Illustrator) —
    // el contraste de las líneas sigue la luminancia del acento, no un
    // color fijo, para que se lea igual con cualquier color elegido
    function buildArtboardCanvas({ categoria, accent, seed }) {
        const ink = inkFor(accent)
        const line = ink === '#0a1826' ? 'rgba(10,24,38,.35)' : 'rgba(255,255,255,.35)'
        const tick = ink === '#0a1826' ? 'rgba(10,24,38,.55)' : 'rgba(255,255,255,.55)'
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="1"/><path d="M3 9h18M9 3v18"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const corner = cls => `<span class="proj-artboard-corner ${cls}" style="border-color:${tick};"></span>`
        const stage = `
            <div class="proj-canvas-stage">
                <div class="proj-artboard-grid" style="background-image:linear-gradient(${line} 1px, transparent 1px), linear-gradient(90deg, ${line} 1px, transparent 1px); background-size:18px 18px;"></div>
                ${corner('tl')}${corner('tr')}${corner('bl')}${corner('br')}
            </div>`
        return { modeClass: 'mode-artboard', shapeClass: '', discLabel: 'mesa', bar, stage }
    }

    // grafo de nodos — cada nodo se conecta a uno anterior random, así
    // siempre da un árbol conexo (no un caos de líneas cruzadas)
    function buildMeshCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const w = 240, h = 100
        const n = 7
        const nodes = []
        for (let i = 0; i < n; i++) nodes.push({ x: 16 + rnd() * (w - 32), y: 14 + rnd() * (h - 28) })
        let lines = ''
        for (let i = 1; i < n; i++) {
            const j = Math.floor(rnd() * i)
            lines += `<line x1="${nodes[i].x.toFixed(0)}" y1="${nodes[i].y.toFixed(0)}" x2="${nodes[j].x.toFixed(0)}" y2="${nodes[j].y.toFixed(0)}" stroke="${shade(accent, .7)}" stroke-width="1.2" opacity=".6"/>`
        }
        const dots = nodes.map((p, i) => `<circle cx="${p.x.toFixed(0)}" cy="${p.y.toFixed(0)}" r="${i === 0 ? 5 : 3.2}" fill="${shade(accent, .85)}" stroke="${shade(accent, -.3)}" stroke-width="1"/>`).join('')
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="6" cy="6" r="2"/><circle cx="18" cy="6" r="2"/><circle cx="12" cy="18" r="2"/><path d="M6 8l5 8M18 8l-5 8"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `
            <div class="proj-canvas-stage">
                <svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet" style="width:100%;height:100%;">${lines}${dots}</svg>
            </div>`
        return { modeClass: 'mode-mesh', shapeClass: '', discLabel: 'grafo', bar, stage }
    }

    // boceto — barras de ancho variable apiladas, tipo wireframe/skeleton
    // de contenido (el lenguaje visual estándar de un mockup de layout)
    function buildSketchCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const tone = shade(accent, .8)
        let bars = ''
        for (let i = 0; i < 5; i++) {
            const w = Math.round(35 + rnd() * 55)
            const op = (0.35 + rnd() * 0.45).toFixed(2)
            bars += `<span class="proj-sketch-bar" style="width:${w}%;background:${tone};opacity:${op};"></span>`
        }
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="3" rx="1"/><rect x="3" y="10.5" width="12" height="3" rx="1"/><rect x="3" y="17" width="15" height="3" rx="1"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `<div class="proj-canvas-stage"><div class="proj-sketch">${bars}</div></div>`
        return { modeClass: 'mode-sketch', shapeClass: '', discLabel: 'boceto', bar, stage }
    }

    // grilla de celdas rellenas con opacidad al azar, tipo heatmap/mosaico
    // de píxeles — la contraparte "sólida" de la grilla de líneas de mesa
    function buildPixelCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const cols = 10, rows = 4
        const tone = shade(accent, .6)
        let cells = ''
        for (let i = 0; i < cols * rows; i++) {
            const op = (rnd() * 0.75 + 0.1).toFixed(2)
            cells += `<span class="proj-pixel-cell" style="background:${tone};opacity:${op};"></span>`
        }
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `<div class="proj-canvas-stage"><div class="proj-pixel-grid" style="grid-template-columns:repeat(${cols},1fr);grid-template-rows:repeat(${rows},1fr);">${cells}</div></div>`
        return { modeClass: 'mode-pixel', shapeClass: '', discLabel: 'mosaico', bar, stage }
    }

    // anillos orbitales elípticos con un "planeta" en cada uno — la
    // contraparte estática/cósmica del pulso concéntrico de señal
    function buildOrbitCanvas({ categoria, accent, seed }) {
        const rnd = mulberry32(seed)
        const w = 240, h = 100
        const cx = w / 2, cy = h / 2 + 4
        const rings = [{ rx: 30, ry: 12 }, { rx: 58, ry: 22 }, { rx: 90, ry: 33 }]
        const ellipses = rings.map(r => `<ellipse cx="${cx}" cy="${cy}" rx="${r.rx}" ry="${r.ry}" fill="none" stroke="${shade(accent, .7)}" stroke-width="1" opacity=".5"/>`).join('')
        const planets = rings.map((r, i) => {
            const angle = rnd() * Math.PI * 2
            const px = cx + Math.cos(angle) * r.rx
            const py = cy + Math.sin(angle) * r.ry
            return `<circle cx="${px.toFixed(0)}" cy="${py.toFixed(0)}" r="${3 + i}" fill="${shade(accent, .85)}"/>`
        }).join('')
        const bar = `
            <div class="proj-canvas-bar">
                <div class="bar-left"><svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="2.5"/><ellipse cx="12" cy="12" rx="10" ry="4"/></svg></div>
                <div class="bar-right"><span class="proj-dims-inline">${escapeHtml(categoria || '')}</span></div>
            </div>`
        const stage = `
            <div class="proj-canvas-stage">
                <svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid meet" style="width:100%;height:100%;">${ellipses}<circle cx="${cx.toFixed(0)}" cy="${cy.toFixed(0)}" r="4" fill="${shade(accent, .9)}"/>${planets}</svg>
            </div>`
        return { modeClass: 'mode-orbit', shapeClass: '', discLabel: 'órbita', bar, stage }
    }

    // el Estilo elegido en el formulario (banner/coverActual) define qué
    // dibujito se pinta — classifyMode(categoria) queda solo de fallback
    // para proyectos viejos guardados con un banner de la tanda anterior
    const STYLE_BUILDERS = {
        'cover-terminal': buildCodeCanvas,
        'cover-artboard': buildArtboardCanvas,
        'cover-signal': buildSignalCanvas,
        'cover-void': buildVoidCanvas,
        'cover-mint-glow': buildDesignCanvas,
        'cover-amber-glow': buildWaveCanvas,
        'cover-mesh': buildMeshCanvas,
        'cover-sketch': buildSketchCanvas,
        'cover-pixel': buildPixelCanvas,
        'cover-orbit': buildOrbitCanvas
    }
    function buildCanvas(params) {
        const builder = STYLE_BUILDERS[params.estilo]
        if (builder) return builder(params)
        const mode = classifyMode(params.categoria)
        return mode === 'code' ? buildCodeCanvas(params) : buildDesignCanvas(params)
    }

    function buildTagsHTML(stack, etiquetas) {
        stack = stack || []; etiquetas = etiquetas || []
        return stack.map(t => `<span class="tag-code">${escapeHtml(t)}</span>`).join('') +
            etiquetas.map(t => `<span class="tag-tool">${escapeHtml(t)}</span>`).join('')
    }

    function buildSwatchesHTML(accent) {
        const tones = [shade(accent, -0.3), accent, shade(accent, 0.35)]
        return tones.map(hex => `<span class="proj-swatch" style="background:${hex}"></span>`).join('')
    }

    global.ProyectosRender = {
        escapeHtml, hashStr, mulberry32, shade, inkFor, classifyMode, guessExt, slugify,
        buildCodeCanvas, buildDesignCanvas, buildSignalCanvas, buildVoidCanvas, buildWaveCanvas, buildArtboardCanvas,
        buildMeshCanvas, buildSketchCanvas, buildPixelCanvas, buildOrbitCanvas,
        buildCanvas, buildTagsHTML, buildSwatchesHTML
    }
})(window)
