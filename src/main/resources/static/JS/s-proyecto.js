let paso = 0;
let coverActual = 'cover-ocean';
let iconSVGHtml = document.querySelector('.icon-opt.sel svg').outerHTML;
let iconKey = document.querySelector('.icon-opt.sel').dataset.key;
let iconColor = '#1f8f8f';
let logoSubido = false;
let bannerSubido = false;
let bannerDataUrl = '';
let logoDataUrl = '';
const stackTags = [];
const extraTags = [];

const panels = document.querySelectorAll('.paso-panel');
const stepNums = document.querySelectorAll('.step-item .step-num');
const stepLabels = document.querySelectorAll('.step-item .step-label');
const stepLines = document.querySelectorAll('.step-line');
const stepItems = document.querySelectorAll('.step-item');

function irAPaso(n) {
    paso = n;
    panels.forEach((p, i) => p.style.display = i === paso ? 'block' : 'none');

    stepNums.forEach((s, i) => {
        s.classList.remove('done', 'active');
        s.textContent = i < paso ? '✓' : i + 1;
        if (i < paso) s.classList.add('done');
        if (i === paso) s.classList.add('active');
    });

    stepLabels.forEach((l, i) => {
        l.classList.remove('done', 'active');
        if (i < paso) l.classList.add('done');
        if (i === paso) l.classList.add('active');
    });

    stepLines.forEach((l, i) => l.classList.toggle('done', i < paso));

    stepItems.forEach((item, i) => item.classList.toggle('clickable', i < paso));

    document.getElementById('side-preview').style.display = paso === 3 ? 'none' : '';
    if (paso === 3) construirResumen();

    actualizarPreviewLateral();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

stepItems.forEach((item, i) => {
    item.addEventListener('click', () => { if (i < paso) irAPaso(i); });
});

function validarPaso(n) {
    const reqs = panels[n].querySelectorAll('[required]');
    let ok = true;
    reqs.forEach(el => {
        el.classList.remove('error');
        if (!el.value.trim()) {
            el.classList.add('error');
            if (ok) el.focus();
            ok = false;
        }
    });
    if (n === 2 && stackTags.length === 0) {
        document.getElementById('stack-field').style.border = '1px solid #e53e3e';
        document.getElementById('stack-field').style.boxShadow = '0 0 0 3px rgba(229,62,62,0.12)';
        ok = false;
    }
    return ok;
}

document.querySelectorAll('.form-input, .form-select, .form-textarea').forEach(el => {
    el.addEventListener('input', () => {
        el.classList.remove('error');
        actualizarPreviewLateral();
    });
    el.addEventListener('change', actualizarPreviewLateral);
});

document.getElementById('sig-0').addEventListener('click', () => { if (validarPaso(0)) irAPaso(1); });
document.getElementById('at-1').addEventListener('click', () => irAPaso(0));
document.getElementById('sig-1').addEventListener('click', () => irAPaso(2));
document.getElementById('at-2').addEventListener('click', () => irAPaso(1));
document.getElementById('sig-2').addEventListener('click', () => { if (validarPaso(2)) irAPaso(3); });
document.getElementById('at-3').addEventListener('click', () => irAPaso(2));

function showProjectToast(message, type) {
    if (window.UIFeedback && typeof window.UIFeedback.toast === 'function') {
        window.UIFeedback.toast(message, type || 'info');
        return;
    }
    if (window.console && typeof window.console.info === 'function') {
        window.console.info(message);
    }
}

document.getElementById('btn-borrador').addEventListener('click', () => showProjectToast('Borrador guardado localmente.', 'success'));
document.getElementById('btn-borrador-2').addEventListener('click', () => showProjectToast('Borrador guardado localmente.', 'success'));

document.querySelectorAll('.banner-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.banner-opt').forEach(o => o.classList.remove('sel'));
        opt.classList.add('sel');
        coverActual = opt.dataset.cover;
        bannerSubido = false;
        document.getElementById('btn-quitar-banner').style.display = 'none';
        document.getElementById('banner-upload-label').textContent = 'Subir imagen de banner (opcional)';
        actualizarPreviewLateral();
    });
});

