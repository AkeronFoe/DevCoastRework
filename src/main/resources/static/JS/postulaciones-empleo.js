/* Tracker de candidatos de un empleo: toggle lista/kanban, drag & drop
   entre columnas, selector de fase del proceso — misma lógica que antes,
   reskinada, y usando el toast del propio workspace en vez de uno propio. */
(function () {
  const EMPLEO_ID = window.__PE_EMPLEO_ID__ || ''
  let draggingFrom = null

  function toast(msg, type) {
    if (window.UIFeedback?.toast) window.UIFeedback.toast(msg, type)
  }

  /* ── construir el kanban a partir de las cards de la lista ── */
  function buildKanban() {
    const cards = document.querySelectorAll('#pe-view-lista .pe-card')
    const cols = { revision: [], aprobada: [], rechazada: [] }

    cards.forEach(card => {
      const estado = card.dataset.estado || 'revision'
      if (estado === 'descartado') return
      const nombre = card.dataset.nombre || '—'
      const rol = card.dataset.rol || ''
      const fecha = card.dataset.fecha || ''
      const link = card.dataset.link || '#'
      const solId = card.dataset.solId || ''
      const key = estado === 'aprobada' ? 'aprobada' : estado === 'rechazada' ? 'rechazada' : 'revision'

      const kan = document.createElement('div')
      kan.className = 'pe-kan-card'
      kan.draggable = true
      kan.dataset.solId = solId
      kan.dataset.col = key

      const rolHtml = rol ? `<span class="pe-kan-rol">${rol}</span>` : ''
      kan.innerHTML = `
        <a href="${link}" class="pe-kan-card-inner" draggable="false">
          <div class="pe-kan-avatar">${nombre.charAt(0).toUpperCase()}</div>
          <div class="pe-kan-info"><strong>${nombre}</strong>${rolHtml}<span class="pe-kan-fecha">${fecha}</span></div>
          <svg class="pe-kan-card-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m9 18 6-6-6-6"/></svg>
        </a>
        ${solId ? `<a href="/chat/${solId}" class="pe-kan-chat" title="Chat" draggable="false" onclick="event.stopPropagation()"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></a>` : ''}
      `
      kan.querySelectorAll('*').forEach(el => { el.style.pointerEvents = 'none' })
      kan.querySelector('.pe-kan-card-inner').style.pointerEvents = 'auto'
      const chatLink = kan.querySelector('.pe-kan-chat')
      if (chatLink) chatLink.style.pointerEvents = 'auto'

      kan.addEventListener('dragstart', (e) => {
        draggingFrom = key
        e.dataTransfer.effectAllowed = 'move'
        e.dataTransfer.setData('text/plain', solId)
        e.dataTransfer.setData('solId', solId)
        e.dataTransfer.setData('fromCol', key)
        kan.querySelectorAll('*').forEach(el => { el.style.pointerEvents = 'none' })
        setTimeout(() => kan.classList.add('is-dragging'), 0)
      })
      kan.addEventListener('dragend', () => {
        kan.classList.remove('is-dragging')
        kan.querySelectorAll('*').forEach(el => { el.style.pointerEvents = '' })
        const chat = kan.querySelector('.pe-kan-chat')
        if (chat) chat.style.pointerEvents = 'auto'
        document.querySelectorAll('.pe-col').forEach(c => c.classList.remove('is-drag-over'))
        draggingFrom = null
      })

      cols[key].push(kan)
    })
    ;['revision', 'aprobada', 'rechazada'].forEach(k => {
      const container = document.getElementById('pe-col-' + k)
      const count = document.getElementById('pe-count-' + k)
      if (!container) return
      container.innerHTML = ''
      if (cols[k].length === 0) container.innerHTML = '<p class="pe-kan-empty">Sin candidatos</p>'
      else cols[k].forEach(c => container.appendChild(c))
      if (count) count.textContent = cols[k].length
    })

    document.querySelectorAll('.pe-col').forEach(col => {
      const estado = col.dataset.estado
      col.addEventListener('dragover', (e) => { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; col.classList.add('is-drag-over') })
      col.addEventListener('dragleave', (e) => { if (!col.contains(e.relatedTarget)) col.classList.remove('is-drag-over') })
      col.addEventListener('drop', (e) => {
        e.preventDefault()
        col.classList.remove('is-drag-over')
        const solId = e.dataTransfer.getData('solId') || e.dataTransfer.getData('text/plain')
        const fromCol = e.dataTransfer.getData('fromCol') || draggingFrom
        if (!solId || fromCol === estado) return
        moverCandidato(solId, estado)
      })
    })
  }

  function moverCandidato(solId, nuevoEstado) {
    fetch(`/empleos/${EMPLEO_ID}/postulaciones/${solId}/estado?nuevoEstado=${nuevoEstado}`, {
      method: 'POST', headers: { 'X-Requested-With': 'XMLHttpRequest' }
    }).then(r => r.json()).then(data => {
      if (!data.ok) return
      const card = document.querySelector(`#pe-view-lista .pe-card[data-sol-id="${solId}"]`)
      if (card) {
        card.dataset.estado = nuevoEstado
        card.querySelectorAll('.pe-estado').forEach(b => b.style.display = 'none')
        const map = { revision: 'pe-estado-revision', aprobada: 'pe-estado-aprobada', rechazada: 'pe-estado-rechazada' }
        const badge = card.querySelector('.' + map[nuevoEstado])
        if (badge) badge.style.display = ''
      }
      buildKanban()
      toast(nuevoEstado === 'aprobada' ? '✓ Candidato aceptado' : nuevoEstado === 'rechazada' ? 'Candidato rechazado' : 'Vuelto a revisión', nuevoEstado === 'aprobada' ? 'success' : 'info')
    }).catch(() => toast('Error al actualizar el estado', 'error'))
  }

  /* ── toggle lista / kanban ── */
  window.setView = function (view) {
    const lista = document.getElementById('pe-view-lista')
    const kanban = document.getElementById('pe-view-kanban')
    const btnL = document.getElementById('pe-btn-view-lista')
    const btnK = document.getElementById('pe-btn-view-kanban')
    if (!lista || !kanban) return
    const toKanban = view === 'kanban'
    lista.hidden = toKanban
    kanban.hidden = !toKanban
    btnL?.classList.toggle('is-active', !toKanban)
    btnK?.classList.toggle('is-active', toKanban)
    if (toKanban) buildKanban()
  }

  /* ── selector de fase del proceso ── */
  document.getElementById('pe-fase-selector')?.addEventListener('click', (e) => {
    const btn = e.target.closest('.pe-fase-btn')
    if (!btn) return
    fetch(`/empleos/${EMPLEO_ID}/fase?fase=${btn.dataset.fase}`, {
      method: 'POST', headers: { 'X-Requested-With': 'XMLHttpRequest' }
    }).then(r => r.json()).then(data => {
      if (!data.ok) return
      document.querySelectorAll('.pe-fase-btn').forEach(b => b.classList.remove('is-active'))
      btn.classList.add('is-active')
      toast('Fase actualizada: ' + btn.textContent.trim(), 'success')
    }).catch(() => toast('Error al cambiar la fase', 'error'))
  })
})()
