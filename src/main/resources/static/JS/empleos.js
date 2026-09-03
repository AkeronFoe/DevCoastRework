document.addEventListener('DOMContentLoaded', () => {

    const reduce = !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches)
    const hasGsap = typeof window.gsap !== 'undefined'
    if (hasGsap && window.ScrollTrigger) gsap.registerPlugin(ScrollTrigger)

    /* ── Barra de progreso en navegación ── */
    const navBar = document.getElementById('nav-progress')
    if (navBar) {
        document.querySelectorAll('a[href]').forEach(a => {
            a.addEventListener('click', e => {
                if (e.metaKey || e.ctrlKey || e.shiftKey || a.target === '_blank') return
                navBar.style.transition = 'none'
                navBar.style.width = '0'
                navBar.style.opacity = '1'
                requestAnimationFrame(() => {
                    navBar.style.transition = 'width .5s cubic-bezier(.4,0,.2,1)'
                    navBar.style.width = '80%'
                })
            })
        })
        window.addEventListener('load', () => {
            navBar.style.width = '100%'
            setTimeout(() => { navBar.style.opacity = '0' }, 250)
        })
    }

    /* ── Fechas relativas desde MongoDB ObjectId, y marca "nuevo" si es
       reciente (< 48h) — mismo id que ya usa la card para el color fallback,
       ninguna llamada de red extra */
    function idToRelDate(hexId) {
        if (!hexId || hexId.length < 8) return { text: '', ageMs: Infinity }
        const ts = parseInt(hexId.substring(0, 8), 16) * 1000
        const d = Date.now() - ts
        let text
        if (d < 3600000) text = 'Hace menos de 1h'
        else if (d < 86400000) text = `Hace ${Math.floor(d / 3600000)}h`
        else if (d < 2 * 86400000) text = 'Ayer'
        else if (d < 7 * 86400000) text = `Hace ${Math.floor(d / 86400000)} días`
        else if (d < 30 * 86400000) text = `Hace ${Math.floor(d / (7 * 86400000))} sem.`
        else if (d < 365 * 86400000) text = `Hace ${Math.floor(d / (30 * 86400000))} meses`
        else text = 'Hace más de 1 año'
        return { text, ageMs: d }
    }

    /* ── Color real del empleador (colorIcono) propagado a --card-accent
       del card entero — banda, medallón y comilla de la cita lo leen de
       ahí — con un fallback determinístico por nombre cuando falta ── */
    function strToHsl(str) {
        let h = 0
        for (let i = 0; i < str.length; i++) h = ((h << 5) - h) + str.charCodeAt(i)
        return `hsl(${Math.abs(h) % 360}, 55%, 55%)`
    }

    /* ── Texto legible sobre cualquier color de empresa: en vez de un
       #0a1826 fijo que se volvía ilegible contra colores oscuros (propios
       de una empresa o del fallback anterior), se calcula la luminancia
       real del color y se elige texto oscuro o claro según corresponda ── */
    function parseColorToRgb(str) {
        if (!str) return null
        str = str.trim()
        if (str[0] === '#') {
            let hex = str.slice(1)
            if (hex.length === 3) hex = hex.split('').map(c => c + c).join('')
            if (hex.length !== 6) return null
            const num = parseInt(hex, 16)
            if (Number.isNaN(num)) return null
            return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 }
        }
        const m = str.match(/^hsl\(\s*([\d.]+)\s*,\s*([\d.]+)%\s*,\s*([\d.]+)%\s*\)$/i)
        if (!m) return null
        const h = parseFloat(m[1]) / 360, s = parseFloat(m[2]) / 100, l = parseFloat(m[3]) / 100
        if (s === 0) { const v = Math.round(l * 255); return { r: v, g: v, b: v } }
        const q = l < 0.5 ? l * (1 + s) : l + s - l * s
        const p = 2 * l - q
        const hue2rgb = (p, q, t) => {
            if (t < 0) t += 1
            if (t > 1) t -= 1
            if (t < 1 / 6) return p + (q - p) * 6 * t
            if (t < 1 / 2) return q
            if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
            return p
        }
        return {
            r: Math.round(hue2rgb(p, q, h + 1 / 3) * 255),
            g: Math.round(hue2rgb(p, q, h) * 255),
            b: Math.round(hue2rgb(p, q, h - 1 / 3) * 255)
        }
    }
    function inkColorFor(colorStr) {
        const rgb = parseColorToRgb(colorStr)
        if (!rgb) return '#0a1826'
        const luminance = (0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b) / 255
        return luminance > 0.55 ? '#0a1826' : '#f2f5f8'
    }

    const cards = Array.from(document.querySelectorAll('.job-card[data-empleo-id]'))

    cards.forEach((card, i) => {
        card.style.animationDelay = `${Math.min(i * 0.04, 0.22)}s`

        const dateEl = card.querySelector('.job-date')
        const rel = idToRelDate(card.dataset.empleoId)
        if (dateEl) dateEl.textContent = rel.text
        if (rel.ageMs < 48 * 3600000 && !card.querySelector('.job-featured-tag')) {
            const tag = document.createElement('span')
            tag.className = 'job-new-tag'
            tag.textContent = 'nuevo'
            card.insertBefore(tag, card.firstChild)
        }

        let bg = (card.querySelector('.job-band') || {}).dataset?.color
        if (!bg || bg === 'null' || bg === 'undefined') {
            const name = card.querySelector('.job-title')?.textContent || 'DC'
            bg = strToHsl(name)
        }
        card.style.setProperty('--card-accent', bg)
        card.style.setProperty('--card-ink', inkColorFor(bg))
    })

    /* ── Mismo cálculo de contraste para los avatares de "empresas
       contratando esta semana" — traen su color por style inline desde
       el servidor, no por --card-accent ── */
    document.querySelectorAll('.ws-hiring-avatars span[style]').forEach(span => {
        const m = (span.getAttribute('style') || '').match(/background-color:\s*([^;]+)/i)
        span.style.color = inkColorFor(m ? m[1].trim() : null)
    })

    /* ── Anillo de match: relleno con leve overshoot, como una aguja que
       pasa de largo y se asienta, no un fill lineal plano. Cuando hay GSAP
       la animación de scroll de más abajo se encarga del relleno — este
       bloque solo cubre el caso sin JS de animación (reduced-motion o sin
       GSAP), fijando el offset final de una vez ── */
    if (!hasGsap || reduce) {
        document.querySelectorAll('.job-ring .prog').forEach(circle => {
            const pct = parseFloat(circle.dataset.pct)
            const full = 100.5
            const target = full * (1 - pct / 100)
            circle.style.strokeDasharray = String(full)
            circle.style.strokeDashoffset = String(target)
        })
    }

    /* ── Copiar enlace de empleo ── */
    async function copyText(text) {
        if (navigator?.clipboard?.writeText) {
            try { await navigator.clipboard.writeText(text); return true } catch { /**/ }
        }
        const ta = document.createElement('textarea')
        Object.assign(ta.style, { position: 'absolute', left: '-9999px' })
        ta.value = text; ta.readOnly = true
        document.body.appendChild(ta); ta.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(ta); return ok
    }

    document.querySelectorAll('.btn-copy-empleo[data-copy-url]').forEach(btn => {
        const label = btn.querySelector('.copy-label')
        const orig = label?.textContent || 'Copiar'
        btn.addEventListener('click', async e => {
            e.preventDefault(); e.stopPropagation()
            const path = btn.dataset.copyUrl
            if (!path) return
            const url = path.startsWith('http') ? path : location.origin + path
            if (await copyText(url)) {
                btn.classList.add('is-copied')
                if (label) label.textContent = '¡Copiado!'
                setTimeout(() => {
                    btn.classList.remove('is-copied')
                    if (label) label.textContent = orig
                }, 1400)
            }
        })
    })

    /* ── Botón volver arriba ── */
    const btt = document.getElementById('back-to-top')
    if (btt) {
        window.addEventListener('scroll', () => {
            btt.classList.toggle('is-visible', window.scrollY > 300)
        }, { passive: true })
        btt.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }))
    }

    /* ── Toggle favoritos: filtro real de servidor (empleoController lee
       ?favoritos=true), no display:none sobre lo ya cargado — así respeta
       la paginación en vez de esconder favoritos que cayeron en otra
       página. El estado activo/inactivo ya viene renderizado desde el
       server (th:classappend en el template), acá solo se dispara la
       navegación con el resto de filtros ya puestos ── */
    const toggle = document.getElementById('fav-only-toggle')
    if (toggle) {
        const toggleActive = () => {
            toggle.classList.toggle('active')
            toggle.setAttribute('aria-pressed', toggle.classList.contains('active') ? 'true' : 'false')
            if (typeof window.applyFiltersToUrl === 'function') window.applyFiltersToUrl()
        }

        toggle.addEventListener('click', e => { e.preventDefault(); toggleActive() })
        toggle.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggleActive() }
        })
    }

    /* ── Sidebar: colapsar grupos ── */
    document.querySelectorAll('.ws-side-head').forEach(btn => {
        btn.addEventListener('click', () => {
            const group = btn.closest('.ws-side-group')
            const collapsed = group.classList.toggle('is-collapsed')
            btn.setAttribute('aria-expanded', collapsed ? 'false' : 'true')
        })
    })

    /* ── Sidebar: menú móvil ── */
    const side = document.getElementById('side')
    const scrim = document.getElementById('scrim')
    const menuBtn = document.getElementById('menuBtn')
    function closeSide() { side?.classList.remove('is-open'); scrim?.classList.remove('is-open'); menuBtn?.setAttribute('aria-expanded', 'false') }
    function openSide() { side?.classList.add('is-open'); scrim?.classList.add('is-open'); menuBtn?.setAttribute('aria-expanded', 'true') }
    menuBtn?.addEventListener('click', () => { side?.classList.contains('is-open') ? closeSide() : openSide() })
    scrim?.addEventListener('click', closeSide)

    /* ── Campana de notificaciones ── */
    const bellBtn = document.getElementById('bellBtn')
    const bellDrop = document.getElementById('bellDrop')
    if (bellBtn && bellDrop) {
        bellBtn.addEventListener('click', e => {
            e.stopPropagation()
            const isOpen = bellDrop.classList.toggle('is-open')
            bellBtn.classList.toggle('is-open', isOpen)
        })
        document.addEventListener('click', e => {
            if (!bellBtn.contains(e.target) && !bellDrop.contains(e.target)) {
                bellDrop.classList.remove('is-open')
                bellBtn.classList.remove('is-open')
            }
        })
    }
    document.getElementById('markAllRead')?.addEventListener('click', function () {
        fetch('/notificaciones/leer-todas', { method: 'POST', headers: { 'Content-Type': 'application/json' } })
            .then(r => r.json()).then(d => {
                if (d.ok) {
                    document.querySelectorAll('.ws-notif.is-unread').forEach(el => el.classList.remove('is-unread'))
                    document.querySelectorAll('.ws-notif-dot').forEach(el => el.remove())
                    document.querySelectorAll('.ws-bell-badge, .ws-bell-drop-count, .ws-bell-drop-foot').forEach(el => el.remove())
                }
            }).catch(() => {})
    })

    /* ── Numeración del gutter, como el margen de un editor de código —
       sigue el alto real del contenido en vez de un rango fijo que se
       quedaba corto en páginas largas.
       Medido con el trace de rendimiento de Chrome DevTools: esto era uno
       de los dos causantes reales del lag en scroll (el otro es el propio
       refresh interno de ScrollTrigger). El ResizeObserver de .ws-content
       puede disparar varias veces seguidas mientras el layout se acomoda
       (content-visibility de las job cards, animaciones de entrada), y
       cada disparo forzaba un reflow síncrono (offsetHeight) y reescribía
       hasta ~150 <br> por innerHTML — el elemento con más hijos de toda la
       página. Ahora: se agrupan ráfagas de resize en un solo tick con
       requestAnimationFrame, el alto de línea se calcula una sola vez (no
       cambia en caliente), y si el número de líneas no cambió no se toca
       el DOM ── */
    const gutterEl = document.getElementById('gutterLines')
    const contentEl = document.querySelector('.ws-content')
    if (gutterEl && contentEl) {
        const lineHeight = parseFloat(getComputedStyle(gutterEl).fontSize) * 2.05
        let lastLines = -1
        let gutterRaf = null

        const updateGutter = () => {
            const lines = Math.max(20, Math.ceil((contentEl.offsetHeight - 28) / lineHeight))
            if (lines === lastLines) return
            lastLines = lines
            const out = []
            for (let i = 1; i <= lines; i++) out.push(i < 10 ? '0' + i : String(i))
            gutterEl.innerHTML = out.join('<br>')
        }
        const scheduleUpdateGutter = () => {
            if (gutterRaf) return
            gutterRaf = requestAnimationFrame(() => { gutterRaf = null; updateGutter() })
        }

        updateGutter()
        if (typeof ResizeObserver !== 'undefined') {
            new ResizeObserver(scheduleUpdateGutter).observe(contentEl)
        } else {
            window.addEventListener('resize', scheduleUpdateGutter)
        }
    }

    /* ── Overlay de arranque — un timeline real (relleno, cambio de línea,
       fundido) en vez de setTimeouts sueltos. bootExtra es cuánto tiempo
       se queda encima de todo: cualquier entrada "por encima del pliegue"
       (el sello de match, los avatares de contratación) tiene que esperar
       al menos eso o termina de jugarse detrás de la cortina, ya en reposo
       cuando esta se levanta, como si no hubiera pasado nada.
       Solo se reproduce una vez por sesión de navegador: cada filtro/orden
       navega a una URL nueva (recarga completa), así que sin esto la
       cortina de "arranque" se repetía en cada clic de filtro ── */
    let bootExtra = 0
    const boot = document.getElementById('wsBoot')
    if (boot) {
        let yaArranco = false
        try { yaArranco = sessionStorage.getItem('wsBootPlayed') === '1' } catch { /* storage bloqueado, se trata como primera vez */ }

        if (reduce || !hasGsap || yaArranco) {
            boot.classList.add('is-done')
            boot.style.opacity = '0'; boot.style.visibility = 'hidden'
        } else {
            bootExtra = 1.05
            const bootFill = document.getElementById('bootFill')
            const bootLast = document.getElementById('bootLast')
            gsap.timeline()
                .to(bootFill, { width: '100%', duration: .55, ease: 'power2.out' })
                .call(() => { if (bootLast) bootLast.innerHTML = '<span class="ok">✓</span> listo en 0.5s' }, null, '-=0.15')
                .to(boot, {
                    autoAlpha: 0, duration: .35, ease: 'power1.in', delay: .1,
                    onComplete: () => boot.classList.add('is-done')
                })
        }
        try { sessionStorage.setItem('wsBootPlayed', '1') } catch { /* no-op */ }
    }

    /* ── Paleta de comandos (⌘K) — lee los propios enlaces del sidebar
       como fuente de datos, así nunca se desincroniza de qué secciones
       están realmente disponibles para talento/empresa ── */
    const paletteScrim = document.getElementById('paletteScrim')
    const paletteInput = document.getElementById('paletteInput')
    const paletteList = document.getElementById('paletteList')
    const paletteOpenBtn = document.getElementById('paletteOpen')
    const paletteBox = paletteScrim?.querySelector('.ws-palette')
    if (paletteScrim && paletteInput && paletteList && paletteOpenBtn) {
        const paletteItems = Array.from(document.querySelectorAll('.ws-side-link')).map(a => ({
            label: a.querySelector('.name')?.textContent.trim() || '',
            ext: a.querySelector('.ext')?.textContent.trim() || '',
            href: a.getAttribute('href')
        })).filter(it => it.label)

        let selIndex = 0
        let currentMatches = paletteItems

        const renderPalette = query => {
            const q = (query || '').toLowerCase()
            currentMatches = paletteItems.filter(it => it.label.toLowerCase().indexOf(q) !== -1)
            paletteList.innerHTML = ''
            if (!currentMatches.length) {
                const empty = document.createElement('div')
                empty.className = 'ws-palette-empty'
                empty.textContent = `Sin resultados para "${query}"`
                paletteList.appendChild(empty)
                return
            }
            selIndex = Math.max(0, Math.min(selIndex, currentMatches.length - 1))
            currentMatches.forEach((it, i) => {
                const row = document.createElement('div')
                row.className = 'ws-palette-item' + (i === selIndex ? ' is-sel' : '')
                const label = document.createElement('span')
                label.textContent = it.label
                const ext = document.createElement('span')
                ext.className = 'ext'
                ext.textContent = it.ext
                row.append(label, ext)
                row.addEventListener('mouseenter', () => { selIndex = i; renderPalette(paletteInput.value) })
                row.addEventListener('click', goToSelected)
                paletteList.appendChild(row)
            })
        }

        function goToSelected() {
            const item = currentMatches[selIndex]
            closePalette()
            if (item?.href) window.location = item.href
        }

        function openPalette() {
            paletteScrim.classList.add('is-open'); selIndex = 0; paletteInput.value = ''; renderPalette('')
            if (hasGsap && !reduce) {
                gsap.killTweensOf([paletteScrim, paletteBox])
                gsap.set(paletteScrim, { autoAlpha: 1 })
                gsap.fromTo(paletteBox, { scale: .98 }, { scale: 1, duration: .1, ease: 'power1.out' })
            } else {
                paletteScrim.style.opacity = 1; paletteScrim.style.visibility = 'visible'
            }
            setTimeout(() => paletteInput.focus(), 10)
        }
        function closePalette() {
            paletteOpenBtn.focus()
            if (hasGsap && !reduce) {
                gsap.killTweensOf([paletteScrim, paletteBox])
                gsap.to(paletteScrim, {
                    autoAlpha: 0, duration: .08, ease: 'power1.in',
                    onComplete: () => paletteScrim.classList.remove('is-open')
                })
            } else {
                paletteScrim.classList.remove('is-open')
                paletteScrim.style.opacity = 0; paletteScrim.style.visibility = 'hidden'
            }
        }

        paletteOpenBtn.addEventListener('click', openPalette)
        paletteInput.addEventListener('input', () => { selIndex = 0; renderPalette(paletteInput.value) })
        paletteScrim.addEventListener('click', e => { if (e.target === paletteScrim) closePalette() })
        document.addEventListener('keydown', e => {
            if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openPalette(); return }
            if (!paletteScrim.classList.contains('is-open')) return
            if (e.key === 'Escape') closePalette()
            if (e.key === 'ArrowDown') { e.preventDefault(); selIndex++; renderPalette(paletteInput.value) }
            if (e.key === 'ArrowUp') { e.preventDefault(); selIndex--; renderPalette(paletteInput.value) }
            if (e.key === 'Enter') { e.preventDefault(); goToSelected() }
        })
    }

    /* nota: los chips de filtro (.chip[data-filter]) disparan una navegación
       de página completa en cuanto se sueltan (filtros.js hace
       window.location = ... en el mismo click), así que animarlos aquí solo
       lograba un parpadeo a medio jugar antes de que la página se
       descargara — se quitó esa animación por eso */

    /* ── Guardar empleo: un pequeño aplastón, como una pestaña física que
       cede al presionarla, en vez de un cambio de color plano ── */
    if (hasGsap && !reduce) {
        document.querySelectorAll('.btn-favorito').forEach(btn => {
            btn.addEventListener('click', () => {
                gsap.timeline()
                    .to(btn, { scaleX: 1.25, scaleY: .8, duration: .09, ease: 'power1.out' })
                    .to(btn, { scaleX: .92, scaleY: 1.08, duration: .1, ease: 'power1.inOut' })
                    .to(btn, { scaleX: 1, scaleY: 1, duration: .18, ease: 'power2.out' })
            })
        })
    }

    /* ── Anillos de match: relleno con leve sobregiro al entrar en
       viewport — la aguja pasa de largo y se asienta ── */
    if (hasGsap && !reduce) {
        document.querySelectorAll('.job-ring .prog').forEach(circle => {
            const pct = parseFloat(circle.dataset.pct)
            const full = 100.5
            const target = full * (1 - pct / 100)
            gsap.set(circle, { strokeDashoffset: full })
            circle._matchTarget = target
        })
        const rings = Array.from(document.querySelectorAll('.job-ring .prog'))
        if (rings.length) {
            // un ScrollTrigger.batch en vez de un ScrollTrigger.create por
            // anillo — con una página llena de cards, decenas de triggers
            // individuales es justo el tipo de cosa que hace lento el scroll
            if (window.ScrollTrigger) {
                ScrollTrigger.batch(rings, {
                    start: 'top 92%', once: true,
                    onEnter: batch => batch.forEach(c => gsap.to(c, { strokeDashoffset: c._matchTarget, duration: 1.1, ease: 'back.out(1.5)' }))
                })
            } else {
                rings.forEach(c => gsap.to(c, { strokeDashoffset: c._matchTarget, duration: 1.1, ease: 'back.out(1.5)' }))
            }
        }
    }

    /* ── Entrada de las job cards: stagger de traslación + opacity (sin
       rotación — ver nota de costo de composición más abajo), y una
       inclinación al pasar el mouse en vez de un lift plano en CSS. Cada
       bloque va en su propio try/catch — que uno falle no debe apagar los
       de más abajo ── */
    try {
        if (hasGsap && !reduce && cards.length) {
            cards.forEach(card => { card.style.animation = 'none' })
            // sin rotation en la entrada: y + opacity arma solo una traslación
            // simple para componer, en vez de la matriz de transform 2D que
            // pide una rotación — con todas las cards animando en el mismo
            // batch de scroll es la diferencia entre barato y caro
            gsap.set(cards, { y: 22, autoAlpha: 0 })
            if (window.ScrollTrigger) {
                ScrollTrigger.batch(cards, {
                    start: 'top 92%', once: true,
                    onEnter: batch => gsap.to(batch, { y: 0, autoAlpha: 1, duration: .5, ease: 'power3.out', stagger: .08 })
                })
            } else {
                gsap.to(cards, { y: 0, autoAlpha: 1, duration: .5, ease: 'power3.out', stagger: .08 })
            }
            // will-change acotado a la card puntual y solo mientras dura el
            // hover — promoverla a su propia capa de composición justo
            // cuando se anima, sin pagar memoria de GPU en las otras diez
            cards.forEach(card => {
                card.addEventListener('mouseenter', () => {
                    card.style.willChange = 'transform'
                    gsap.to(card, { y: -4, rotation: .35, duration: .25, ease: 'power2.out' })
                })
                card.addEventListener('mouseleave', () => {
                    gsap.to(card, { y: 0, rotation: 0, duration: .3, ease: 'power2.out', onComplete: () => { card.style.willChange = 'auto' } })
                })
            })
        }
    } catch (err) { console.error('job card entrance failed', err) }

    /* ── Avatares de "empresas contratando" se despliegan con el mismo
       ritmo, esperando a que la cortina de arranque termine de subir ── */
    try {
        const hiringAvatars = Array.from(document.querySelectorAll('.ws-hiring-avatars span'))
        if (hasGsap && !reduce && hiringAvatars.length) {
            gsap.set(hiringAvatars, { scale: .5, autoAlpha: 0 })
            const revealHiring = () => gsap.to(hiringAvatars, { scale: 1, autoAlpha: 1, duration: .4, ease: 'back.out(2)', stagger: .05 })
            if (window.ScrollTrigger) {
                ScrollTrigger.create({
                    trigger: '.ws-hiring', start: 'top 90%', once: true,
                    onEnter: () => gsap.delayedCall(bootExtra, revealHiring)
                })
            } else gsap.delayedCall(bootExtra, revealHiring)
        }
    } catch (err) { console.error('hiring avatars reveal failed', err) }

    /* ── El sello de match del perfil aterriza con un golpe firme, sin
       rebote elástico — también espera a que la cortina termine ── */
    try {
        const stamp = document.querySelector('.ws-mini-stamp')
        if (stamp && hasGsap && !reduce) {
            gsap.fromTo(stamp, { scale: 1.22, autoAlpha: 0 }, { scale: 1, autoAlpha: .92, duration: .35, ease: 'power4.out', delay: bootExtra + .15 })
        }
    } catch (err) { console.error('stamp entrance failed', err) }

    /* nota: hubo un parallax de scroll acá (grano/viñeta desplazándose con
       el scroll) — se quitó a propósito. Son elementos position:fixed a
       pantalla completa; scrubear su transform en cada tick de scroll sobre
       toda la altura del documento es justo el tipo de trabajo que hacía
       lag el scroll en páginas largas, por un efecto puramente decorativo */

    /* ── Última card de la grilla, si queda sola en su fila: se estira a
       ancho completo en vez de dejar un hueco vacío al lado. No se puede
       resolver con nth-child a ciegas porque la card destacada (que ocupa
       las 2 columnas) solo aparece en la página 1 y cambia la paridad —
       medir el layout real es lo único que funciona en cualquier página ── */
    try {
        const grid = document.querySelector('.ws-job-grid')
        const gridCards = grid ? Array.from(grid.querySelectorAll('.job-card')) : []
        if (gridCards.length > 1) {
            const last = gridCards[gridCards.length - 1]
            const prev = gridCards[gridCards.length - 2]
            if (last.offsetTop !== prev.offsetTop) last.classList.add('is-solo')
        }
    } catch (err) { console.error('solo card layout check failed', err) }

    /* ── Reloj en vivo de la barra de estado ── */
    const clockEl = document.getElementById('clock')
    if (clockEl) {
        const pad = x => String(x).padStart(2, '0')
        const tick = () => {
            const d = new Date()
            clockEl.textContent = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
        }
        tick(); setInterval(tick, 1000)
    }
})