document.getElementById('banner-img-input').addEventListener('change', function () {
    if (!this.files[0]) return;
    const reader = new FileReader();
    reader.onload = e => {
        const sideBanner = document.getElementById('side-banner');
        const pvBanner = document.getElementById('pv-banner');
        sideBanner.style.backgroundImage = `url('${e.target.result}')`;
        sideBanner.style.backgroundSize = 'cover';
        sideBanner.style.backgroundPosition = 'center';
        pvBanner.style.backgroundImage = `url('${e.target.result}')`;
        pvBanner.style.backgroundSize = 'cover';
        pvBanner.style.backgroundPosition = 'center';
        bannerDataUrl = e.target.result;
        document.querySelectorAll('.banner-opt').forEach(o => o.classList.remove('sel'));
        document.getElementById('banner-upload-label').textContent = this.files[0].name;
        document.getElementById('btn-quitar-banner').style.display = 'block';
        bannerSubido = true;
    };
    reader.readAsDataURL(this.files[0]);
});

document.getElementById('btn-quitar-banner').addEventListener('click', () => {
    document.getElementById('banner-img-input').value = '';
    document.getElementById('banner-upload-label').textContent = 'Subir imagen de banner (opcional)';
    document.getElementById('btn-quitar-banner').style.display = 'none';
    bannerSubido = false;
    bannerDataUrl = '';
    const first = document.querySelector('.banner-opt');
    first.classList.add('sel');
    coverActual = first.dataset.cover;
    actualizarPreviewLateral();
});

document.querySelectorAll('.icon-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.icon-opt').forEach(o => o.classList.remove('sel'));
        opt.classList.add('sel');
        iconKey = opt.dataset.key;
        iconSVGHtml = opt.querySelector('svg').outerHTML;
        if (!logoSubido) actualizarIconPreview();
    });
});

document.querySelectorAll('.color-dot').forEach(dot => {
    dot.addEventListener('click', () => {
        document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('sel'));
        dot.classList.add('sel');
        iconColor = dot.dataset.color;
        if (!logoSubido) actualizarIconPreview();
    });
});

function actualizarIconPreview() {
    const prev = document.getElementById('icon-preview');
    prev.style.background = iconColor;
    prev.innerHTML = iconSVGHtml;
    actualizarPreviewLateral();
}

document.getElementById('logo-input').addEventListener('change', async function () {
    if (!this.files[0]) return;
    const file = this.files[0];

    const reader = new FileReader();
    reader.onload = e => {
        const prev = document.getElementById('icon-preview');
        prev.style.background = 'transparent';
        prev.innerHTML = `<img src="${e.target.result}" style="width:48px;height:48px;object-fit:cover;border-radius:10px;">`;
        actualizarPreviewLateral();
    };
    reader.readAsDataURL(file);

    try {
        document.getElementById('logo-upload-label').textContent = 'Subiendo...';
        const formData = new FormData();
        formData.append('file', file);

        const res = await fetch('/proyectos/logo/temp', { method: 'POST', body: formData });
        const data = await res.json();

        if (data.ok) {
            logoDataUrl = data.url;
            document.getElementById('logo-upload-label').textContent = file.name;
            document.getElementById('btn-quitar-logo').style.display = 'block';
            logoSubido = true;
            showProjectToast('Logo subido correctamente', 'success');
        } else {
            showProjectToast('Error al subir el logo', 'error');
            document.getElementById('logo-upload-label').textContent = 'Subir logo propio (opcional)';
        }
    } catch {
        showProjectToast('Error de conexión', 'error');
        document.getElementById('logo-upload-label').textContent = 'Subir logo propio (opcional)';
    }
});

document.getElementById('btn-quitar-logo').addEventListener('click', () => {
    document.getElementById('logo-input').value = '';
    document.getElementById('logo-upload-label').textContent = 'Subir logo propio (opcional)';
    document.getElementById('btn-quitar-logo').style.display = 'none';
    logoSubido = false;
    logoDataUrl = '';
    actualizarIconPreview();
});

function crearChip(texto, array, fieldId) {
    const chip = document.createElement('span');
    chip.className = 'tag-chip';
    chip.innerHTML = `${texto} <button class="tag-remove" type="button">×</button>`;
    chip.querySelector('.tag-remove').addEventListener('click', () => {
        const idx = array.indexOf(texto);
        if (idx > -1) array.splice(idx, 1);
        chip.remove();
        if (fieldId === 'stack-field') {
            document.getElementById('stack-field').style.border = '';
            document.getElementById('stack-field').style.boxShadow = '';
        }
        actualizarPreviewLateral();
    });
    return chip;
}

function setupTags(inputId, fieldId, array) {
    const input = document.getElementById(inputId);
    const field = document.getElementById(fieldId);
    field.addEventListener('click', () => input.focus());
    input.addEventListener('keydown', e => {
        if ((e.key === 'Enter' || e.key === ',') && input.value.trim()) {
            e.preventDefault();
            const val = input.value.trim().replace(/,$/, '');
            if (val && !array.includes(val)) {
                array.push(val);
                const chip = crearChip(val, array, fieldId);
                field.insertBefore(chip, input);
                if (fieldId === 'stack-field') {
                    field.style.border = '';
                    field.style.boxShadow = '';
                }
                actualizarPreviewLateral();
            }
            input.value = '';
        }
        if (e.key === 'Backspace' && !input.value && array.length) {
            array.pop();
            const chips = field.querySelectorAll('.tag-chip');
            if (chips.length) chips[chips.length - 1].remove();
            actualizarPreviewLateral();
        }
    });
}

