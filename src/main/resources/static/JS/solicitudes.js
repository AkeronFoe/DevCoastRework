const filterChips    = Array.from(document.querySelectorAll('.filter-chip[data-filter], .chip-solicitud[data-filter]'))
const rows           = Array.from(document.querySelectorAll('.solicitud-row'))
const invFilterChips = Array.from(document.querySelectorAll('.inv-filter[data-inv-filter]'))
const invCards       = Array.from(document.querySelectorAll('.inv-card'))
const invList        = document.querySelector('.inv-list')
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

/* ── Filtros y búsqueda ───────────────────────────────────── */
function applyRowsVisibility() {
    const query = (buscar?.value || '').toLowerCase().trim()
    rows.forEach(row => {
        const byFilter = activeFilter === 'todas' || row.dataset.estado === activeFilter
        const bySearch  = !query || row.textContent.toLowerCase().includes(query)
        row.style.display = byFilter && bySearch ? '' : 'none'
    })
}

function applyInvVisibility() {
    const query = (buscar?.value || '').toLowerCase().trim()
    invCards.forEach(card => {
        const estado   = card.dataset.estado || ''
        const byFilter = activeInvFilter === 'todas' || estado === activeInvFilter
        const bySearch  = !query || card.textContent.toLowerCase().includes(query)
        card.style.display = byFilter && bySearch ? '' : 'none'
    })
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

function setInvFilter(value) {
    activeInvFilter = value || 'todas'
    invFilterChips.forEach(chip =>
        chip.classList.toggle('active', chip.dataset.invFilter === activeInvFilter))
    applyInvVisibility()
}

filterChips.forEach(chip => {
    chip.addEventListener('click', () => {
        filterChips.forEach(c => c.classList.remove('active'))
        chip.classList.add('active')
        activeFilter = chip.dataset.filter || 'todas'
        applyRowsVisibility()
    })
})

invFilterChips.forEach(chip =>
    chip.addEventListener('click', () => setInvFilter(chip.dataset.invFilter || 'todas')))

if (invSortSelect) invSortSelect.addEventListener('change', sortInvCards)

if (buscar) buscar.addEventListener('input', () => {
    applyRowsVisibility()
    applyInvVisibility()
})

applyRowsVisibility()
setInvFilter(activeInvFilter)
sortInvCards()

/* ── Aceptar / Rechazar via fetch ─────────────────────────── */
const overlay   = document.getElementById('confirm-rechazar-overlay')
const btnNo     = document.getElementById('confirm-rechazar-no')
const btnSi     = document.getElementById('confirm-rechazar-si')
let pendingRejectId = null

function showConfirm(invId) {
    pendingRejectId = invId
    if (overlay) { overlay.style.display = 'flex' }
}
function hideConfirm() {
    pendingRejectId = null
    if (overlay) { overlay.style.display = 'none' }
}

async function invAction(invId, action) {
    try {
        const res  = await fetch(`/invitaciones/${invId}/${action}`, { method: 'POST' })
        if (res.ok || res.redirected) { window.location.reload() }
        else { showToast('Error al procesar la invitación', 'error') }
    } catch { showToast('Error de red. Intenta de nuevo.', 'error') }
}

btnNo?.addEventListener('click', hideConfirm)
overlay?.addEventListener('click', e => { if (e.target === overlay) hideConfirm() })

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
