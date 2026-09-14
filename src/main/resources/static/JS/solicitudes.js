/* el servidor ya manda los dos paneles renderizados, acá solo se
   alterna cuál se ve — el link real sigue apuntando a ?tab=... por si
   alguien lo abre sin JS o en una pestaña nueva */
const tabs         = Array.from(document.querySelectorAll('.ws-tab[data-tab]'))
const panels        = { solicitudes: document.getElementById('panel-solicitudes'), invitaciones: document.getElementById('panel-invitaciones') }

function activateTab(tabName, pushState) {
    if (!panels[tabName]) return
    tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === tabName))
    Object.entries(panels).forEach(([name, panel]) => { if (panel) panel.hidden = name !== tabName })
    if (pushState) {
        const url = new URL(window.location.href)
        url.searchParams.set('tab', tabName)
        history.pushState({ tab: tabName }, '', url)
    }
}

tabs.forEach(tab => {
    tab.addEventListener('click', e => {
        e.preventDefault()
        activateTab(tab.dataset.tab, true)
    })
})

window.addEventListener('popstate', () => {
    const tabName = new URLSearchParams(window.location.search).get('tab') || 'solicitudes'
    activateTab(tabName, false)
})

/* ── Filtros y búsqueda ── */
// se buscan por atributo [data-filter], no por clase — así el estilo de
// los chips se puede cambiar sin romper el filtro de nuevo
const filterChips    = Array.from(document.querySelectorAll('[data-filter]'))
const rows           = Array.from(document.querySelectorAll('.solicitud-row'))
const solEmptyFilter = document.getElementById('sol-filter-empty')
const solTableWrap   = document.getElementById('sol-table-wrap')

const invFilterChips = Array.from(document.querySelectorAll('[data-inv-filter]'))
const invCards       = Array.from(document.querySelectorAll('.inv-card'))
const invList        = document.querySelector('.inv-list')
const invEmptyFilter = document.getElementById('inv-filter-empty')
const invSortSelect  = document.getElementById('inv-sort-select')
const buscar         = document.getElementById('buscar-solicitud')

let activeFilter    = (filterChips.find(c => c.classList.contains('active'))?.dataset.filter)    || 'todas'
let activeInvFilter = (invFilterChips.find(c => c.classList.contains('active'))?.dataset.invFilter) || 'todas'

/* ── Fechas relativas ─────────────────────────────────────── */
function toRelDate(isoStr) {
    if (!isoStr) return null
    const d = Date.now() - new Date(isoStr).getTime()
    if (isNaN(d)) return null
    if (d < 3_600_000)           return 'Hace menos de 1h'
    if (d < 86_400_000)          return `Hace ${Math.floor(d / 3_600_000)}h`
    if (d < 2 * 86_400_000)      return 'Ayer'
    if (d < 7 * 86_400_000)      return `Hace ${Math.floor(d / 86_400_000)} días`
    if (d < 30 * 86_400_000)     return `Hace ${Math.floor(d / (7 * 86_400_000))} sem.`
    if (d < 365 * 86_400_000)    return `Hace ${Math.floor(d / (30 * 86_400_000))} meses`
    return 'Hace más de 1 año'
}

document.querySelectorAll('.inv-rel-date[data-fecha-iso]').forEach(span => {
    const rel = toRelDate(span.dataset.fechaIso)
    if (rel) {
        const texto = span.querySelector('.inv-hora-texto')
        if (texto) texto.textContent = rel
    }
})

function applyRowsVisibility() {
    const query = (buscar?.value || '').toLowerCase().trim()
    let visible = 0
    rows.forEach(row => {
        const byFilter = activeFilter === 'todas' || row.dataset.estado === activeFilter
        const bySearch  = !query || row.textContent.toLowerCase().includes(query)
        const show = byFilter && bySearch
        row.style.display = show ? '' : 'none'
        if (show) visible++
    })
    if (solEmptyFilter) solEmptyFilter.hidden = visible !== 0
    if (solTableWrap)   solTableWrap.hidden   = visible === 0 && rows.length > 0
}