setupTags('stack-input', 'stack-field', stackTags);
setupTags('tags-input', 'tags-field', extraTags);

function actualizarPreviewLateral() {
    const nombre = v('f-nombre') || 'Nombre del proyecto';
    const cat = v('f-categoria') || 'Categoria';
    const tipo = v('f-tipo') || 'Tipo';
    const desc = v('f-desc') || 'La descripcion aparecera aqui.';
    const anio = v('f-anio') || '2026';

    const sideBanner = document.getElementById('side-banner');
    if (!bannerSubido) {
        sideBanner.className = 'project-banner ' + coverActual;
        sideBanner.style.backgroundImage = '';
    }
    document.getElementById('side-titulo').textContent = nombre;
    document.getElementById('side-cat').textContent = cat;
    document.getElementById('side-tipo').textContent = tipo;
    document.getElementById('side-nombre').textContent = nombre;
    document.getElementById('side-meta').textContent = cat + ' · ' + anio;
    document.getElementById('side-desc').textContent = desc.slice(0, 100) + (desc.length > 100 ? '...' : '');

    const sideLogo = document.getElementById('side-logo');
    if (logoSubido) {
        const img = document.querySelector('#icon-preview img');
        if (img) {
            sideLogo.innerHTML = `<img src="${img.src}" style="width:30px;height:30px;object-fit:cover;border-radius:8px;">`;
            sideLogo.style.background = 'transparent';
        }
    } else {
        sideLogo.style.background = iconColor;
        sideLogo.style.color = '#fff';
        sideLogo.innerHTML = iconSVGHtml;
        const svgEl = sideLogo.querySelector('svg');
        if (svgEl) { svgEl.style.width = '18px'; svgEl.style.height = '18px'; }
    }

    const sideStack = document.getElementById('side-stack');
    sideStack.innerHTML = '';
    stackTags.slice(0, 4).forEach(t => {
        const s = document.createElement('span');
        s.className = 'chip';
        s.textContent = t;
        sideStack.appendChild(s);
    });
}

