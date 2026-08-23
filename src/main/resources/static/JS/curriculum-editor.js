const REMOVE_ICON_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
const langState = { es: NIVEL_ES_INICIAL, en: NIVEL_EN_INICIAL };
const LANG_LABELS    = { 1:'A1 Básico', 2:'A2 Elemental', 3:'B1 Intermedio', 4:'B2 Conversacional', 5:'Nativo' };
const LANG_LABELS_EN = { 1:'A1 Básico', 2:'A2 Elemental', 3:'B1 Intermedio', 4:'B2 Conversacional', 5:'C1/C2' };
let softSkills = [...SOFT_SKILLS_INICIALES];

/* ─── Idiomas ─────────────────────────────────────────────── */
function updateLangBars(lang, level) {
    const barsEl = document.getElementById('bars-' + lang);
    const labelEl = document.getElementById('label-' + lang);
    if (!barsEl || !labelEl) return;
    barsEl.querySelectorAll('.cv-lang-bar').forEach((bar, i) => {
        bar.classList.remove('fill', 'fill-full');
        if (i < level) bar.classList.add(level === 5 && lang === 'es' ? 'fill-full' : 'fill');
    });
    labelEl.textContent = lang === 'es' ? LANG_LABELS[level] : LANG_LABELS_EN[level];
}
function initLangButtons() {
    ['es', 'en'].forEach(lang => {
        const level = langState[lang];
        document.querySelector(`.lang-btn[data-lang="${lang}"][data-level="${level}"]`)?.classList.add('selected');
    });
    document.querySelectorAll('.lang-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const lang  = btn.dataset.lang;
            const level = parseInt(btn.dataset.level);
            langState[lang] = level;
            document.querySelectorAll(`.lang-btn[data-lang="${lang}"]`).forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            updateLangBars(lang, level);
        });
    });
}

/* ─── Soft skills chips ────────────────────────────────────── */
function renderSoftChips() {
    const wrapper = document.getElementById('soft-chips-wrapper');
    const input   = document.getElementById('soft-chip-input');
    wrapper.querySelectorAll('.chip-tag').forEach(c => c.remove());
    softSkills.forEach((skill, i) => {
        const chip = document.createElement('span');
        chip.className = 'chip-tag';
        chip.innerHTML = `${skill} <button onclick="removeChip(${i})" type="button" style="display:inline-flex;align-items:center;">${REMOVE_ICON_SVG}</button>`;
        wrapper.insertBefore(chip, input);
    });
}
function removeChip(idx) { softSkills.splice(idx, 1); renderSoftChips(); }

document.getElementById('soft-chip-input').addEventListener('keydown', function(e) {
    if (e.key === 'Enter' || e.key === ',') {
        e.preventDefault();
        const val = this.value.trim().replace(/,$/, '');
        if (val && !softSkills.includes(val)) { softSkills.push(val); renderSoftChips(); }
        this.value = '';
    }
});
document.getElementById('soft-chips-wrapper').addEventListener('click', () =>
    document.getElementById('soft-chip-input').focus());

/* ─── Tabs del modal ───────────────────────────────────────── */
document.querySelectorAll('.edit-tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.edit-tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.edit-panel').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById('panel-' + tab.dataset.tab).classList.add('active');
    });
});

/* ─── Preferencias chips ───────────────────────────────────── */
function initPrefChips() {
    const normalize = val => (val || '').toString().toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[-_]/g, ' ').trim();
    const modalidadInicial = (MODALIDAD_INICIAL || '').split(',').map(v => v.trim()).filter(Boolean);
    const disponibInicial  = (DISPONIB_INICIAL  || '').split(',').map(v => v.trim()).filter(Boolean);
    const modalidadNorm = modalidadInicial.map(normalize);
    const disponibNorm  = disponibInicial.map(normalize);
    document.querySelectorAll('#pref-modalidad .pref-chip-toggle').forEach(btn => {
        if (modalidadNorm.includes(normalize(btn.dataset.val))) btn.classList.add('selected');
        btn.addEventListener('click', () => btn.classList.toggle('selected'));
    });
    document.querySelectorAll('#pref-contrato .pref-chip-toggle').forEach(btn => {
        if (disponibNorm.includes(normalize(btn.dataset.val))) btn.classList.add('selected');
        btn.addEventListener('click', () => btn.classList.toggle('selected'));
    });
}

