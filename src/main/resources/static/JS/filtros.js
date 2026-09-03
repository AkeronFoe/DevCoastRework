const filtrosActivos = new Set()

const categorias = new Set(['Programacion', 'Diseño', 'Frontend', 'Backend'])
const modalidades = new Set(['Presencial', 'Remoto', 'Hibrido'])
const seniorities = new Set(['Senior', 'Junior'])
const contratos = new Set(['Tiempo completo', 'Medio tiempo', 'Freelance'])
const regiones = new Set(['Latam', 'Europa'])

// el backend solo acepta UN valor para categoria/modalidad/seniority — estos
// tres grupos son mutuamente excluyentes entre sí (elegir uno desactiva
// cualquier otro del mismo grupo). contrato/región sí se combinan libremente
// como "extras", por eso no entran aquí.
const gruposExclusivos = [categorias, modalidades, seniorities]
function grupoDe(filtro) {
    return gruposExclusivos.find(g => g.has(filtro))
}

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

    const favToggle = document.getElementById('fav-only-toggle')
    if (favToggle && favToggle.classList.contains('active')) params.set('favoritos', 'true')

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

    // el dropdown de "más filtros" queda abierto tapando parte de la
    // grilla si no se cierra a mano — con el reload completo de antes
    // esto no hacía falta, se resolvía gratis
    document.getElementById('dropdown')?.classList.remove('open')
    document.getElementById('btn-more')?.classList.remove('open')

    // "limpiar filtros" vive fuera del fragmento que trae dcLoadResultados,
    // así que no se re-renderiza solo — pero el cliente ya sabe si hay
    // algún filtro activo (es lo mismo que acaba de calcular arriba), no
    // hace falta esperar la respuesta del servidor para decidir esto
    const hayFiltros = ['q', 'categoria', 'modalidad', 'seniority', 'extras', 'favoritos']
        .some(k => params.has(k))
    document.querySelector('.ws-clear-filters')?.classList.toggle('is-hidden', !hayFiltros)

    if (typeof window.dcLoadResultados === 'function') {
        window.dcLoadResultados(url, { pushState: true })
    } else {
        window.location = url
    }
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

// Reconstruye el estado de chips/búsqueda/page-size/favoritos a partir de
// location.search — se usa en la carga inicial Y en cada `popstate` (atrás/
// adelante del navegador tras navegar por fetch). Idempotente: limpia todo
// antes de reconstruir, así se puede llamar más de una vez por carga de
// página. El sort-select NO se toca acá: vive dentro del fragmento que trae
// dcLoadResultados y llega siempre con el th:selected correcto del server.
function restoreFiltersUI() {
    filtrosActivos.clear()
    document.querySelectorAll('.chip[data-filter]').forEach(c => c.classList.remove('active'))

    const params = new URLSearchParams(window.location.search)
    const q = params.get('q')
    const categoria = params.get('categoria')
    const modalidad = params.get('modalidad')
    const seniority = params.get('seniority')
    const extras = params.get('extras')

    const input = document.getElementById('global-search-input')
    if (input) input.value = q || ''

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

    document.querySelector('.chip[data-filter="todos"]')?.classList.toggle('active', filtrosActivos.size === 0)
    actualizarFiltrosActivos()

    // page-size y favoritos-toggle hoy dependen 100% de SSR (th:selected /
    // th:classappend) — en un popstate no hay render fresco del server para
    // el chrome de filtros (solo para el fragmento de resultados), así que
    // acá sí hace falta restaurarlos a mano
    const sizeEl = document.getElementById('page-size')
    const sizeParam = params.get('size')
    if (sizeEl && sizeParam) sizeEl.value = sizeParam

    const favToggle = document.getElementById('fav-only-toggle')
    if (favToggle) {
        const isFav = params.get('favoritos') === 'true'
        favToggle.classList.toggle('active', isFav)
        favToggle.setAttribute('aria-pressed', isFav ? 'true' : 'false')
    }

    const hayFiltros = ['q', 'categoria', 'modalidad', 'seniority', 'extras', 'favoritos']
        .some(k => params.has(k) && params.get(k))
    document.querySelector('.ws-clear-filters')?.classList.toggle('is-hidden', !hayFiltros)
}
window.restoreFiltersUI = restoreFiltersUI

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
                    // dentro del mismo grupo (categoria/modalidad/seniority) solo uno
                    // puede estar activo a la vez — si no, el segundo que elijas se
                    // manda como "extras" al servidor y no filtra nada, dando la
                    // sensación de que "a veces los filtros no funcionan"
                    const grupo = grupoDe(filtro)
                    if (grupo) {
                        grupo.forEach(otro => {
                            if (otro !== filtro && filtrosActivos.has(otro)) {
                                filtrosActivos.delete(otro)
                                document.querySelectorAll(`.chip[data-filter="${otro}"]`)
                                    .forEach(c => c.classList.remove('active'))
                            }
                        })
                    }
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

    restoreFiltersUI()

    // el sort-select vive DENTRO del fragmento que reemplaza dcLoadResultados
    // (innerHTML completo) — un bind directo sobre el nodo capturado acá
    // se pierde en cuanto se pagina o filtra una vez. Delegado en document,
    // sobrevive cualquier cantidad de swaps sin tener que re-invocar nada
    document.addEventListener('change', (e) => {
        if (e.target.id === 'empleo-sort-select') applyFiltersToUrl()
    })
    document.getElementById('page-size')?.addEventListener('change', applyFiltersToUrl)

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
