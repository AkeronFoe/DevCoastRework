/* Ficha de un empleo: modal de aplicar (con selector de proyectos a
   adjuntar, máx. 3) y micro-animación de entrada del hero. El botón de
   favorito lo maneja dashboard-extras.js (initFavoritos), igual que en
   /empleos, así ambas páginas comparten la misma lógica de guardado. */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  const overlay = document.getElementById('jd-modal-overlay')
  const btnAbrir = document.getElementById('btn-abrir-modal-aplicar')
  const btnCerrar = document.getElementById('btn-cerrar-modal-aplicar')
  const btnCancelar = document.getElementById('btn-cancelar-aplicar')
  const counter = document.getElementById('jd-counter-proyectos')
  const aviso = document.getElementById('jd-max-aviso')
  const MAX = 3

  function abrirModal() {
    overlay.classList.add('is-open')
    document.body.style.overflow = 'hidden'
    if (hasGsap && !reduceMotion) {
      gsap.fromTo(overlay.querySelector('.jd-modal'), { y: 16, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: .28, ease: 'power2.out' })
    }
    setTimeout(() => document.querySelector('#jd-form-aplicar textarea')?.focus(), 100)
  }
  function cerrarModal() {
    overlay.classList.remove('is-open')
    document.body.style.overflow = ''
  }

  btnAbrir?.addEventListener('click', abrirModal)
  btnCerrar?.addEventListener('click', cerrarModal)
  btnCancelar?.addEventListener('click', cerrarModal)
  overlay?.addEventListener('click', (e) => { if (e.target === overlay) cerrarModal() })
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && overlay?.classList.contains('is-open')) cerrarModal() })

  let seleccionados = 0
  document.querySelectorAll('.jd-proy-card').forEach(card => {
    const checkbox = card.querySelector('.jd-proy-checkbox')
    card.addEventListener('click', () => {
      const yaSeleccionado = checkbox.checked
      if (!yaSeleccionado && seleccionados >= MAX) {
        if (aviso) aviso.style.display = 'block'
        if (hasGsap && !reduceMotion) gsap.fromTo(card, { x: -4 }, { x: 0, duration: .3, ease: 'elastic.out(1, 0.4)' })
        return
      }
      checkbox.checked = !yaSeleccionado
      if (aviso) aviso.style.display = 'none'
      card.classList.toggle('is-selected', checkbox.checked)
      seleccionados += checkbox.checked ? 1 : -1
      if (counter) {
        counter.textContent = seleccionados === 0
          ? '0 proyectos seleccionados'
          : seleccionados + (seleccionados === 1 ? ' proyecto seleccionado' : ' proyectos seleccionados')
      }
    })
  })

  /* ── entrada suave del hero y las cards de la ficha ── */
  document.addEventListener('DOMContentLoaded', () => {
    if (reduceMotion || !hasGsap) {
      document.querySelectorAll('.job-ring .prog').forEach(circle => {
        const pct = parseFloat(circle.dataset.pct) || 0
        const full = 100.5
        circle.style.strokeDasharray = String(full)
        circle.style.strokeDashoffset = String(full * (1 - pct / 100))
      })
      return
    }
    const chunks = document.querySelectorAll('.jd-reveal')
    gsap.fromTo(chunks, { opacity: 0, y: 10 }, { opacity: 1, y: 0, duration: .35, ease: 'power2.out', stagger: .05 })

    document.querySelectorAll('.job-ring .prog').forEach(circle => {
      const pct = parseFloat(circle.dataset.pct) || 0
      const full = 100.5
      const target = full * (1 - pct / 100)
      gsap.set(circle, { strokeDasharray: full, strokeDashoffset: full })
      gsap.to(circle, { strokeDashoffset: target, duration: .9, delay: .2, ease: 'power2.out' })
    })
  })
})()