function construirResumen() {
    const nombre = v('f-nombre') || '—';
    const cat = v('f-categoria') || '';
    const tipo = v('f-tipo') || '';
    const desc = v('f-desc') || 'Sin descripcion.';
    const anio = v('f-anio') || '2026';

    document.getElementById('rv-nombre').textContent = nombre;
    document.getElementById('rv-cat').textContent = [cat, tipo].filter(Boolean).join(' · ') || '—';

    const pub = document.getElementById('t-publicar').checked;
    const cv = document.getElementById('t-cv').checked;
    const dest = document.getElementById('t-destacar').checked;
    const visPartes = [];
    if (pub) visPartes.push('Publicado');
    if (cv) visPartes.push('En CV');
    if (dest) visPartes.push('Destacado');
    document.getElementById('rv-vis').textContent = visPartes.length ? visPartes.join(' · ') : 'Privado';

    const rvStack = document.getElementById('rv-stack');
    rvStack.innerHTML = '';
    if (stackTags.length) {
        stackTags.forEach(t => {
            const s = document.createElement('span');
            s.className = 'chip';
            s.textContent = t;
            rvStack.appendChild(s);
        });
    } else {
        rvStack.innerHTML = '<span class="muted">—</span>';
    }

    const pvBanner = document.getElementById('pv-banner');
    if (!bannerSubido) {
        pvBanner.className = 'project-banner ' + coverActual;
        pvBanner.style.backgroundImage = '';
    }
    document.getElementById('pv-banner-titulo').textContent = nombre;
    document.getElementById('pv-cat-tag').textContent = cat || 'Categoria';
    document.getElementById('pv-tipo-tag').textContent = tipo || 'Tipo';
    document.getElementById('pv-anio').textContent = anio;
    document.getElementById('pv-titulo').textContent = nombre;
    document.getElementById('pv-meta').textContent = [cat, tipo, anio].filter(Boolean).join(' · ');
    document.getElementById('pv-desc').textContent = desc;
    document.getElementById('pv-estado').textContent = pub ? 'Publicado' : 'Borrador';
    document.getElementById('pv-estado').className = 'status-pill ' + (pub ? 'status-live' : 'status-draft');

    const pvLogo = document.getElementById('pv-logo');
    if (logoSubido) {
        const img = document.querySelector('#icon-preview img');
        if (img) {
            pvLogo.innerHTML = `<img src="${img.src}" style="width:38px;height:38px;object-fit:cover;border-radius:10px;">`;
            pvLogo.style.background = 'transparent';
        }
    } else {
        pvLogo.style.background = iconColor;
        pvLogo.style.color = '#fff';
        pvLogo.innerHTML = iconSVGHtml;
        const svgEl = pvLogo.querySelector('svg');
        if (svgEl) { svgEl.style.width = '22px'; svgEl.style.height = '22px'; }
    }

    const pvStack = document.getElementById('pv-stack');
    pvStack.innerHTML = '';
    stackTags.slice(0, 5).forEach(t => {
        const s = document.createElement('span');
        s.className = 'chip';
        s.textContent = t;
        pvStack.appendChild(s);
    });

    const scores = [
        { label: 'Nombre y categoria', val: (nombre !== '—' && cat) ? 100 : (nombre !== '—' ? 50 : 0) },
        { label: 'Descripcion corta', val: v('f-desc').length > 30 ? 100 : v('f-desc').length > 0 ? 50 : 0 },
        { label: 'Stack definido', val: stackTags.length > 0 ? 100 : 0 },
        { label: 'Descripcion extendida', val: v('f-desc2').length > 30 ? 100 : 0 },
        { label: 'Banner personalizado', val: (bannerSubido || coverActual !== 'cover-ocean') ? 100 : 50 },
    ];

    const scoreWrap = document.getElementById('score-wrap');
    scoreWrap.innerHTML = '';
    scores.forEach(s => {
        const cls = s.val === 100 ? 'hi' : s.val >= 50 ? 'mid' : 'lo';
        const fillStyle = s.val < 50 ? 'background:#ea580c;' : '';
        scoreWrap.innerHTML += `
            <div class="score-item">
                <div class="score-meta">
                    <span>${s.label}</span>
                    <span class="score-pct ${cls}">${s.val}%</span>
                </div>
                <div class="progress-track">
                    <div class="progress-fill score-fill ${cls}" style="width:${s.val}%;${fillStyle}"></div>
                </div>
            </div>`;
    });

    const pendientes = scores.filter(s => s.val < 100).map(s => s.label);
    document.getElementById('score-tip').textContent = pendientes.length
        ? `Pendiente: ${pendientes.join(', ')}.`
        : '¡Todo completo! Tu proyecto tiene maxima visibilidad.';
}

function v(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}

function setHidden(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value || '';
}

function enviarProyecto(event) {
    const paso0Valido = validarPaso(0);
    const paso2Valido = validarPaso(2);

    if (!paso0Valido || !paso2Valido) {
        if (event) event.preventDefault();
        irAPaso(paso0Valido ? 2 : 0);
        return false;
    }

    setHidden('h-nombre', v('f-nombre'));
    setHidden('h-categoria', v('f-categoria'));
    setHidden('h-tipo', v('f-tipo'));
    setHidden('h-descripcion', v('f-desc'));
    setHidden('h-anio', v('f-anio'));
    setHidden('h-link', v('f-link'));
    setHidden('h-banner-clase', coverActual);
    setHidden('h-banner-url', bannerSubido ? bannerDataUrl : '');
    setHidden('h-icono-clave', iconKey || 'layers');
    setHidden('h-color-icono', iconColor || '#1f8f8f');
    setHidden('h-logo-url', logoSubido ? logoDataUrl : '');
    setHidden('h-stack', stackTags.join(','));
    setHidden('h-etiquetas', extraTags.join(','));
    setHidden('h-rol', v('f-rol'));
    setHidden('h-duracion', v('f-duracion'));
    setHidden('h-desc2', v('f-desc2'));
    setHidden('h-publicar', document.getElementById('t-publicar').checked ? 'true' : 'false');
    setHidden('h-cv', document.getElementById('t-cv').checked ? 'true' : 'false');
    setHidden('h-destacar', document.getElementById('t-destacar').checked ? 'true' : 'false');
    return true;
}

const form = document.getElementById('proyecto-form');
if (form) {
    form.addEventListener('submit', (event) => {
        enviarProyecto(event);
    });
}

const btnPublicar = document.getElementById('btn-publicar');
if (btnPublicar && form) {
    btnPublicar.addEventListener('click', (event) => {
        event.preventDefault();
        if (enviarProyecto()) form.submit();
    });
}

actualizarIconPreview();
irAPaso(0);