/* ─── Experiencias ─────────────────────────────────────────── */
function removeExpItem(btn) {
    btn.closest('.edit-timeline-item').remove();
    document.querySelectorAll('#exp-items-container .edit-timeline-item').forEach((item, i) => {
        item.querySelector('.edit-item-label').textContent = `Experiencia ${i + 1}`;
    });
}
document.getElementById('btn-add-exp').addEventListener('click', () => {
    const container = document.getElementById('exp-items-container');
    const idx = container.querySelectorAll('.edit-timeline-item').length;
    const div = document.createElement('div');
    div.className = 'edit-timeline-item';
    div.innerHTML = `
        <button class="btn-remove-item" onclick="removeExpItem(this)" title="Eliminar">${REMOVE_ICON_SVG}</button>
        <div class="edit-item-label">Experiencia ${idx + 1}</div>
        <div class="edit-grid-2">
            <div class="edit-field"><label>Cargo</label><input type="text" class="exp-cargo" placeholder="Tu cargo"></div>
            <div class="edit-field"><label>Empresa</label><input type="text" class="exp-empresa" placeholder="Nombre empresa"></div>
        </div>
        <div class="edit-grid-2">
            <div class="edit-field"><label>Período</label><input type="text" class="exp-periodo" placeholder="2025 – hoy"></div>
            <div class="edit-field"><label>Modalidad</label><input type="text" class="exp-modalidad" placeholder="Remoto / Híbrido"></div>
        </div>
        <div class="edit-field"><label>Descripción</label><textarea class="exp-desc" rows="2" placeholder="Describe tus responsabilidades..."></textarea></div>`;
    container.appendChild(div);
});

/* ─── Educaciones ──────────────────────────────────────────── */
function removeEduItem(btn) {
    btn.closest('.edit-timeline-item').remove();
    document.querySelectorAll('#edu-items-container .edit-timeline-item').forEach((item, i) => {
        item.querySelector('.edit-item-label').textContent = `Educación ${i + 1}`;
    });
}
document.getElementById('btn-add-edu').addEventListener('click', () => {
    const container = document.getElementById('edu-items-container');
    const idx = container.querySelectorAll('.edit-timeline-item').length;
    const div = document.createElement('div');
    div.className = 'edit-timeline-item';
    div.innerHTML = `
        <button class="btn-remove-item" onclick="removeEduItem(this)" title="Eliminar">${REMOVE_ICON_SVG}</button>
        <div class="edit-item-label">Educación ${idx + 1}</div>
        <div class="edit-grid-2">
            <div class="edit-field"><label>Institución</label><input type="text" class="edu-institucion" placeholder="Universidad / Bootcamp"></div>
            <div class="edit-field"><label>Título / Carrera</label><input type="text" class="edu-titulo" placeholder="Ingeniería de Sistemas"></div>
        </div>
        <div class="edit-grid-2">
            <div class="edit-field"><label>Período</label><input type="text" class="edu-periodo" placeholder="2020 – 2024"></div>
            <div class="edit-field"><label>Descripción (opcional)</label><input type="text" class="edu-descripcion" placeholder="Especialización en..."></div>
        </div>`;
    container.appendChild(div);
});

