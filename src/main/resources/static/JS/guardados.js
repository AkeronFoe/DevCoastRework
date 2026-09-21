/* Guardados: quitar de favoritos anima la salida de la card (exit sutil,
   solo opacity + un pequeño translateY, más suave que la entrada), filtra
   por compatibilidad/aplicado, y ordena reciente vs. mayor match. */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  /* ── quitar de guardados ── */
  document.querySelectorAll('.job-save[data-empleo-id]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation()
      if (btn.disabled) return
      const empleoId = btn.dataset.empleoId
      const card = btn.closest('.job-card')
      btn.disabled = true
      btn.style.opacity = '.4'

      fetch('/empleos/favorito/' + empleoId, { method: 'POST' })
        .then(r => { if (!r.ok) throw new Error('http ' + r.status); return r.json() })
        .then(d => {
          if (!(d.ok && !d.favorito)) throw new Error('respuesta inesperada')
          const score = parseInt(card.dataset.score || '0', 10)
          const yaAplico = card.dataset.aplico === 'true'
          const removeCard = () => { card.remove(); updateCounts(score, yaAplico) }
          if (hasGsap && !reduceMotion) {
            gsap.to(card, { opacity: 0, y: -6, duration: .22, ease: 'power2.out', onComplete: removeCard })
          } else {
            card.style.transition = 'opacity .2s'
            card.style.opacity = '0'
            setTimeout(removeCard, 200)
          }
        })
        .catch(() => {
          btn.disabled = false
          btn.style.opacity = ''
          if (window.UIFeedback?.toast) window.UIFeedback.toast('No se pudo quitar el empleo. Intenta de nuevo.', 'error')
        })
    })
  })

  function updateCounts(removedScore, removedAplico) {
    const grid = document.getElementById('guardados-grid')
    const remaining = grid ? grid.querySelectorAll('.job-card').length : 0
    const crumbCount = document.getElementById('guardados-count')
    if (crumbCount) crumbCount.textContent = remaining
    const totalMetric = document.getElementById('gd-total-metric')
    if (totalMetric) totalMetric.textContent = remaining
    if (removedScore >= 70) {
      const altaMetric = document.getElementById('gd-alta-metric')
      if (altaMetric) altaMetric.textContent = Math.max(0, parseInt(altaMetric.textContent, 10) - 1)
    }
    if (removedAplico) {
      const aplicoMetric = document.getElementById('gd-aplico-metric')
      if (aplicoMetric) aplicoMetric.textContent = Math.max(0, parseInt(aplicoMetric.textContent, 10) - 1)
    }
    if (remaining === 0) location.reload()
  }

  /* ── filtros + orden ── */
  const chips = document.querySelectorAll('[data-guardado-filter]')
  const grid = document.getElementById('guardados-grid')
  const empty = document.getElementById('guardados-filter-empty')
  let active = 'todos'

  function applyFilter() {
    if (!grid) return
    const cards = grid.querySelectorAll('.job-card')
    let visible = 0
    cards.forEach(card => {
      const score = parseInt(card.dataset.score || '0', 10)
      const aplico = card.dataset.aplico === 'true'
      const match = active === 'todos' || (active === 'alta' && score >= 70) || (active === 'sin-aplicar' && !aplico)
      card.classList.toggle('is-hidden', !match)
      if (match) visible++
    })
    if (empty) empty.hidden = visible !== 0
    grid.hidden = visible === 0
  }

  chips.forEach(chip => {
    chip.addEventListener('click', () => {
      chips.forEach(c => c.classList.remove('active'))
      chip.classList.add('active')
      active = chip.dataset.guardadoFilter
      applyFilter()
    })
  })

  document.getElementById('guardados-filter-reset')?.addEventListener('click', () => {
    chips.forEach(c => c.classList.remove('active'))
    document.querySelector('[data-guardado-filter="todos"]')?.classList.add('active')
    active = 'todos'
    applyFilter()
  })

  document.getElementById('guardado-sort-select')?.addEventListener('change', function () {
    if (!grid) return
    const cards = Array.from(grid.querySelectorAll('.job-card'))
    if (this.value === 'score') {
      cards.sort((a, b) => (parseInt(b.dataset.score || '0', 10)) - (parseInt(a.dataset.score || '0', 10)))
    } else {
      cards.sort((a, b) => (a.dataset.originalOrder | 0) - (b.dataset.originalOrder | 0))
    }
    cards.forEach(card => grid.appendChild(card))
  })

  if (grid) {
    Array.from(grid.querySelectorAll('.job-card')).forEach((card, i) => { card.dataset.originalOrder = i })
  }

  /* ── conteo animado de los gauges del resumen ── */
  document.addEventListener('DOMContentLoaded', () => {
    const tiles = document.querySelectorAll('.gd-stat')
    if (hasGsap && !reduceMotion && tiles.length) {
      gsap.fromTo(tiles, { opacity: 0, y: 10 }, { opacity: 1, y: 0, duration: .35, ease: 'power2.out', stagger: .06 })
    }
    document.querySelectorAll('.gd-stat-num[id]').forEach((el, i) => {
      const target = parseInt(el.textContent, 10) || 0
      if (reduceMotion || !hasGsap) return
      const obj = { v: 0 }
      el.textContent = '0'
      gsap.to(obj, { v: target, duration: .6, delay: .1 + i * .05, ease: 'power2.out', onUpdate: () => { el.textContent = String(Math.round(obj.v)) } })
    })
  })
})()
