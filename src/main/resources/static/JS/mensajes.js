/* Bandeja de mensajes: fechas relativas, búsqueda + filtro de no leídos,
   y el conteo animado de los dos gauges del resumen. */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  document.querySelectorAll('.mg-rel-date').forEach((el) => {
    const iso = el.getAttribute('data-fecha-iso')
    if (!iso) return
    const d = new Date(iso)
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const yesterday = new Date(today.getTime() - 86400000)
    const msgDay = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    if (msgDay.getTime() === today.getTime()) el.textContent = d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })
    else if (msgDay.getTime() === yesterday.getTime()) el.textContent = 'Ayer'
    else el.textContent = d.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit' })
  })

  const searchInput = document.getElementById('mg-search-input')
  const chips = document.querySelectorAll('[data-mg-filter]')
  const list = document.getElementById('mg-list')
  const empty = document.getElementById('mg-filter-empty')
  let activeFilter = 'todos'

  function applyFilters() {
    if (!list) return
    const query = (searchInput?.value || '').trim().toLowerCase()
    const items = list.querySelectorAll('.mg-row')
    let visible = 0
    items.forEach((item) => {
      const matchesQuery = !query || (item.dataset.search || '').includes(query)
      const matchesFilter = activeFilter === 'todos' || item.dataset.unread === 'true'
      const match = matchesQuery && matchesFilter
      item.classList.toggle('is-hidden', !match)
      if (match) visible++
    })
    if (empty) empty.hidden = visible !== 0
    list.hidden = visible === 0
  }

  searchInput?.addEventListener('input', applyFilters)
  chips.forEach((chip) => {
    chip.addEventListener('click', () => {
      chips.forEach((c) => c.classList.remove('active'))
      chip.classList.add('active')
      activeFilter = chip.dataset.mgFilter
      applyFilters()
    })
  })
  document.getElementById('mg-filter-reset')?.addEventListener('click', () => {
    if (searchInput) searchInput.value = ''
    chips.forEach((c) => c.classList.remove('active'))
    document.querySelector('[data-mg-filter="todos"]')?.classList.add('active')
    activeFilter = 'todos'
    applyFilters()
  })

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.mg-stat-num[data-value]').forEach((el, i) => {
      const target = parseInt(el.dataset.value, 10) || 0
      if (reduceMotion || !hasGsap) { el.textContent = String(target); return }
      const obj = { v: 0 }
      el.textContent = '0'
      gsap.to(obj, { v: target, duration: .6, delay: i * .05, ease: 'power2.out', onUpdate: () => { el.textContent = String(Math.round(obj.v)) } })
    })
  })
})()