function applyInvVisibility() {
    const query = (buscar?.value || '').toLowerCase().trim()
    let visible = 0
    invCards.forEach(card => {
        const estado   = card.dataset.estado || ''
        const byFilter = activeInvFilter === 'todas' || estado === activeInvFilter
        const bySearch  = !query || card.textContent.toLowerCase().includes(query)
        const show = byFilter && bySearch
        card.style.display = show ? '' : 'none'
        if (show) visible++
    })
    if (invEmptyFilter) invEmptyFilter.hidden = visible !== 0
    if (invList)         invList.hidden        = visible === 0 && invCards.length > 0
}

function getInvTime(card) {
    const raw  = card.dataset.date || ''
    if (!raw) return 0
    const time = new Date(raw).getTime()
    return isNaN(time) ? 0 : time
}

function sortInvCards() {
    if (!invList || invCards.length === 0) return
    const order  = invSortSelect?.value || 'recent'
    const sorted = [...invCards].sort((a, b) => {
        const diff = getInvTime(a) - getInvTime(b)
        return order === 'oldest' ? diff : -diff
    })
    sorted.forEach(card => invList.appendChild(card))
}

function setFilter(chip) {
    filterChips.forEach(c => c.classList.remove('active'))
    chip.classList.add('active')
    activeFilter = chip.dataset.filter || 'todas'
    applyRowsVisibility()
}

function setInvFilter(chip) {
    invFilterChips.forEach(c => c.classList.remove('active'))
    chip.classList.add('active')
    activeInvFilter = chip.dataset.invFilter || 'todas'
    applyInvVisibility()
}

filterChips.forEach(chip => chip.addEventListener('click', () => setFilter(chip)))
invFilterChips.forEach(chip => chip.addEventListener('click', () => setInvFilter(chip)))

if (invSortSelect) invSortSelect.addEventListener('change', sortInvCards)

if (buscar) buscar.addEventListener('input', () => {
    applyRowsVisibility()
    applyInvVisibility()
})

applyRowsVisibility()
applyInvVisibility()
sortInvCards()

/* ── Aceptar / Rechazar via fetch ─────────────────────────── */
const overlay   = document.getElementById('confirm-rechazar-overlay')
const btnNo     = document.getElementById('confirm-rechazar-no')
const btnSi     = document.getElementById('confirm-rechazar-si')
let pendingRejectId = null

function showConfirm(invId) {
    pendingRejectId = invId
    overlay?.classList.add('is-open')
}
function hideConfirm() {
    pendingRejectId = null
    overlay?.classList.remove('is-open')
}

// al aceptar/rechazar ya no recargamos la página: actualiza la card,
// los contadores y el badge del tab directo acá
function bumpStat(id, delta) {
    const el = document.getElementById(id)
    if (!el) return
    el.textContent = String(Math.max(0, parseInt(el.textContent, 10) + delta))
}

function applyInvitationOutcome(card, nuevoEstado) {
    const estadoAnterior = card.dataset.estado
    card.dataset.estado = nuevoEstado

    const badge = card.querySelector('.inv-estado-badge')
    if (badge) {
        badge.classList.remove('pendiente', 'aceptada', 'rechazada')
        badge.classList.add(nuevoEstado)
        badge.textContent = nuevoEstado === 'aceptada' ? 'Aceptada' : 'Rechazada'
    }

    const footerRight = card.querySelector('.inv-footer-right')
    if (footerRight) {
        footerRight.innerHTML = nuevoEstado === 'aceptada'
            ? `<span class="inv-estado-final aceptada">
                 <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m9 12 2 2 4-4"/><circle cx="12" cy="12" r="10"/></svg>
                 Aceptaste — la empresa puede contactarte.
               </span>`
            : `<span class="inv-estado-final rechazada">
                 <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>
                 Invitación rechazada.
               </span>`
    }

    if (estadoAnterior === 'pendiente') {
        bumpStat('inv-stat-pendientes', -1)
        bumpStat(nuevoEstado === 'aceptada' ? 'inv-stat-aceptadas' : 'inv-stat-rechazadas', 1)
        const tabBadge = document.getElementById('tab-badge-invitaciones')
        const pendientesRestantes = parseInt(document.getElementById('inv-stat-pendientes')?.textContent || '0', 10)
        if (tabBadge) {
            tabBadge.classList.toggle('is-alert', pendientesRestantes > 0)
            tabBadge.textContent = String(pendientesRestantes > 0 ? pendientesRestantes : invCards.length)
        }
    }

    applyInvVisibility()
}