/* ─── Modal open/close ─────────────────────────────────────── */
function openModal(tabName) {
    document.getElementById('edit-overlay').classList.add('is-open');
    document.body.style.overflow = 'hidden';
    if (tabName) {
        document.querySelectorAll('.edit-tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tabName));
        document.querySelectorAll('.edit-panel').forEach(p => p.classList.toggle('active', p.id === 'panel-' + tabName));
    }
    renderSoftChips();
}
function closeModal() {
    document.getElementById('edit-overlay').classList.remove('is-open');
    document.body.style.overflow = '';
}

document.getElementById('btn-editar-secciones').addEventListener('click', () => openModal('perfil'));
document.getElementById('btn-cerrar-modal').addEventListener('click', closeModal);
document.getElementById('btn-cancelar-modal').addEventListener('click', closeModal);
document.getElementById('edit-overlay').addEventListener('click', function(e) { if (e.target === this) closeModal(); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });
document.getElementById('link-editar-bio')?.addEventListener('click', e => { e.preventDefault(); openModal('perfil'); });

/* ─── Toast ────────────────────────────────────────────────── */
function showToast(msg, type = 'ok') {
    if (window.UIFeedback?.toast) { window.UIFeedback.toast(msg, type); return; }
    const t = document.getElementById('toast');
    if (!t) return;
    t.textContent = msg;
    t.className = `toast ${type} show`;
    setTimeout(() => t.classList.remove('show'), 3000);
}

/* ─── Bio char counter ─────────────────────────────────────── */
const bioInput   = document.getElementById('inp-bio');
const bioCounter = document.getElementById('bio-char-counter');
const BIO_MAX    = 400;
function updateBioCounter() {
    if (!bioInput || !bioCounter) return;
    const left = BIO_MAX - bioInput.value.length;
    bioCounter.textContent = left + ' caracteres restantes';
    bioCounter.classList.toggle('near-limit', left <= 80 && left > 20);
    bioCounter.classList.toggle('at-limit',   left <= 20);
}
bioInput?.addEventListener('input', updateBioCounter);
updateBioCounter();

/* ─── Guardar ──────────────────────────────────────────────── */
document.getElementById('btn-guardar-modal').addEventListener('click', async () => {
    const btn = document.getElementById('btn-guardar-modal');
    btn.classList.add('btn-saving');
    btn.textContent = 'Guardando...';

    const bulletsRaw  = document.getElementById('inp-bullets').value.trim();
    const cvBullets   = bulletsRaw ? bulletsRaw.split('\n').map(l => l.trim()).filter(Boolean) : [];

    const cvExperiencias = [];
    document.querySelectorAll('#exp-items-container .edit-timeline-item').forEach(item => {
        const cargo   = item.querySelector('.exp-cargo')?.value.trim() || '';
        const empresa = item.querySelector('.exp-empresa')?.value.trim() || '';
        if (!cargo && !empresa) return;
        cvExperiencias.push({
            cargo, empresa,
            periodo:    item.querySelector('.exp-periodo')?.value.trim()   || '',
            modalidad:  item.querySelector('.exp-modalidad')?.value.trim() || '',
            descripcion:item.querySelector('.exp-desc')?.value.trim()      || ''
        });
    });

    const cvEducaciones = [];
    document.querySelectorAll('#edu-items-container .edit-timeline-item').forEach(item => {
        const institucion = item.querySelector('.edu-institucion')?.value.trim() || '';
        const titulo      = item.querySelector('.edu-titulo')?.value.trim()      || '';
        if (!institucion && !titulo) return;
        cvEducaciones.push({
            institucion, titulo,
            periodo:     item.querySelector('.edu-periodo')?.value.trim()     || '',
            descripcion: item.querySelector('.edu-descripcion')?.value.trim() || ''
        });
    });

    const modalidadSel = [...document.querySelectorAll('#pref-modalidad .pref-chip-toggle.selected')].map(b => b.dataset.val).join(', ');
    const disponibSel  = [...document.querySelectorAll('#pref-contrato .pref-chip-toggle.selected')].map(b => b.dataset.val).join(', ');

    const payload = {
        nombre:         document.getElementById('inp-nombre')?.value.trim()     || '',
        rolTitulo:      document.getElementById('inp-rol')?.value.trim()         || '',
        bio:            document.getElementById('inp-bio')?.value.trim()         || '',
        cvBullets,
        emailContacto:  document.getElementById('inp-email')?.value.trim()       || '',
        telefono:       document.getElementById('inp-telefono')?.value.trim()    || '',
        portfolioUrl:   document.getElementById('inp-portafolio')?.value.trim()  || '',
        linkedinUrl:    document.getElementById('inp-linkedin')?.value.trim()    || '',
        githubUrl:      document.getElementById('inp-github')?.value.trim()      || '',
        ubicacion:      document.getElementById('inp-ubicacion')?.value.trim()   || '',
        modalidad:      modalidadSel,
        disponibilidad: disponibSel,
        salarioEsperado:document.getElementById('inp-salario')?.value.trim()     || '',
        especialidad:   document.getElementById('inp-especialidad')?.value.trim()|| '',
        nivelEspanol:   langState.es,
        nivelIngles:    langState.en,
        habilidadesBlandas: softSkills,
        cvExperiencias,
        cvEducaciones
    };

    try {
        const res  = await fetch('/curriculum/guardar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.ok) {
            actualizarVistaCv(payload);
            closeModal();
            showToast('✓ Cambios guardados', 'ok');
        } else {
            showToast('Error: ' + (data.error || 'No se pudo guardar'), 'err');
        }
    } catch {
        showToast('Error de red. Intenta de nuevo.', 'err');
    } finally {
        btn.classList.remove('btn-saving');
        btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> Guardar cambios`;
    }
});

/* ─── Actualizar vista del CV sin recargar ─────────────────── */
function actualizarVistaCv(p) {
    const set = (id, val) => { const el = document.getElementById(id); if (el && val != null) el.textContent = val; };
    const setLink = (id, url, display) => {
        const el = document.getElementById(id);
        if (!el) return;
        el.href = url && url.trim() ? url : '#';
        const span = el.querySelector('span');
        if (span) span.textContent = display && display.trim() ? display : el.querySelector('span')?.textContent || '';
    };

    if (p.nombre) { set('cv-nombre', p.nombre); set('cv-avatar', p.nombre.charAt(0).toUpperCase()); }
    set('cv-rol', p.rolTitulo);
    set('cv-bio', p.bio);
    set('cv-email', p.emailContacto);
    set('cv-telefono', p.telefono);
    set('cv-portafolio', p.portfolioUrl);
    set('cv-ubicacion', p.ubicacion);

    const modalidadChip = document.getElementById('cv-modalidad-chip');
    if (modalidadChip) modalidadChip.textContent = p.modalidad?.trim() || 'Remoto';
    const disponibilidadChip = document.getElementById('cv-disponibilidad-chip');
    if (disponibilidadChip) disponibilidadChip.textContent = p.disponibilidad?.trim() || 'Full-time';
    const availabilityText = document.getElementById('cv-availability-text');
    if (availabilityText) availabilityText.textContent = p.modalidad?.trim() ? `Abierto a ofertas ${p.modalidad}` : 'Abierto a ofertas remotas';

    setLink('cv-linkedin-link', p.linkedinUrl, p.linkedinUrl);
    setLink('cv-portfolio-link', p.portfolioUrl, p.portfolioUrl || 'Portafolio');

    // GitHub
    const ghLink = document.getElementById('cv-github-link');
    const ghText = document.getElementById('cv-github-text');
    if (ghLink) {
        ghLink.href  = p.githubUrl?.trim() ? p.githubUrl : '#';
        ghLink.style.opacity = p.githubUrl?.trim() ? '' : '0.4';
        ghLink.style.pointerEvents = p.githubUrl?.trim() ? '' : 'none';
    }
    if (ghText) ghText.textContent = p.githubUrl?.trim() ? p.githubUrl : 'GitHub';

    // Bullets
    const bulletsList = document.getElementById('cv-bullets-list');
    if (bulletsList) {
        bulletsList.innerHTML = p.cvBullets?.length
            ? p.cvBullets.map(b => `<li>${b}</li>`).join('')
            : '<li>Agrega tus logros principales en Editar secciones.</li>';
    }

    // Experiencias
    const expDisplay = document.getElementById('cv-experiencia-display');
    if (expDisplay) {
        expDisplay.innerHTML = p.cvExperiencias?.length
            ? p.cvExperiencias.map((exp, i) => `
                <div class="cv-timeline-item">
                    <span class="cv-timeline-dot ${i === 0 ? 'is-active' : ''}"></span>
                    <div>
                        <strong>${exp.cargo || '—'}</strong><span style="color:var(--accent);font-weight:600;"> · ${exp.empresa || '—'}</span>
                        <div class="chip-row" style="margin:6px 0;">
                            ${i === 0 ? '<span class="badge-open">Actual</span>' : ''}
                            ${exp.periodo   ? `<span class="chip">${exp.periodo}</span>`   : ''}
                            ${exp.modalidad ? `<span class="chip">${exp.modalidad}</span>` : ''}
                        </div>
                        ${exp.descripcion ? `<p class="muted" style="font-size:.85rem;">${exp.descripcion}</p>` : ''}
                    </div>
                </div>`).join('')
            : '<p class="muted" style="font-size:.85rem;">Agrega tu experiencia en Editar secciones.</p>';
    }

    // Educaciones
    const eduDisplay = document.getElementById('cv-educacion-display');
    if (eduDisplay) {
        eduDisplay.innerHTML = p.cvEducaciones?.length
            ? p.cvEducaciones.map(edu => `
                <div class="resume-row">
                    <strong>${edu.institucion || '—'}</strong>
                    <span style="color:var(--accent);font-weight:600;">${edu.titulo || '—'}</span>
                    ${edu.periodo    ? `<span class="muted">${edu.periodo}</span>` : ''}
                    ${edu.descripcion? `<p class="muted" style="font-size:.82rem;margin:2px 0 0;">${edu.descripcion}</p>` : ''}
                </div>`).join('')
            : '<p class="muted" style="font-size:.85rem;">Agrega tu educación en Editar secciones.</p>';
    }

    // Especialidad y salario
    const espDisplay = document.getElementById('cv-especialidad-display');
    if (espDisplay) {
        espDisplay.textContent = p.especialidad || '';
        espDisplay.style.display = p.especialidad?.trim() ? '' : 'none';
    }
    const salDisplay = document.getElementById('cv-salario-display');
    const salPill = document.getElementById('cv-salario-pill');
    if (salDisplay) {
        salDisplay.textContent = p.salarioEsperado || '';
    }
    if (salPill) {
        salPill.style.display = p.salarioEsperado?.trim() ? 'inline-flex' : 'none';
    }

    // Soft skills
    const softWrap = document.getElementById('cv-soft-skills');
    if (softWrap) {
        softWrap.innerHTML = softSkills.length
            ? softSkills.map(s => `<span class="pill-soft">${s}</span>`).join('')
            : '<span class="pill-soft">Liderazgo</span><span class="pill-soft">Trabajo en equipo</span>';
    }

    // Idiomas
    updateLangBars('es', langState.es);
    updateLangBars('en', langState.en);

    // Stats y completitud (estimación client-side post-guardado)
    const newExp  = p.cvExperiencias?.length || 0;
    const newSkills = document.querySelectorAll('.stack-chip, .skill-chip').length || STAT_SKILLS;
    set('cv-stat-exp', newExp);

    const fieldsLlenos = [
        p.nombre, p.rolTitulo, p.bio, p.emailContacto, p.ubicacion,
        p.modalidad, p.portfolioUrl, p.linkedinUrl, p.githubUrl
    ].filter(v => v?.trim()).length
        + (newExp > 0 ? 1 : 0)
        + (p.cvEducaciones?.length > 0 ? 1 : 0);
    const nuevaComp = Math.min(100, Math.round((fieldsLlenos / 12) * 100));

    const fillEl  = document.getElementById('cv-completeness-fill');
    const pctEl   = document.getElementById('cv-completeness-pct');
    const pillEl  = document.getElementById('cv-completeness-pill');
    const statEl  = document.getElementById('cv-stat-completeness');
    if (fillEl)  fillEl.style.width   = nuevaComp + '%';
    if (pctEl)   pctEl.textContent    = nuevaComp + '%';
    if (pillEl)  pillEl.textContent   = nuevaComp + '%';
    if (statEl)  statEl.textContent   = nuevaComp + '%';
}

/* ─── Descargar PDF (print) ────────────────────────────────── */
document.getElementById('btn-descargar-pdf')?.addEventListener('click', () => {
    document.title = document.getElementById('cv-nombre')?.textContent?.trim()
        ? document.getElementById('cv-nombre').textContent.trim() + ' – CV'
        : 'CV DevCoast';
    window.print();
});

/* ─── Init ─────────────────────────────────────────────────── */
initLangButtons();
initPrefChips();
renderSoftChips();
updateLangBars('es', langState.es);
updateLangBars('en', langState.en);
