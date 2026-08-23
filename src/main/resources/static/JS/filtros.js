const filtrosActivos = new Set()

const categorias = new Set(['Programacion', 'Diseño', 'Frontend', 'Backend'])
const modalidades = new Set(['Presencial', 'Remoto', 'Hibrido'])
const seniorities = new Set(['Senior', 'Junior'])
const contratos = new Set(['Tiempo completo', 'Medio tiempo', 'Freelance'])
const regiones = new Set(['Latam', 'Europa'])

function buildParamsFromActive() {
    const params = new URLSearchParams()
    const searchInput = document.getElementById('global-search-input')
    if (searchInput && searchInput.value.trim()) params.set('q', searchInput.value.trim())

    const extras = []
    for (const f of filtrosActivos) {
        if (categorias.has(f) && !params.has('categoria')) params.set('categoria', f)
        else if (modalidades.has(f) && !params.has('modalidad')) params.set('modalidad', f)
        else if (seniorities.has(f) && !params.has('seniority')) params.set('seniority', f)
        else if (contratos.has(f) || regiones.has(f)) extras.push(f)
        else extras.push(f)
    }

    if (extras.length) params.set('extras', extras.join(','))

    params.set('page', '0')
    const sizeSelect = document.getElementById('page-size')
    if (sizeSelect && sizeSelect.value) params.set('size', sizeSelect.value)

    const sortSelect = document.getElementById('empleo-sort-select')
    if (sortSelect && sortSelect.value) params.set('sort', sortSelect.value)

    return params
}

function applyFiltersToUrl() {
    const params = buildParamsFromActive()
    const qs = params.toString()
    const url = qs ? ('/empleos?' + qs) : '/empleos'
    window.location = url
}

function actualizarContadorDropdown() {
    const btn = document.getElementById('btn-more')
    if (!btn) return
    const count = Array.from(filtrosActivos).filter(f =>
        document.querySelector(`#dropdown .chip[data-filter="${f}"]`)
    ).length
    let badge = btn.querySelector('.drop-count-badge')
    if (!badge) {
        badge = document.createElement('span')
        badge.className = 'drop-count-badge'
        badge.style.cssText = 'background:var(--accent);color:#000;border-radius:9999px;padding:0 6px;font-size:.68rem;font-weight:700;margin-left:3px;vertical-align:middle;line-height:1.7;display:none;'
        const arrow = btn.querySelector('.arrow')
        if (arrow) btn.insertBefore(badge, arrow)
        else btn.appendChild(badge)
    }
    badge.textContent = count
    badge.style.display = count > 0 ? 'inline-block' : 'none'
}

function actualizarFiltrosActivos() {
    const row = document.getElementById('filtros-activos-row')
    if (!row) return
    row.innerHTML = ''

    filtrosActivos.forEach(filtro => {
        const esDesdeDropdown = document.querySelector(`#dropdown .chip[data-filter="${filtro}"]`)
        if (!esDesdeDropdown) return

        const label = typeof window.translateText === 'function' ? window.translateText(filtro) : filtro

        const chip = document.createElement('span')
        chip.className = 'chip active'
        chip.innerHTML = `${label} <span style="margin-left:4px;cursor:pointer;font-size:14px;line-height:1;" data-quitar="${filtro}">×</span>`
        row.appendChild(chip)
    })

    actualizarContadorDropdown()
}

window.addEventListener('DOMContentLoaded', () => {
    const filtrosActivosRow = document.getElementById('filtros-activos-row')
    if (filtrosActivosRow) {
        filtrosActivosRow.addEventListener('click', (e) => {
            const quitar = e.target.dataset.quitar
            if (!quitar) return
            filtrosActivos.delete(quitar)
            document.querySelector(`.chip[data-filter="${quitar}"]`)?.classList.remove('active')
            if (filtrosActivos.size === 0) {
                document.querySelector('.chip[data-filter="todos"]')?.classList.add('active')
            }
            actualizarFiltrosActivos()
            applyFiltersToUrl()
        })
    }

    document.querySelectorAll('.chip[data-filter]').forEach(chip => {
        chip.addEventListener('click', () => {
            const filtro = chip.dataset.filter

            if (filtro === 'todos') {
                filtrosActivos.clear()
                document.querySelectorAll('.chip[data-filter]').forEach(c => c.classList.remove('active'))
                chip.classList.add('active')
            } else {
                document.querySelector('.chip[data-filter="todos"]')?.classList.remove('active')

                if (filtrosActivos.has(filtro)) {
                    filtrosActivos.delete(filtro)
                    chip.classList.remove('active')
                } else {
                    filtrosActivos.add(filtro)
                    chip.classList.add('active')
                }

                if (filtrosActivos.size === 0) {
                    document.querySelector('.chip[data-filter="todos"]')?.classList.add('active')
                }
            }

            actualizarFiltrosActivos()
            applyFiltersToUrl()
        })
    })

    const btn = document.getElementById('btn-more')
    const dropdown = document.getElementById('dropdown')

    if (btn && dropdown) {
        btn.addEventListener('click', (e) => {
            e.stopPropagation()
            e.preventDefault()
            const isOpen = dropdown.classList.contains('open')
            dropdown.classList.toggle('open', !isOpen)
            btn.classList.toggle('open', !isOpen)
        })

        document.addEventListener('click', (e) => {
            const clickDentroBtn = btn.contains(e.target)
            const clickDentroDropdown = dropdown.contains(e.target)
            if (!clickDentroBtn && !clickDentroDropdown) {
                dropdown.classList.remove('open')
                btn.classList.remove('open')
            }
        })
    }

    const params = new URLSearchParams(window.location.search)
    const q = params.get('q')
    const categoria = params.get('categoria')
    const modalidad = params.get('modalidad')
    const seniority = params.get('seniority')
    const extras = params.get('extras')

    if (q) {
        const input = document.getElementById('global-search-input')
        if (input) input.value = q
    }

    if (categoria) {
        const el = document.querySelector(`.chip[data-filter="${categoria}"]`)
        if (el) { el.classList.add('active'); filtrosActivos.add(categoria) }
    }
    if (modalidad) {
        const el = document.querySelector(`.chip[data-filter="${modalidad}"]`)
        if (el) { el.classList.add('active'); filtrosActivos.add(modalidad) }
    }
    if (seniority) {
        const el = document.querySelector(`.chip[data-filter="${seniority}"]`)
        if (el) { el.classList.add('active'); filtrosActivos.add(seniority) }
    }
    if (extras) {
        extras.split(',').forEach(x => {
            const el = document.querySelector(`.chip[data-filter="${x}"]`)
            if (el) { el.classList.add('active'); filtrosActivos.add(x) }
        })
    }

    if (filtrosActivos.size > 0) {
        document.querySelector('.chip[data-filter="todos"]')?.classList.remove('active')
    }

    actualizarFiltrosActivos()

    const sortEl = document.getElementById('empleo-sort-select')
    const sortParam = params.get('sort')
    if (sortEl && sortParam) sortEl.value = sortParam
    if (sortEl) sortEl.addEventListener('change', applyFiltersToUrl)

    const globalSearch = document.getElementById('global-search-input')
    if (globalSearch) {
        globalSearch.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault()
                actualizarFiltrosActivos()
                applyFiltersToUrl()
            }
        })
    }
})
