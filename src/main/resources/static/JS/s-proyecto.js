let paso = 0;
// el banner sí tiene selector (.banner-opt) y define qué dibujito se
// pinta en la card (ver pintarCard/STYLE_BUILDERS en proyectos-render.js)
// — el color de acento sigue siendo aparte; el ícono clásico (forma SVG)
// ya no tiene selector — se sacó por completo, así que queda fijo en su
// valor por defecto; el guardado real preserva el que ya tenía el
// proyecto vía la precarga de edición más abajo
let coverActual = 'cover-terminal';
let iconSVGHtml = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>';
let iconKey = 'layers';
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

    // la vista previa (canvas código/diseño, resumen) es cosmética — si algo
    // ahí revienta, no puede impedir que el paso realmente cambie de página
    try {
        if (paso === 3) construirResumen();
        actualizarPreviewLateral();
    } catch (err) {
        console.error('Error pintando la vista previa en el paso ' + paso + ':', err);
    }

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
    if (window.DevCoast && typeof window.DevCoast.toast === 'function') {
        window.DevCoast.toast(message);
        return;
    }
    if (window.UIFeedback && typeof window.UIFeedback.toast === 'function') {
        window.UIFeedback.toast(message, type || 'info');
        return;
    }
    if (window.console && typeof window.console.info === 'function') {
        window.console.info(message);
    }
}

document.getElementById('btn-borrador')?.addEventListener('click', () => showProjectToast('Borrador guardado localmente.', 'success'));
document.getElementById('btn-borrador-2').addEventListener('click', () => showProjectToast('Borrador guardado localmente.', 'success'));

document.querySelectorAll('.banner-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.banner-opt').forEach(o => o.classList.remove('sel'));
        opt.classList.add('sel');
        coverActual = opt.dataset.cover;
        bannerSubido = false;
        document.getElementById('btn-quitar-banner').style.display = 'none';
        document.getElementById('banner-upload-label').textContent = 'Subir imagen de banner (opcional)';
        // el banner elegido define qué dibujito se pinta en la card
        // (y su mini vista previa acá) — ver pintarCard()
        actualizarPreviewLateral();
    });
});

document.getElementById('banner-img-input').addEventListener('change', function () {
    if (!this.files[0]) return;
    const reader = new FileReader();
    reader.onload = e => {
        // la imagen subida solo se ve en la mini vista previa y en "Mis
        // proyectos" — el dibujito de la card sigue siendo el del último
        // banner de degradado elegido (coverActual no cambia acá)
        bannerDataUrl = e.target.result;
        document.querySelectorAll('.banner-opt').forEach(o => o.classList.remove('sel'));
        document.getElementById('banner-upload-label').textContent = this.files[0].name;
        document.getElementById('btn-quitar-banner').style.display = 'block';
        bannerSubido = true;
        actualizarPreviewLateral();
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
    pintarEstiloTiles();
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
            document.getElementById('logo-upload-label').textContent = 'Subir ícono personalizado (opcional)';
        }
    } catch {
        showProjectToast('Error de conexión', 'error');
        document.getElementById('logo-upload-label').textContent = 'Subir ícono personalizado (opcional)';
    }
});

