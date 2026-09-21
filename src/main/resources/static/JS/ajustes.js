/* Ajustes, como "editor de configuración" dentro del workspace: cada
   sección es una pestaña de archivo (apariencia.theme, cuenta.env, ...) y
   el panel de la derecha muestra en vivo el YAML resultante. Todo lo que
   toca red (privacidad, correo, eliminar cuenta) sigue pegándole a los
   mismos endpoints que antes, solo cambió la piel. */
(function () {
  const reduceMotion = (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches) ||
    localStorage.getItem('dc-reduce-motion') === 'true'
  const hasGsap = typeof window.gsap !== 'undefined'

  const AJ = window.__AJUSTES__ || {}
  const EXT_MAP = { apariencia: 'theme', cuenta: 'env', privacidad: 'lock', notificaciones: 'json', accesibilidad: 'a11y', peligro: 'danger' }
  const tabsOrder = Array.from(document.querySelectorAll('.set-filetab[data-section]')).map(t => t.dataset.section)
  let isFirstActivation = true

  /* ── pestañas de archivo ── */
  function activarSeccion(nombre, updateHash) {
    const tab = document.querySelector('.set-filetab[data-section="' + nombre + '"]')
    if (!tab) return false
    document.querySelectorAll('.set-filetab').forEach(t => { t.classList.remove('active'); t.tabIndex = -1 })
    document.querySelectorAll('.set-panel').forEach(p => p.classList.remove('active'))
    tab.classList.add('active')
    tab.tabIndex = 0
    const panel = document.getElementById('panel-' + nombre)
    if (panel) {
      panel.classList.add('active')
      // pasada inicial (carga de página): sin animación, per better-ui "skip animation on page load"
      if (!isFirstActivation && hasGsap && !reduceMotion) {
        const chunks = panel.querySelectorAll('.set-group-title, .set-row')
        gsap.fromTo(chunks, { opacity: 0, y: 8 }, { opacity: 1, y: 0, duration: .32, ease: 'power2.out', stagger: .05, overwrite: true })
      }
    }
    const crumb = document.getElementById('crumbFile')
    if (crumb) crumb.textContent = 'ajustes/' + nombre + (EXT_MAP[nombre] ? '.' + EXT_MAP[nombre] : '')
    if (updateHash) history.replaceState(null, '', '#' + nombre)
    isFirstActivation = false
    return true
  }

  const seccionInicial = (location.hash || '').replace('#', '')
  document.querySelectorAll('.set-filetab[data-section]').forEach(tab => {
    tab.addEventListener('click', () => activarSeccion(tab.dataset.section, true))
  })
  if (!seccionInicial || !activarSeccion(seccionInicial, false)) activarSeccion('apariencia', false)

  window.addEventListener('hashchange', () => {
    const s = (location.hash || '').replace('#', '')
    if (s) activarSeccion(s, false)
  })

  /* ── navegación de pestañas con flechas (roving tabindex, como un editor real) ── */
  document.querySelector('.set-filetabs')?.addEventListener('keydown', (e) => {
    if (!['ArrowRight', 'ArrowLeft', 'Home', 'End'].includes(e.key)) return
    const current = tabsOrder.indexOf((location.hash || '').replace('#', '') || 'apariencia')
    let next = current
    if (e.key === 'ArrowRight') next = (current + 1) % tabsOrder.length
    else if (e.key === 'ArrowLeft') next = (current - 1 + tabsOrder.length) % tabsOrder.length
    else if (e.key === 'Home') next = 0
    else if (e.key === 'End') next = tabsOrder.length - 1
    e.preventDefault()
    activarSeccion(tabsOrder[next], true)
    document.querySelector('.set-filetab[data-section="' + tabsOrder[next] + '"]')?.focus()
  })

  /* ── "miembro desde", a partir del timestamp embebido en el id de mongo ── */
  function calcMiembroDesde(hexId) {
    if (!hexId || hexId.length < 8) return null
    const ts = parseInt(hexId.substring(0, 8), 16) * 1000
    if (isNaN(ts)) return null
    const d = new Date(ts)
    const meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']
    return `miembro desde ${meses[d.getMonth()]} ${d.getFullYear()}`
  }
  const miembroLabel = document.getElementById('miembro-desde-label')
  if (miembroLabel && AJ.usuarioId) {
    const texto = calcMiembroDesde(AJ.usuarioId)
    if (texto) miembroLabel.textContent = texto
  }

  /* ── feedback: toast estilo "commit" + estado de sync en la status bar ── */
  function commitToast(path, value) {
    if (!(window.UIFeedback && window.UIFeedback.toast)) return
    const label = typeof value === 'boolean' ? (value ? 'true' : 'false') : value
    window.UIFeedback.toast('commit ' + path + ' → ' + label, 'success')
  }

  let lastSyncAt = Date.now()
  function markSynced() { lastSyncAt = Date.now(); updateSyncLabel() }
  function updateSyncLabel() {
    const el = document.getElementById('syncState')
    if (!el) return
    const secs = Math.floor((Date.now() - lastSyncAt) / 1000)
    el.innerHTML = '<span class="sync-dot"></span>' + (secs < 3 ? 'sincronizado justo ahora' : 'sincronizado hace ' + secs + 's')
  }
  updateSyncLabel()
  setInterval(updateSyncLabel, 1000)

  /* ── panel YAML en vivo ── */
  function esc(s) { return String(s).replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c])) }
  function yBool(v) { return `<span class="y-bool-${v ? 'true' : 'false'}">${v}</span>` }

  function yamlState() {
    return {
      tema: localStorage.getItem('theme') || 'dark',
      densidad: localStorage.getItem('ui-density') || 'normal',
      texto: localStorage.getItem('ui-text-size') || 'normal',
      contraste_alto: localStorage.getItem('ui-contrast') === 'high',
      reducir_movimiento: localStorage.getItem('dc-reduce-motion') === 'true',
      'privacidad.perfil_publico': !!privacyState.perfilPublico,
      'privacidad.mostrar_correo': !!privacyState.mostrarCorreo,
      'privacidad.permitir_mensajes': !!privacyState.permitirMensajes,
      'notificaciones.invitaciones': document.getElementById('notif-invitaciones')?.checked ?? true,
      'notificaciones.solicitudes': document.getElementById('notif-solicitudes')?.checked ?? true,
      'notificaciones.producto': document.getElementById('notif-producto')?.checked ?? false
    }
  }

  function updateYaml(changedKey) {
    const s = yamlState()
    const rol = AJ.tipo === 'empresa' ? 'empresa' : 'talento'
    const lines = [
      { key: '_comment', html: `<span class="y-com"># ajustes.yml — devcoast/${rol}</span>` },
      { key: 'tema', html: `<span class="y-key">tema</span>: <span class="y-str">"${esc(s.tema)}"</span> <span class="y-com"># páginas clásicas</span>` },
      { key: 'densidad', html: `<span class="y-key">densidad</span>: <span class="y-str">"${esc(s.densidad)}"</span>` },
      { key: 'texto', html: `<span class="y-key">texto</span>: <span class="y-str">"${esc(s.texto)}"</span>` },
      { key: 'contraste_alto', html: `<span class="y-key">contraste_alto</span>: ${yBool(s.contraste_alto)}` },
      { key: 'reducir_movimiento', html: `<span class="y-key">reducir_movimiento</span>: ${yBool(s.reducir_movimiento)}` },
      { key: '_privacidad', html: `<span class="y-key">privacidad</span>:` },
      { key: 'privacidad.perfil_publico', html: `<span class="y-indent">  </span><span class="y-key">perfil_publico</span>: ${yBool(s['privacidad.perfil_publico'])}` },
      { key: 'privacidad.mostrar_correo', html: `<span class="y-indent">  </span><span class="y-key">mostrar_correo</span>: ${yBool(s['privacidad.mostrar_correo'])}` },
      { key: 'privacidad.permitir_mensajes', html: `<span class="y-indent">  </span><span class="y-key">permitir_mensajes</span>: ${yBool(s['privacidad.permitir_mensajes'])}` },
      { key: '_notificaciones', html: `<span class="y-key">notificaciones</span>:` },
      { key: 'notificaciones.invitaciones', html: `<span class="y-indent">  </span><span class="y-key">invitaciones</span>: ${yBool(s['notificaciones.invitaciones'])}` },
      { key: 'notificaciones.solicitudes', html: `<span class="y-indent">  </span><span class="y-key">solicitudes</span>: ${yBool(s['notificaciones.solicitudes'])}` },
      { key: 'notificaciones.producto', html: `<span class="y-indent">  </span><span class="y-key">producto</span>: ${yBool(s['notificaciones.producto'])}` }
    ]
    const out = document.getElementById('yaml-output')
    if (!out) return
    out.innerHTML = lines.map(l => `<span class="y-line" data-key="${l.key}">${l.html}</span>`).join('\n')

    const gutter = document.getElementById('yaml-gutter')
    if (gutter && gutter.childElementCount !== lines.length) {
      gutter.innerHTML = lines.map((_, i) => `<span>${i + 1}</span>`).join('\n')
    }

    // versión sin markup, para el botón de copiar
    lastYamlPlain = out.textContent

    if (changedKey) {
      const el = out.querySelector('.y-line[data-key="' + changedKey + '"]')
      if (el) {
        requestAnimationFrame(() => {
          el.classList.add('is-flash')
          setTimeout(() => el.classList.remove('is-flash'), 900)
        })
      }
    }
  }
  let lastYamlPlain = ''

  document.getElementById('yaml-copy')?.addEventListener('click', function () {
    const btn = this
    navigator.clipboard?.writeText(lastYamlPlain).then(() => {
      btn.classList.add('is-copied')
      if (window.UIFeedback?.toast) window.UIFeedback.toast('YAML copiado al portapapeles', 'success')
      setTimeout(() => btn.classList.remove('is-copied'), 1400)
    }).catch(() => {
      if (window.UIFeedback?.toast) window.UIFeedback.toast('No se pudo copiar', 'error')
    })
  })

  /* ── tema (swatches) ── */
  document.addEventListener('dc:theme-changed', (e) => {
    document.querySelectorAll('.set-swatch').forEach(b => b.classList.toggle('active', b.dataset.theme === e.detail.theme))
    updateYaml('tema')
  })

  function initThemeSwatches() {
    const saved = localStorage.getItem('theme') || 'dark'
    document.querySelectorAll('.set-swatch').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.theme === saved)
      btn.addEventListener('click', () => {
        document.querySelectorAll('.set-swatch').forEach(b => b.classList.remove('active'))
        btn.classList.add('active')
        if (window.applyTheme) window.applyTheme(btn.dataset.theme)
        markSynced(); commitToast('tema', btn.dataset.theme)
        if (hasGsap && !reduceMotion) gsap.fromTo(btn, { scale: .94 }, { scale: 1, duration: .3, ease: 'back.out(1.7)' })
      })
    })
  }

  /* ── densidad y texto ── */
  function initDensity() {
    const saved = localStorage.getItem('ui-density') || 'normal'
    document.querySelectorAll('.set-density-opt[data-density]').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.density === saved)
      btn.addEventListener('click', () => {
        document.querySelectorAll('.set-density-opt').forEach(b => b.classList.remove('active'))
        btn.classList.add('active')
        localStorage.setItem('ui-density', btn.dataset.density)
        if (window.applyUiPreferences) window.applyUiPreferences()
        updateYaml('densidad'); markSynced(); commitToast('densidad', btn.dataset.density)
      })
    })
  }

  function initTextSize() {
    const sel = document.getElementById('text-size-select')
    if (!sel) return
    sel.value = localStorage.getItem('ui-text-size') || 'normal'
    sel.addEventListener('change', () => {
      localStorage.setItem('ui-text-size', sel.value)
      if (window.applyUiPreferences) window.applyUiPreferences()
      updateYaml('texto'); markSynced(); commitToast('texto', sel.value)
    })
  }

  /* ── privacidad ── */
  let privacyState = {
    perfilPublico: !!AJ.perfilPublico,
    mostrarCorreo: !!AJ.mostrarCorreo,
    permitirMensajes: AJ.permitirMensajes !== false
  }

  function updatePrivacyPanel() {
    const dot = (id, on) => { const el = document.getElementById(id); if (el) el.className = 'p-dot ' + (on ? 'on' : 'off') }
    const val = (id, txt) => { const el = document.getElementById(id); if (el) el.textContent = txt }
    dot('pdot-perfil', privacyState.perfilPublico)
    dot('pdot-correo', privacyState.mostrarCorreo)
    dot('pdot-mensajes', privacyState.permitirMensajes)
    val('pval-perfil', privacyState.perfilPublico ? 'visible públicamente' : 'oculto')
    val('pval-correo', privacyState.mostrarCorreo ? 'expuesto' : 'oculto')
    val('pval-mensajes', privacyState.permitirMensajes ? 'permitido' : 'bloqueado')
    updateTabDots()
  }

  function updateTabDots() {
    const dotPriv = document.getElementById('tabdot-privacidad')
    if (dotPriv) dotPriv.style.display = privacyState.perfilPublico ? 'inline-block' : 'none'
    const allOn = (document.getElementById('notif-invitaciones')?.checked ?? true) && (document.getElementById('notif-solicitudes')?.checked ?? true)
    const dotNotif = document.getElementById('tabdot-notif')
    if (dotNotif) dotNotif.style.display = !allOn ? 'inline-block' : 'none'
  }

  async function savePrivacy(field, value, yamlKey) {
    privacyState[field] = value
    updatePrivacyPanel()
    updateYaml(yamlKey)
    try {
      const res = await fetch('/ajustes/privacidad', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ [field]: value })
      })
      const data = await res.json()
      if (data.ok) { markSynced(); commitToast('privacidad.' + field, value) }
      else if (window.UIFeedback?.toast) window.UIFeedback.toast('No se pudo guardar el cambio', 'error')
    } catch {
      if (window.UIFeedback?.toast) window.UIFeedback.toast('Error de conexión', 'error')
    }
  }

  document.getElementById('toggle-perfil-publico')?.addEventListener('change', function () {
    savePrivacy('perfilPublico', this.checked, 'privacidad.perfil_publico')
  })
  document.getElementById('toggle-mostrar-correo')?.addEventListener('change', function () {
    savePrivacy('mostrarCorreo', this.checked, 'privacidad.mostrar_correo')
  })
  document.getElementById('toggle-permitir-mensajes')?.addEventListener('change', function () {
    savePrivacy('permitirMensajes', this.checked, 'privacidad.permitir_mensajes')
  })

  /* ── notificaciones (solo local, no hay endpoint) ── */
  function initNotif() {
    try {
      const p = JSON.parse(localStorage.getItem('dc-notif') || '{}')
      if ('invitaciones' in p) document.getElementById('notif-invitaciones').checked = p.invitaciones
      if ('solicitudes' in p) document.getElementById('notif-solicitudes').checked = p.solicitudes
      if ('producto' in p) document.getElementById('notif-producto').checked = p.producto
    } catch { }
    updateTabDots()
  }

  function saveNotif(field, yamlKey) {
    const prefs = {
      invitaciones: document.getElementById('notif-invitaciones')?.checked ?? true,
      solicitudes: document.getElementById('notif-solicitudes')?.checked ?? true,
      producto: document.getElementById('notif-producto')?.checked ?? false
    }
    localStorage.setItem('dc-notif', JSON.stringify(prefs))
    updateYaml(yamlKey); markSynced(); commitToast('notificaciones.' + field, prefs[field])
    updateTabDots()
  }
  document.getElementById('notif-invitaciones')?.addEventListener('change', () => saveNotif('invitaciones', 'notificaciones.invitaciones'))
  document.getElementById('notif-solicitudes')?.addEventListener('change', () => saveNotif('solicitudes', 'notificaciones.solicitudes'))
  document.getElementById('notif-producto')?.addEventListener('change', () => saveNotif('producto', 'notificaciones.producto'))

  /* ── accesibilidad ── */
  function initAccesibilidad() {
    const hc = localStorage.getItem('ui-contrast') === 'high'
    const rm = localStorage.getItem('dc-reduce-motion') === 'true'
    const c = document.getElementById('toggle-contrast')
    const r = document.getElementById('toggle-reduce-motion')
    if (c) c.checked = hc
    if (r) r.checked = rm
  }
  document.getElementById('toggle-contrast')?.addEventListener('change', function () {
    localStorage.setItem('ui-contrast', this.checked ? 'high' : 'normal')
    if (window.applyUiPreferences) window.applyUiPreferences()
    updateYaml('contraste_alto'); markSynced(); commitToast('contraste_alto', this.checked)
  })
  document.getElementById('toggle-reduce-motion')?.addEventListener('change', function () {
    localStorage.setItem('dc-reduce-motion', this.checked)
    if (window.applyUiPreferences) window.applyUiPreferences()
    updateYaml('reducir_movimiento'); markSynced(); commitToast('reducir_movimiento', this.checked)
  })

  /* ── modal: eliminar cuenta ── */
  const modalEliminar = document.getElementById('modal-eliminar')
  const inputConfirmar = document.getElementById('input-confirmar-eliminar')
  const inputPasswordEliminar = document.getElementById('input-password-eliminar')
  const btnConfirmar = document.getElementById('btn-confirmar-eliminar')

  function actualizarBotonEliminar() {
    const ok = inputConfirmar.value.trim() === 'ELIMINAR' && inputPasswordEliminar.value.length > 0
    btnConfirmar.disabled = !ok
    btnConfirmar.style.opacity = ok ? '1' : '.45'
    btnConfirmar.style.cursor = ok ? 'pointer' : 'default'
  }

  document.getElementById('btn-eliminar-cuenta')?.addEventListener('click', () => {
    if (inputConfirmar) inputConfirmar.value = ''
    if (inputPasswordEliminar) inputPasswordEliminar.value = ''
    if (btnConfirmar) { btnConfirmar.disabled = true; btnConfirmar.style.opacity = '.45' }
    modalEliminar.classList.add('is-open')
  })
  document.getElementById('btn-cancelar-eliminar')?.addEventListener('click', () => modalEliminar.classList.remove('is-open'))
  inputConfirmar?.addEventListener('input', actualizarBotonEliminar)
  inputPasswordEliminar?.addEventListener('input', actualizarBotonEliminar)
  modalEliminar?.addEventListener('click', (e) => { if (e.target === modalEliminar) modalEliminar.classList.remove('is-open') })

  /* ── modal: cambiar correo ── */
  const modalCorreo = document.getElementById('modal-correo')
  document.getElementById('btn-cambiar-correo')?.addEventListener('click', () => {
    document.getElementById('input-nuevo-correo').value = ''
    document.getElementById('input-password-correo').value = ''
    document.getElementById('correo-error').style.display = 'none'
    modalCorreo.classList.add('is-open')
  })
  const cerrarCorreo = () => modalCorreo.classList.remove('is-open')
  document.getElementById('btn-cancelar-correo')?.addEventListener('click', cerrarCorreo)
  document.getElementById('btn-cerrar-correo')?.addEventListener('click', cerrarCorreo)
  modalCorreo?.addEventListener('click', (e) => { if (e.target === modalCorreo) cerrarCorreo() })

  document.getElementById('btn-guardar-correo')?.addEventListener('click', async () => {
    const nuevoCorreo = document.getElementById('input-nuevo-correo').value.trim()
    const password = document.getElementById('input-password-correo').value
    const errEl = document.getElementById('correo-error')
    if (!nuevoCorreo || !nuevoCorreo.includes('@')) { errEl.textContent = 'Ingresa un correo válido.'; errEl.style.display = 'block'; return }
    if (!password) { errEl.textContent = 'Ingresa tu contraseña actual.'; errEl.style.display = 'block'; return }
    const btn = document.getElementById('btn-guardar-correo')
    btn.textContent = 'Guardando…'; btn.disabled = true
    try {
      const res = await fetch('/ajustes/cambiarCorreo', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nuevoCorreo, password })
      })
      const data = await res.json()
      if (data.ok) {
        cerrarCorreo()
        if (window.UIFeedback?.toast) window.UIFeedback.toast('Correo actualizado correctamente', 'success')
        setTimeout(() => window.location.reload(), 1200)
      } else {
        errEl.textContent = data.mensaje || 'Error al actualizar el correo.'; errEl.style.display = 'block'
      }
    } catch {
      errEl.textContent = 'Error de conexión. Intenta de nuevo.'; errEl.style.display = 'block'
    } finally {
      btn.textContent = 'Guardar correo'; btn.disabled = false
    }
  })

  /* ── notificaciones del sistema: marcar todas como leídas ── */
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
    initThemeSwatches()
    initDensity()
    initTextSize()
    initNotif()
    initAccesibilidad()
    updatePrivacyPanel()
    updateYaml()
  })
})()
