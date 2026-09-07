document.addEventListener('DOMContentLoaded', () => {

    const reduce = !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches)
    const hasGsap = typeof window.gsap !== 'undefined'

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

    /* ── Botón volver arriba ── */
    const btt = document.getElementById('back-to-top')
    if (btt) {
        window.addEventListener('scroll', () => {
            btt.classList.toggle('is-visible', window.scrollY > 300)
        }, { passive: true })
        btt.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }))
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

    /* ── Numeración del gutter, como el margen de un editor de código ── */
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
        if (typeof ResizeObserver !== 'undefined') new ResizeObserver(scheduleUpdateGutter).observe(contentEl)
        else window.addEventListener('resize', scheduleUpdateGutter)
    }

    /* ── Overlay de arranque ── */
    const boot = document.getElementById('wsBoot')
    if (boot) {
        if (reduce || !hasGsap) {
            boot.classList.add('is-done')
            boot.style.opacity = '0'; boot.style.visibility = 'hidden'
        } else {
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
    }

    /* ── Paleta de comandos (⌘K) — lee los enlaces del sidebar ── */
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
        })).filter(i => i.label)

        let selIndex = 0
        let filtered = paletteItems

        function renderPalette(q) {
            const query = (q || '').toLowerCase().trim()
            filtered = query ? paletteItems.filter(i => i.label.toLowerCase().includes(query)) : paletteItems
            selIndex = 0
            if (!filtered.length) {
                paletteList.innerHTML = '<div class="ws-palette-empty">Sin coincidencias</div>'
                return
            }
            paletteList.innerHTML = filtered.map((i, idx) =>
                `<div class="ws-palette-item${idx === 0 ? ' is-sel' : ''}" data-idx="${idx}"><span>${i.label}</span><span class="ext">${i.ext}</span></div>`
            ).join('')
        }

        function go(idx) {
            const item = filtered[idx]
            if (item && item.href) window.location.href = item.href
        }

        function openPalette() {
            paletteScrim.classList.add('is-open'); selIndex = 0; paletteInput.value = ''; renderPalette('')
            if (hasGsap && !reduce) {
                gsap.killTweensOf([paletteScrim, paletteBox])
                gsap.set(paletteScrim, { autoAlpha: 1 })
                gsap.fromTo(paletteBox, { y: -14, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: .25, ease: 'power2.out' })
            } else {
                paletteScrim.style.opacity = 1; paletteScrim.style.visibility = 'visible'
            }
            setTimeout(() => paletteInput.focus(), 10)
        }
        function closePalette() {
            if (hasGsap && !reduce) {
                gsap.killTweensOf([paletteScrim, paletteBox])
                gsap.to(paletteScrim, {
                    autoAlpha: 0, duration: .18, ease: 'power1.in',
                    onComplete: () => paletteScrim.classList.remove('is-open')
                })
            } else {
                paletteScrim.classList.remove('is-open')
                paletteScrim.style.opacity = 0; paletteScrim.style.visibility = 'hidden'
            }
            paletteOpenBtn.focus()
        }

        paletteOpenBtn.addEventListener('click', openPalette)
        paletteScrim.addEventListener('click', e => { if (e.target === paletteScrim) closePalette() })
        document.addEventListener('keydown', e => {
            if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); openPalette() }
            if (!paletteScrim.classList.contains('is-open')) return
            if (e.key === 'Escape') closePalette()
            else if (e.key === 'ArrowDown') { e.preventDefault(); selIndex = Math.min(selIndex + 1, filtered.length - 1); highlight() }
            else if (e.key === 'ArrowUp') { e.preventDefault(); selIndex = Math.max(selIndex - 1, 0); highlight() }
            else if (e.key === 'Enter') { e.preventDefault(); go(selIndex) }
        })
        function highlight() {
            paletteList.querySelectorAll('.ws-palette-item').forEach((el, idx) => el.classList.toggle('is-sel', idx === selIndex))
            paletteList.querySelector('.is-sel')?.scrollIntoView({ block: 'nearest' })
        }
        paletteInput.addEventListener('input', () => renderPalette(paletteInput.value))
        paletteList.addEventListener('click', e => {
            const item = e.target.closest('.ws-palette-item')
            if (item) go(parseInt(item.getAttribute('data-idx'), 10))
        })
    }

    /* ── Dropdown "más filtros" (los chips ya son links reales — esto solo
       abre/cierra el panel) ── */
    const moreBtn = document.getElementById('btn-more')
    const moreDropdown = document.getElementById('dropdown')
    if (moreBtn && moreDropdown) {
        moreBtn.addEventListener('click', e => {
            e.stopPropagation()
            const isOpen = moreDropdown.classList.contains('open')
            moreDropdown.classList.toggle('open', !isOpen)
            moreBtn.classList.toggle('open', !isOpen)
        })
        document.addEventListener('click', e => {
            if (!moreBtn.contains(e.target) && !moreDropdown.contains(e.target)) {
                moreDropdown.classList.remove('open')
                moreBtn.classList.remove('open')
            }
        })
    }

    /* ================================================================
       MOTOR DE PRESENTACIÓN — convierte los datos reales de cada
       proyecto (categoria, stack, bannerClase) en el canvas "código" o
       "diseño". Vive en proyectos-render.js (window.ProyectosRender),
       compartido con la vista previa en vivo de subirProyecto — así la
       vitrina y el formulario siempre muestran el mismo componente. */
    const PR = window.ProyectosRender

    const cards = Array.from(document.querySelectorAll('.proj-card'))
    cards.forEach(card => {
        const accent = card.dataset.color || '#4fa0e8'
        const ink = PR.inkFor(accent)
        card.style.setProperty('--card-accent', accent)
        card.style.setProperty('--card-ink', ink)

        const seed = PR.hashStr(card.dataset.id || card.dataset.name || 'x')
        const stack = (card.dataset.stack || '').split(',').map(s => s.trim()).filter(Boolean)
        const built = PR.buildCanvas({
            nombre: card.dataset.name, categoria: card.dataset.categoria, tipo: card.dataset.tipo,
            stack, accent, seed, estilo: card.dataset.banner
        })

        const canvas = card.querySelector('.proj-canvas')
        if (canvas) {
            canvas.classList.add(built.modeClass)
            canvas.innerHTML = built.bar + built.stage
        }
        const avatar = card.querySelector('.proj-avatar')
        if (avatar && built.shapeClass) avatar.classList.add(built.shapeClass)

        const title = card.querySelector('.proj-title')
        if (title && !title.querySelector('.proj-mode-badge')) {
            const badge = document.createElement('span')
            badge.className = 'proj-title-disc proj-mode-badge ' + built.modeClass.replace('mode-', 'is-')
            badge.textContent = built.discLabel
            title.appendChild(badge)
        }

        // swatches: 3 variantes reales del único color guardado del proyecto,
        // no una paleta de 4 tonos inventada
        const swatchesEl = card.querySelector('.proj-swatches')
        if (swatchesEl) swatchesEl.innerHTML = PR.buildSwatchesHTML(accent)
    })

    /* ================= entrada + tilt suave de las cards ================= */
    const grid = document.getElementById('proyectos-grid')
    function recomputeSolo() {
        cards.forEach(c => c.classList.remove('is-solo'))
        if (cards.length > 1) {
            const last = cards[cards.length - 1]
            const prev = cards[cards.length - 2]
            requestAnimationFrame(() => { if (last.offsetTop !== prev.offsetTop) last.classList.add('is-solo') })
        }
    }
    if (hasGsap && !reduce && cards.length) {
        gsap.set(cards, { y: 18, autoAlpha: 0 })
        gsap.to(cards, { y: 0, autoAlpha: 1, duration: .5, ease: 'power3.out', stagger: .06 })
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
    recomputeSolo()

    /* ================= panel de vista rápida ================= */
    const qvScrim = document.getElementById('qvScrim')
    if (qvScrim) {
        const qvPanel = qvScrim.querySelector('.qv-panel')
        const qvCanvasCol = document.getElementById('qvCanvasCol')
        const qvTitle = document.getElementById('qvTitle')
        const qvMeta = document.getElementById('qvMeta')
        const qvDesc = document.getElementById('qvDesc')
        const qvSwatches = document.getElementById('qvSwatches')
        const qvTags = document.getElementById('qvTags')
        const qvSpecs = document.getElementById('qvSpecs')
        const qvNote = document.getElementById('qvNote')

        function openQuickView(card) {
            const accent = card.style.getPropertyValue('--card-accent')
            const ink = card.style.getPropertyValue('--card-ink')
            qvPanel.style.setProperty('--card-accent', accent)
            qvCanvasCol.innerHTML = ''
            const canvasClone = card.querySelector('.proj-canvas')?.cloneNode(true)
            if (canvasClone) {
                canvasClone.style.setProperty('--card-accent', accent)
                canvasClone.style.setProperty('--card-ink', ink)
                qvCanvasCol.appendChild(canvasClone)
            }

            qvTitle.textContent = card.dataset.name || ''
            card.querySelectorAll('.proj-title-disc').forEach(b => qvTitle.appendChild(b.cloneNode(true)))

            // se clona el contenido ya renderizado (y ya escapado por el
            // servidor) en vez de reconstruirlo desde los data-* crudos —
            // así no hay que volver a sanitizar texto de usuario acá
            qvMeta.innerHTML = card.querySelector('.proj-meta-line')?.innerHTML || ''
            qvTags.innerHTML = card.querySelector('.proj-tags')?.innerHTML || ''
            qvDesc.textContent = card.dataset.descExt || card.querySelector('.proj-tagline')?.textContent || ''

            const tones = [shade(accent, -0.3), accent, shade(accent, 0.35)]
            qvSwatches.innerHTML = tones.map(hex =>
                `<div class="qv-swatch"><div class="qv-swatch-chip" style="background:${hex}"></div><span class="qv-swatch-hex">${hex}</span></div>`
            ).join('')

            const specs = [
                [card.dataset.categoria || '—', 'Disciplina'],
                [card.dataset.tipo || '—', 'Tipo'],
                [card.dataset.anio || '—', 'Año'],
                [card.dataset.duracion || '—', 'Duración']
            ]
            qvSpecs.innerHTML = specs.map(([value, label]) =>
                `<div class="qv-spec-item"><span class="qv-spec-value">${escapeHtml(value)}</span><span class="qv-spec-label">${label}</span></div>`
            ).join('')

            // el link, si existe, se copia del <a> real que ya renderizó y
            // validó el servidor (solo http/https) — nunca se arma a mano
            // desde el dataset crudo
            const realLink = card.querySelector('.proj-actions a[href]')
            if (qvNote) {
                if (realLink) {
                    qvNote.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>`
                    const a = document.createElement('a')
                    a.href = realLink.href
                    a.target = '_blank'
                    a.rel = 'noopener'
                    a.textContent = 'Abrir el proyecto en una pestaña nueva'
                    a.style.color = 'inherit'
                    a.style.fontWeight = '700'
                    qvNote.appendChild(a)
                    qvNote.hidden = false
                } else {
                    qvNote.hidden = true
                }
            }

            qvScrim.classList.add('is-open')
            document.body.style.overflow = 'hidden'
            if (hasGsap && !reduce) {
                gsap.killTweensOf([qvScrim, qvPanel])
                gsap.set(qvScrim, { autoAlpha: 1 })
                gsap.set(qvPanel, { y: 24, scale: .96, autoAlpha: 0 })
                requestAnimationFrame(() => {
                    gsap.to(qvPanel, { y: 0, scale: 1, autoAlpha: 1, duration: .32, ease: 'power2.out' })
                })
            } else {
                qvScrim.style.opacity = 1; qvScrim.style.visibility = 'visible'
            }
        }

        function closeQuickView() {
            document.body.style.overflow = ''
            if (hasGsap && !reduce) {
                gsap.killTweensOf([qvScrim, qvPanel])
                gsap.to(qvScrim, { autoAlpha: 0, duration: .2, ease: 'power1.in', onComplete: () => qvScrim.classList.remove('is-open') })
            } else {
                qvScrim.classList.remove('is-open')
                qvScrim.style.opacity = 0; qvScrim.style.visibility = 'hidden'
            }
        }

        cards.forEach(card => {
            card.addEventListener('click', e => {
                if (e.target.closest('button, a')) return
                openQuickView(card)
            })
            card.querySelector('.proj-qv-btn')?.addEventListener('click', e => {
                e.stopPropagation()
                openQuickView(card)
            })
        })

        qvScrim.querySelector('.qv-close')?.addEventListener('click', closeQuickView)
        qvScrim.addEventListener('click', e => { if (e.target === qvScrim) closeQuickView() })
        document.addEventListener('keydown', e => { if (e.key === 'Escape' && qvScrim.classList.contains('is-open')) closeQuickView() })
    }
})