document.getElementById('btn-quitar-logo').addEventListener('click', () => {
    document.getElementById('logo-input').value = '';
    document.getElementById('logo-upload-label').textContent = 'Subir ícono personalizado (opcional)';
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

// pinta el mismo componente .proj-card que usa la vitrina real (canvas
// del Estilo elegido, avatar, swatches, tags) — comparte el motor con
// proyectos-vitrina.js vía window.ProyectosRender en vez de tener acá una
// vista previa "aparte" que pueda desincronizarse de cómo se ve de verdad
function pintarCard(prefix) {
  try {
    const PR = window.ProyectosRender;
    const card = document.getElementById(prefix + '-card');
    if (!PR || !card) return;

    const nombre = v('f-nombre') || 'Nombre del proyecto';
    const cat = v('f-categoria');
    const tipo = v('f-tipo');
    // el color de acento tiñe tanto el dibujito código/diseño como el
    // avatar de la card — el Estilo (banner) define el patrón/dibujo, no
    // el color
    const accent = iconColor || '#1f8f8f';
    const ink = PR.inkFor(accent);
    const seed = PR.hashStr(nombre || 'preview');

    card.style.setProperty('--card-accent', accent);
    card.style.setProperty('--card-ink', ink);

    const built = PR.buildCanvas({ nombre, categoria: cat, tipo, stack: stackTags, accent, seed, estilo: coverActual });
    const canvas = document.getElementById(prefix + '-canvas');
    if (canvas) {
        canvas.className = 'proj-canvas ' + built.modeClass;
        canvas.innerHTML = built.bar + built.stage;
    }

    const avatar = document.getElementById(prefix + '-avatar');
    if (avatar) {
        avatar.className = 'proj-avatar' + (built.shapeClass ? ' ' + built.shapeClass : '');
        if (logoSubido) {
            const img = document.querySelector('#icon-preview img');
            avatar.innerHTML = img ? `<img src="${img.src}" alt="">` : '<span>D</span>';
        } else {
            const inicial = nombre.trim().charAt(0).toUpperCase() || 'D';
            avatar.innerHTML = `<span>${inicial}</span>`;
        }
    }

    const swatches = document.getElementById(prefix + '-swatches');
    if (swatches) swatches.innerHTML = PR.buildSwatchesHTML(accent);

    const tags = document.getElementById(prefix + '-tags') || document.getElementById(prefix + '-stack');
    if (tags) tags.innerHTML = PR.buildTagsHTML(stackTags, extraTags);

    const modeBadge = document.getElementById(prefix + '-mode-badge');
    if (modeBadge) {
        modeBadge.className = 'proj-title-disc ' + built.modeClass.replace('mode-', 'is-');
        modeBadge.textContent = built.discLabel;
    }
  } catch (err) {
    // la vista previa es cosmética — un error acá nunca debe trabar el
    // wizard (guardar/avanzar/atrás tienen que seguir funcionando siempre)
    console.error('pintarCard(' + prefix + ') falló:', err);
  }
}

// pinta una miniatura real de cada Estilo del picker (con PR.buildCanvas,
// semilla fija por estilo — no por nombre, para que el tile no cambie de
// forma mientras escribís) en vez de mostrar solo un chip de color plano
function pintarEstiloTiles() {
    const PR = window.ProyectosRender;
    if (!PR) return;
    const accent = iconColor || '#1f8f8f';
    document.querySelectorAll('.banner-opt').forEach(opt => {
        const preview = opt.querySelector('.banner-opt-preview');
        if (!preview) return;
        const estilo = opt.dataset.cover;
        const built = PR.buildCanvas({ nombre: 'preview', categoria: '', tipo: '', stack: [], accent, seed: PR.hashStr(estilo), estilo });
        preview.className = 'banner-opt-preview ' + built.modeClass;
        preview.innerHTML = built.stage;
    });
}

function actualizarPreviewLateral() {
    const nombre = v('f-nombre') || 'Nombre del proyecto';
    const cat = v('f-categoria') || 'Categoria';
    const desc = v('f-desc') || 'La descripcion aparecera aqui.';
    const anio = v('f-anio') || '2026';

    document.getElementById('lp-title').textContent = nombre;
    document.getElementById('lp-meta').textContent = [cat, anio].filter(Boolean).join(' · ');
    document.getElementById('lp-desc').textContent = desc.slice(0, 100) + (desc.length > 100 ? '...' : '');

    pintarCard('lp');
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

    document.getElementById('pv-titulo').textContent = nombre;
    document.getElementById('pv-meta').textContent = [cat, tipo, anio].filter(Boolean).join(' · ');
    document.getElementById('pv-desc').textContent = desc;
    document.getElementById('pv-draft-badge').hidden = pub;

    pintarCard('pv');

    const scores = [
        { label: 'Nombre y categoria', val: (nombre !== '—' && cat) ? 100 : (nombre !== '—' ? 50 : 0) },
        { label: 'Descripcion corta', val: v('f-desc').length > 30 ? 100 : v('f-desc').length > 0 ? 50 : 0 },
        { label: 'Stack definido', val: stackTags.length > 0 ? 100 : 0 },
        { label: 'Descripcion extendida', val: v('f-desc2').length > 30 ? 100 : 0 },
        { label: 'Estilo personalizado', val: (bannerSubido || coverActual !== 'cover-terminal') ? 100 : 50 },
        { label: 'Ícono personalizado', val: logoSubido ? 100 : 50 },
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