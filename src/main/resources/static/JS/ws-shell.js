/* chrome compartido del workspace (topbar, sidebar, paleta de comandos,
   overlay de arranque). Lo saqué de empleos.js/proyectos-vitrina.js,
   que lo tenían duplicado, para que las páginas nuevas no lo repitan */
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
    // busco el .ws-content dentro del mismo .ws-main que el gutter, no
    // uno cualquiera con querySelector — así no se confunde si hay otro
    // .ws-content suelto en un modal
    const gutterEl = document.getElementById('gutterLines')
    const contentEl = gutterEl?.closest('.ws-main')?.querySelector('.ws-content') || document.querySelector('.ws-content')
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
            // los resultados no son focusables (se navegan con flechas), así
            // que sin esto Tab se escapa del modal hacia el fondo
            else if (e.key === 'Tab') { e.preventDefault(); paletteInput.focus() }
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
})