async function invAction(invId, action) {
    const btn = document.querySelector(`[data-inv-id="${invId}"]`)
    const card = btn?.closest('.inv-card')
    try {
        const res = await fetch(`/invitaciones/${invId}/${action}`, { method: 'POST' })
        if (res.ok || res.redirected) {
            if (card) {
                applyInvitationOutcome(card, action === 'aceptar' ? 'aceptada' : 'rechazada')
                showToast(action === 'aceptar' ? 'Invitación aceptada' : 'Invitación rechazada', 'ok')
            } else {
                window.location.reload()
            }
        } else {
            showToast('Error al procesar la invitación', 'error')
        }
    } catch {
        showToast('Error de red. Intenta de nuevo.', 'error')
    }
}

btnNo?.addEventListener('click', hideConfirm)
overlay?.addEventListener('click', e => { if (e.target === overlay) hideConfirm() })
document.addEventListener('keydown', e => { if (e.key === 'Escape' && overlay?.classList.contains('is-open')) hideConfirm() })

btnSi?.addEventListener('click', async () => {
    if (!pendingRejectId) return
    const id = pendingRejectId
    hideConfirm()
    await invAction(id, 'rechazar')
})

document.querySelectorAll('.btn-rechazar-inv').forEach(btn => {
    btn.addEventListener('click', () => showConfirm(btn.dataset.invId))
})

document.querySelectorAll('.btn-aceptar-inv').forEach(btn => {
    btn.addEventListener('click', async () => {
        btn.disabled = true
        btn.textContent = 'Aceptando...'
        await invAction(btn.dataset.invId, 'aceptar')
    })
})

/* ── Copiar enlace ────────────────────────────────────────── */
const copyButtons = Array.from(document.querySelectorAll('.inv-copy-btn[data-copy-url]'))

const copyText = async text => {
    if (navigator?.clipboard?.writeText) {
        try { await navigator.clipboard.writeText(text); return true } catch { /**/ }
    }
    const ta = document.createElement('textarea')
    ta.value = text; ta.setAttribute('readonly', '')
    ta.style.position = 'absolute'; ta.style.left = '-9999px'
    document.body.appendChild(ta); ta.select()
    let ok = false
    try { ok = document.execCommand('copy') } catch { /**/ }
    document.body.removeChild(ta); return ok
}

copyButtons.forEach(button => {
    const label = button.querySelector('.inv-copy-label')
    const def   = label?.textContent || ''
    button.addEventListener('click', async e => {
        e.preventDefault(); e.stopPropagation()
        const url = button.dataset.copyUrl
        if (!url) return
        const text   = url.startsWith('http') ? url : `${window.location.origin}${url}`
        const copied = await copyText(text)
        if (!copied) return
        button.classList.add('is-copied')
        if (label) label.textContent = 'Copiado'
        setTimeout(() => { button.classList.remove('is-copied'); if (label) label.textContent = def }, 1400)
    })
})

/* ── showToast fallback si ui-feedback no está ────────────── */
function showToast(msg, type) {
    if (window.UIFeedback?.toast) { window.UIFeedback.toast(msg, type); return }
    console.warn('[Toast]', msg)
}
