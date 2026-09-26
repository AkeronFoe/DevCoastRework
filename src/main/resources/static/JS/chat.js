/* Chat en vivo: envío por fetch (fallback a submit normal si falla),
   auto-resize del textarea, separadores de fecha, "cargar anteriores",
   y polling de mensajes nuevos mientras la pestaña está visible. */
(function () {
  const meta = document.getElementById('chat-meta')
  const solicitudId = meta.getAttribute('data-sol-id')
  const currentUserId = meta.getAttribute('data-user-id')
  const otroAvatarUrl = meta.getAttribute('data-otro-avatar')
  const otroNombre = meta.getAttribute('data-otro-nombre')

  const msgArea = document.getElementById('chat-scroll')
  const textarea = document.getElementById('chat-input')
  const form = document.getElementById('chat-form')
  const loadMoreWrap = document.getElementById('chat-load-more')
  const loadMoreBtn = document.getElementById('chat-load-more-btn')
  const sendBtn = document.getElementById('chat-send-btn')

  if (msgArea) msgArea.scrollTop = msgArea.scrollHeight

  let sending = false

  function resetTextareaHeight() { if (textarea) textarea.style.height = 'auto' }

  function enviarMensajeActual() {
    if (!textarea || sending) return
    const texto = textarea.value.trim()
    if (!texto) return

    sending = true
    textarea.disabled = true
    if (sendBtn) sendBtn.disabled = true

    fetch('/chat/' + solicitudId + '/enviar', {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'texto=' + encodeURIComponent(texto)
    }).then(r => { if (!r.ok) throw new Error('http ' + r.status); return r.json() })
      .then(data => {
        if (!data.ok) throw new Error('respuesta inesperada')
        textarea.value = ''
        resetTextareaHeight()
        appendMensaje(data.mensaje, true)
        if (data.mensaje.fechaIso) lastSeenIso = data.mensaje.fechaIso
      })
      .catch(() => { window.UIFeedback?.toast?.('No se pudo enviar el mensaje. Intenta de nuevo.', 'error') })
      .finally(() => {
        sending = false
        textarea.disabled = false
        if (sendBtn) sendBtn.disabled = false
        textarea.focus()
      })
  }

  if (textarea && form) {
    textarea.focus()
    form.addEventListener('submit', (e) => { e.preventDefault(); enviarMensajeActual() })
    textarea.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); enviarMensajeActual() }
    })
    textarea.addEventListener('input', function () {
      this.style.height = 'auto'
      this.style.height = Math.min(this.scrollHeight, 120) + 'px'
    })
  }

  function applyTimestamps(root) {
    ;(root || document).querySelectorAll('.chat-time:not([data-done])').forEach((el) => {
      const iso = el.getAttribute('data-fecha-iso')
      if (!iso) return
      el.textContent = new Date(iso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })
      el.setAttribute('data-done', '1')
    })
  }
  applyTimestamps()

  function buildDateSeps() {
    if (!msgArea) return
    msgArea.querySelectorAll('.chat-date-sep').forEach((s) => s.remove())
    const rows = msgArea.querySelectorAll('.chat-row')
    let lastDay = null
    rows.forEach((row) => {
      const d = row.getAttribute('data-msg-date')
      if (!d || d === lastDay) return
      lastDay = d
      const date = new Date(d + 'T00:00:00')
      const now = new Date()
      const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const yesterday = new Date(today.getTime() - 86400000)
      const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
      let label
      if (msgDay.getTime() === today.getTime()) label = 'Hoy'
      else if (msgDay.getTime() === yesterday.getTime()) label = 'Ayer'
      else { label = date.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }); label = label.charAt(0).toUpperCase() + label.slice(1) }
      const sep = document.createElement('div')
      sep.className = 'chat-date-sep'
      sep.textContent = label
      row.parentNode.insertBefore(sep, row)
    })
  }
  buildDateSeps()

  const TICK_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>'

  function esc(str) { return String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') }

  function buildBubble(msg) {
    const isOwn = msg.remitenteId === currentUserId
    const dateStr = msg.fechaIso ? msg.fechaIso.substring(0, 10) : ''
    const timeStr = msg.fechaIso ? new Date(msg.fechaIso).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }) : ''

    let avatarHtml = ''
    if (!isOwn) {
      const avStyle = otroAvatarUrl ? `background-image:url(${esc(otroAvatarUrl)});` : ''
      const initial = otroNombre ? otroNombre.charAt(0).toUpperCase() : '?'
      avatarHtml = `<div class="chat-avatar" style="${avStyle}">${otroAvatarUrl ? '' : `<span>${esc(initial)}</span>`}</div>`
    }
    const senderHtml = !isOwn ? `<span class="chat-sender">${esc(msg.remitenteNombre || '')}</span>` : ''
    const tickHtml = isOwn ? `<span class="chat-tick${msg.leido ? ' is-read' : ''}">${TICK_SVG}</span>` : ''
    const timeHtml = timeStr ? `<span class="chat-time" data-done="1">${esc(timeStr)}</span>` : ''

    return `<div class="chat-row is-new ${isOwn ? 'own' : 'other'}" data-msg-date="${esc(dateStr)}">`
      + avatarHtml
      + `<div class="chat-wrap"><div class="chat-bubble">${esc(msg.texto || '')}</div><div class="chat-meta">${senderHtml}${timeHtml}${tickHtml}</div></div>`
      + `</div>`
  }

  function isNearBottom() {
    if (!msgArea) return true
    return msgArea.scrollHeight - msgArea.scrollTop - msgArea.clientHeight < 120
  }

  function appendMensaje(msg, forceScroll) {
    if (!msgArea || !msg) return
    const wasNearBottom = isNearBottom()
    const tmp = document.createElement('div')
    tmp.innerHTML = buildBubble(msg)
    while (tmp.firstChild) msgArea.appendChild(tmp.firstChild)
    buildDateSeps()
    if (forceScroll || wasNearBottom) msgArea.scrollTop = msgArea.scrollHeight
  }

  if (loadMoreBtn && loadMoreWrap) {
    loadMoreBtn.addEventListener('click', () => {
      const primerFecha = loadMoreWrap.getAttribute('data-primer-fecha')
      if (!primerFecha) return

      loadMoreBtn.disabled = true
      loadMoreBtn.textContent = 'Cargando…'

      fetch('/chat/' + solicitudId + '/mas-mensajes?antes=' + encodeURIComponent(primerFecha))
        .then(r => r.json())
        .then(data => {
          if (data.mensajes && data.mensajes.length > 0) {
            const firstNode = msgArea.querySelector('.chat-load-more + *') || msgArea.querySelector('.chat-row')
            const frag = document.createDocumentFragment()
            data.mensajes.forEach((msg) => {
              const tmp = document.createElement('div')
              tmp.innerHTML = buildBubble(msg)
              while (tmp.firstChild) frag.appendChild(tmp.firstChild)
            })
            if (firstNode) msgArea.insertBefore(frag, firstNode)
            else msgArea.appendChild(frag)
            buildDateSeps()
            if (data.primerFecha) loadMoreWrap.setAttribute('data-primer-fecha', data.primerFecha)
          }
          if (!data.hayMas) {
            loadMoreWrap.style.display = 'none'
          } else {
            loadMoreBtn.disabled = false
            loadMoreBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg> Cargar mensajes anteriores'
          }
        })
        .catch(() => { loadMoreBtn.disabled = false; loadMoreBtn.textContent = 'Error al cargar. Reintentar' })
    })
  }

  let lastSeenIso = (function () {
    let last = null
    ;(msgArea ? msgArea.querySelectorAll('.chat-time[data-fecha-iso]') : []).forEach((el) => {
      const iso = el.getAttribute('data-fecha-iso')
      if (iso && (!last || iso > last)) last = iso
    })
    return last || new Date().toISOString().substring(0, 19)
  })()

  const liveDot = document.getElementById('chat-live-indicator')
  const liveLabel = liveDot ? liveDot.querySelector('.lbl') : null
  let pollTimer = null
  let pollFails = 0

  function setLiveOffline(offline) {
    if (!liveDot) return
    liveDot.classList.toggle('is-offline', offline)
    if (liveLabel) liveLabel.textContent = offline ? 'Reconectando…' : 'En vivo'
    liveDot.title = offline ? 'No se pudo actualizar la conversación, reintentando…' : 'Recibiendo mensajes nuevos automáticamente'
  }

  function pollNuevos() {
    if (document.visibilityState !== 'visible') return
    fetch('/chat/' + solicitudId + '/nuevos?despues=' + encodeURIComponent(lastSeenIso))
      .then(r => { if (!r.ok) throw new Error('http ' + r.status); return r.json() })
      .then(data => {
        if (pollFails > 0) { pollFails = 0; setLiveOffline(false) }
        if (!data.mensajes || !data.mensajes.length) return
        data.mensajes.forEach((msg) => {
          appendMensaje(msg, msg.remitenteId !== currentUserId)
          if (msg.fechaIso && msg.fechaIso > lastSeenIso) lastSeenIso = msg.fechaIso
        })
      }).catch(() => {
        pollFails++
        // recién a la 2da falla seguida (~8s) para no parpadear por un
        // único hipo de red
        if (pollFails >= 2) setLiveOffline(true)
      })
  }

  function startPolling() { stopPolling(); pollTimer = setInterval(pollNuevos, 4000); if (liveDot) liveDot.style.opacity = '1' }
  function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null }; pollFails = 0; setLiveOffline(false); if (liveDot) liveDot.style.opacity = '.4' }

  if (solicitudId) {
    startPolling()
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') { pollNuevos(); startPolling() }
      else stopPolling()
    })
    window.addEventListener('beforeunload', stopPolling)
  }
})()
