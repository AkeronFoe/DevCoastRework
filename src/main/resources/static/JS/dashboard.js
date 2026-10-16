/* Dashboard como "dashboard.tsx" corriendo en modo dev: stagger de entrada
   por sección (una sola vez, al cargar), conteo animado en los gauges, y
   el número grande de la cabecera armado a partir de los warnings
   realmente pendientes (no un valor fijo del backend). */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  /* ── número grande de la cabecera: cuántos warnings tiene el talento hoy ── */
  const bigNum = document.getElementById('dashBigNum')
  if (bigNum && bigNum.dataset.source === 'warnings') {
    const count = document.querySelectorAll('.dash-warning').length
    bigNum.textContent = String(count)
  }

  /* ── conteo animado de los gauges (y del número grande, si trae data-value) ── */
  function animateCounters() {
    document.querySelectorAll('.dash-stat-num[data-value], .ws-big-num[data-value]').forEach((el, i) => {
      const target = parseInt(el.dataset.value, 10) || 0
      if (reduceMotion || !hasGsap) { el.textContent = String(target); return }
      const obj = { v: 0 }
      gsap.to(obj, {
        v: target, duration: .7, delay: i * .04, ease: 'power2.out',
        onUpdate: () => { el.textContent = String(Math.round(obj.v)) }
      })
    })
  }

  /* ── stagger de entrada de las secciones (una sola vez, carga inicial) ── */
  function revealSections() {
    if (reduceMotion || !hasGsap) return
    const chunks = document.querySelectorAll('.dash-reveal')
    gsap.fromTo(chunks, { opacity: 0, y: 12 }, { opacity: 1, y: 0, duration: .4, ease: 'power2.out', stagger: .07 })
  }

  /* ── marcar todas las notificaciones como leídas ── */
  document.getElementById('markAllRead')?.addEventListener('click', function () {
    fetch('/notificaciones/leer-todas', { method: 'POST', headers: { 'Content-Type': 'application/json' } })
      .then(r => r.json()).then(d => {
        if (d.ok) {
          document.querySelectorAll('.ws-notif.is-unread').forEach(el => el.classList.remove('is-unread'))
          document.querySelectorAll('.ws-notif-dot').forEach(el => el.remove())
          document.querySelectorAll('.ws-bell-badge, .ws-bell-drop-count, .ws-bell-drop-foot').forEach(el => el.remove())
        }
      }).catch(() => { })
  })

  document.addEventListener('DOMContentLoaded', () => {
    animateCounters()
    revealSections()
  })
})()
