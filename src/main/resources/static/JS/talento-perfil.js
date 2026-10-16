/* Ficha pública de un talento: modal de invitar (toggle empleo/mensaje
   libre), "en DevCoast desde…" a partir del id de mongo, copiar enlace,
   y entrada suave de las cards. */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  /* ── modal de invitar ── */
  const modal = document.getElementById('tp-modal-invitar')
  const btnAbrir = document.getElementById('btn-invitar')
  const btnCerrar = document.getElementById('tp-btn-cerrar-modal')
  const btnCancelar = document.getElementById('tp-btn-cancelar-modal')

  function abrirModal() {
    modal.classList.add('is-open')
    if (hasGsap && !reduceMotion) {
      gsap.fromTo(modal.querySelector('.tp-modal'), { y: 16, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: .28, ease: 'power2.out' })
    }
  }
  function cerrarModal() { modal.classList.remove('is-open') }

  btnAbrir?.addEventListener('click', abrirModal)
  btnCerrar?.addEventListener('click', cerrarModal)
  btnCancelar?.addEventListener('click', cerrarModal)
  modal?.addEventListener('click', (e) => { if (e.target === modal) cerrarModal() })
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && modal?.classList.contains('is-open')) cerrarModal() })

  document.querySelectorAll('input[name="tipoInvitacion"]').forEach(radio => {
    radio.addEventListener('change', () => {
      const esEmpleo = document.getElementById('tp-tipo-empleo').checked
      const bloque = document.getElementById('tp-bloque-empleo')
      if (bloque) bloque.style.display = esEmpleo ? '' : 'none'
      const select = document.getElementById('tp-select-empleo')
      if (select) select.required = esEmpleo
      document.querySelectorAll('.tp-type-card').forEach(card => {
        card.classList.toggle('is-active', card.dataset.tipo === (esEmpleo ? 'empleo' : 'libre'))
      })
    })
  })

  /* ── "en DevCoast desde…", a partir del timestamp del ObjectId ── */
  const since = document.getElementById('tp-since')
  if (since && since.dataset.talentoId) {
    const hexId = since.dataset.talentoId
    if (hexId.length >= 8) {
      const ts = parseInt(hexId.substring(0, 8), 16) * 1000
      const d = Date.now() - ts
      let texto
      if (d < 86400000) texto = 'Se unió hoy'
      else if (d < 30 * 86400000) texto = 'en DevCoast hace ' + Math.floor(d / 86400000) + ' días'
      else if (d < 365 * 86400000) texto = 'en DevCoast hace ' + Math.floor(d / (30 * 86400000)) + ' meses'
      else texto = 'en DevCoast hace más de 1 año'
      since.textContent = '· ' + texto
    }
  }

  /* ── copiar enlace del perfil ── */
  const copyBtn = document.querySelector('.tp-copy-btn')
  if (copyBtn) {
    const label = copyBtn.querySelector('.lbl')
    const orig = label ? label.textContent : 'Copiar enlace'
    async function copyText(text) {
      if (navigator?.clipboard?.writeText) {
        try { await navigator.clipboard.writeText(text); return true } catch { /* fallback */ }
      }
      const ta = document.createElement('textarea')
      Object.assign(ta.style, { position: 'absolute', left: '-9999px' })
      ta.value = text; ta.readOnly = true
      document.body.appendChild(ta); ta.select()
      const ok = document.execCommand('copy')
      document.body.removeChild(ta); return ok
    }
    copyBtn.addEventListener('click', async () => {
      const path = copyBtn.dataset.copyUrl
      if (!path) return
      const url = path.startsWith('http') ? path : location.origin + path
      if (await copyText(url)) {
        copyBtn.classList.add('is-copied')
        if (label) label.textContent = '¡Copiado!'
        setTimeout(() => { copyBtn.classList.remove('is-copied'); if (label) label.textContent = orig }, 1400)
      }
    })
  }

  /* ── entrada suave de las secciones ── */
  document.addEventListener('DOMContentLoaded', () => {
    if (reduceMotion || !hasGsap) return
    const chunks = document.querySelectorAll('.tp-reveal')
    gsap.fromTo(chunks, { opacity: 0, y: 10 }, { opacity: 1, y: 0, duration: .35, ease: 'power2.out', stagger: .05 })
  })
})()
