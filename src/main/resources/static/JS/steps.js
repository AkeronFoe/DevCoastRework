const TOTAL = 4;
let paso = 0;
let moneda = 'USD';
const stackTags = [];
const softTags = [];

const panels = document.querySelectorAll('.paso-panel');
const stepNums = document.querySelectorAll('.step-num');
const stepLabels = document.querySelectorAll('.step-label');
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

    stepLines.forEach((l, i) => {
        l.classList.toggle('done', i < paso);
    });

    stepItems.forEach((item, i) => {
        item.classList.toggle('clickable', i < paso);
    });

    if (paso === 3) construirResumen();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

stepItems.forEach((item, i) => {
    item.addEventListener('click', () => {
        if (i < paso) irAPaso(i);
    });
});

function validarPaso(n) {
    if (n === 2) {
        capturarTagsPendientes('stack-input', stackTags);
        capturarTagsPendientes('soft-input', softTags);
    }

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

document.getElementById('btn-siguiente').addEventListener('click', () => {
    if (validarPaso(0)) irAPaso(1);
});
document.getElementById('btn-atras-1').addEventListener('click', () => irAPaso(0));
document.getElementById('btn-siguiente-1').addEventListener('click', () => {
    if (validarPaso(1)) irAPaso(2);
});
document.getElementById('btn-atras-2').addEventListener('click', () => irAPaso(1));
document.getElementById('btn-siguiente-2').addEventListener('click', () => {
    if (validarPaso(2)) irAPaso(3);
});
document.getElementById('btn-atras-3').addEventListener('click', () => irAPaso(2));
document.querySelectorAll('.form-input, .form-select, .form-textarea').forEach(el => {
    el.addEventListener('input', () => el.classList.remove('error'));
});

function capturarTagsPendientes(inputId, array) {
    const input = document.getElementById(inputId);
    if (!input || !input.value.trim()) {
        return;
    }

    input.value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean)
        .forEach(item => {
            if (!array.includes(item)) {
                array.push(item);
                const fieldId = inputId === 'stack-input' ? 'stack-field' : 'soft-field';
                const field = document.getElementById(fieldId);
                if (field) {
                    const chip = crearTagChip(item, array, fieldId);
                    field.insertBefore(chip, input);
                }
            }
        });

    input.value = '';
}

document.querySelectorAll('.currency-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.currency-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        moneda = btn.dataset.currency;
        const hiddenMon = document.getElementById('hidden-moneda');
        if (hiddenMon) hiddenMon.value = moneda;
    });
});

const form = document.querySelector('form');
if (form) {
    form.setAttribute('novalidate', 'novalidate');
    form.addEventListener('submit', (e) => {
        const paso0Ok = validarPaso(0);
        const paso1Ok = validarPaso(1);
        const paso2Ok = validarPaso(2);

        if (!paso0Ok || !paso1Ok || !paso2Ok) {
            e.preventDefault();
            if (!paso0Ok) {
                irAPaso(0);
            } else if (!paso1Ok) {
                irAPaso(1);
            } else {
                irAPaso(2);
            }
            return;
        }

        capturarTagsPendientes('stack-input', stackTags);
        capturarTagsPendientes('soft-input', softTags);

        const stackInput = document.getElementById('stack-input');
        if (stackInput) stackInput.value = stackTags.join(',');
        const softInput = document.getElementById('soft-input');
        if (softInput) softInput.value = softTags.join(',');
        const hidIcon = document.getElementById('hidden-icono');
        if (hidIcon) hidIcon.value = hidIcon.value || 'code';
        const hidColor = document.getElementById('hidden-color-icono');
        if (hidColor) hidColor.value = hidColor.value || iconColor;
        const hidMon = document.getElementById('hidden-moneda');
        if (hidMon) hidMon.value = hidMon.value || moneda;
    });
}

function crearTagChip(texto, array, fieldId) {
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
    });
    return chip;
}

function setupTagInput(inputId, fieldId, array) {
    const input = document.getElementById(inputId);
    const field = document.getElementById(fieldId);

    field.addEventListener('click', () => input.focus());

    input.addEventListener('keydown', e => {
        if ((e.key === 'Enter' || e.key === ',') && input.value.trim()) {
            e.preventDefault();
            const val = input.value.trim().replace(/,$/, '');
            if (val && !array.includes(val)) {
                array.push(val);
                const chip = crearTagChip(val, array, fieldId);
                field.insertBefore(chip, input);
                if (fieldId === 'stack-field') {
                    field.style.border = '';
                    field.style.boxShadow = '';
                }
            }
            input.value = '';
        }
        if (e.key === 'Backspace' && !input.value && array.length) {
            array.pop();
            field.querySelectorAll('.tag-chip').forEach((c, i, arr) => {
                if (i === arr.length - 1) c.remove();
            });
        }
    });
}

setupTagInput('stack-input', 'stack-field', stackTags);
setupTagInput('soft-input', 'soft-field', softTags);

let iconColor = '#1f8f8f';
let iconSVGHtml = document.querySelector('.icon-opt.active svg').outerHTML;

document.querySelectorAll('.icon-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.icon-opt').forEach(o => o.classList.remove('active'));
        opt.classList.add('active');
        iconSVGHtml = opt.querySelector('svg').outerHTML;
        if (!imagenSubida) actualizarIconPreview();
    });
});
document.querySelectorAll('.icon-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.icon-opt').forEach(o => o.classList.remove('active'));
        opt.classList.add('active');
        const iconoKey = opt.dataset.key;
        iconSVGHtml = opt.querySelector('svg').outerHTML;
        document.getElementById('hidden-icono').value = iconoKey;
        if (!imagenSubida) actualizarIconPreview();
    });
});

document.querySelectorAll('.color-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.color-opt').forEach(o => o.classList.remove('active'));
        opt.classList.add('active');
        iconColor = opt.dataset.color;
        if (!imagenSubida) actualizarIconPreview();
    });
});
document.querySelectorAll('.color-opt').forEach(opt => {
    opt.addEventListener('click', () => {
        document.querySelectorAll('.color-opt').forEach(o => o.classList.remove('active'));
        opt.classList.add('active');
        iconColor = opt.dataset.color;
        document.getElementById('hidden-color-icono').value = iconColor;
        if (!imagenSubida) actualizarIconPreview();
    });
});

function actualizarIconPreview() {
    const preview = document.getElementById('icon-preview');
    preview.style.background = iconColor;
    preview.innerHTML = iconSVGHtml;
}

let imagenSubida = false;

document.getElementById('logo-input').addEventListener('change', async function () {
    if (!this.files[0]) return;
    const file = this.files[0];
    const label = document.getElementById('upload-label');
    const preview = document.getElementById('icon-preview');

    const reader = new FileReader();
    reader.onload = e => {
        preview.style.background = 'transparent';
        preview.innerHTML = `<img src="${e.target.result}" style="width:48px;height:48px;object-fit:cover;border-radius:10px;">`;
    };
    reader.readAsDataURL(file);

    label.textContent = 'Subiendo...';
    document.getElementById('btn-quitar-img').style.display = 'none';

    try {
        const formData = new FormData();
        formData.append('file', file);
        const res = await fetch('/proyectos/logo/temp', { method: 'POST', body: formData });
        const data = await res.json();
        if (data.ok) {
            document.getElementById('hidden-logo-url').value = data.url;
            label.textContent = file.name;
            document.getElementById('btn-quitar-img').style.display = 'block';
            imagenSubida = true;
        } else {
            label.textContent = 'Error al subir. Intenta de nuevo.';
            preview.style.background = iconColor;
            preview.innerHTML = iconSVGHtml;
        }
    } catch (e) {
        label.textContent = 'Error de red. Intenta de nuevo.';
        preview.style.background = iconColor;
        preview.innerHTML = iconSVGHtml;
    }
});

document.getElementById('btn-quitar-img').addEventListener('click', () => {
    document.getElementById('logo-input').value = '';
    document.getElementById('upload-label').textContent = 'Subir logo propio (opcional)';
    document.getElementById('btn-quitar-img').style.display = 'none';
    document.getElementById('hidden-logo-url').value = '';
    imagenSubida = false;
    actualizarIconPreview();
});

actualizarIconPreview();

function construirResumen() {
    const empresa = v('empresa-nombre') || '—';
    const sector = v('empresa-sector');
    const titulo = v('rol-titulo') || '—';
    const categoria = v('rol-categoria');
    const seniority = v('rol-seniority');
    const modalidad = v('rol-modalidad');
    const contrato = v('rol-contrato');
    const vacantes = v('rol-vacantes');
    const salMin = v('salario-min');
    const salMax = v('salario-max');
    const periodo = v('salario-periodo');

    document.getElementById('rv-empresa').textContent =
        empresa + (sector ? ` · ${sector}` : '');

    document.getElementById('rv-puesto').textContent =
        [titulo, categoria, seniority].filter(Boolean).join(' · ');

    document.getElementById('rv-condiciones').textContent =
        [modalidad, contrato, vacantes ? `${vacantes} vacante(s)` : ''].filter(Boolean).join(' · ');

    const salTexto = salMin || salMax
        ? `${moneda} ${salMin || '?'} – ${salMax || '?'} ${periodo}`
        : 'No especificado';
    document.getElementById('rv-salario').textContent = salTexto;

    const rvStack = document.getElementById('rv-stack');
    rvStack.innerHTML = '';
    stackTags.forEach(t => {
        const s = document.createElement('span');
        s.className = 'chip';
        s.textContent = t;
        rvStack.appendChild(s);
    });
    if (!stackTags.length) rvStack.innerHTML = '<span class="muted">—</span>';

    document.getElementById('pv-titulo').textContent = titulo;
    document.getElementById('pv-empresa').textContent = empresa;
    document.getElementById('pv-modalidad').textContent = modalidad || 'Modalidad';
    document.getElementById('pv-seniority').textContent = seniority || 'Nivel';
    document.getElementById('pv-contrato').textContent = contrato || 'Contrato';
    document.getElementById('pv-vacantes').textContent =
        vacantes ? `${vacantes} vacante${vacantes == 1 ? '' : 's'}` : '—';
    document.getElementById('pv-salario').textContent =
        salMin || salMax ? `${moneda} ${salMin || '?'} – ${salMax || '?'}` : '—';
    document.getElementById('pv-periodo').textContent = `${moneda} ${periodo}`;

    const pvStack = document.getElementById('pv-stack');
    pvStack.innerHTML = '';
    stackTags.slice(0, 5).forEach(t => {
        const s = document.createElement('span');
        s.className = 'chip';
        s.textContent = t;
        pvStack.appendChild(s);
    });

    const scores = [
        { label: 'Descripcion de empresa', val: v('empresa-desc') ? 100 : 0 },
        { label: 'Titulo y categoria', val: (titulo !== '—' && categoria) ? 100 : (titulo !== '—' ? 50 : 0) },
        { label: 'Salario visible', val: (salMin && salMax) ? 100 : 0 },
        { label: 'Stack definido', val: stackTags.length > 0 ? 100 : 0 },
        { label: 'Descripcion del rol', val: v('req-descripcion') ? 100 : 0 },
    ];

    const scoreWrap = document.getElementById('score-wrap');
    scoreWrap.innerHTML = '';
    scores.forEach(s => {
        const cls = s.val === 100 ? 'hi' : s.val >= 50 ? 'mid' : 'lo';
        scoreWrap.innerHTML += `
                    <div class="score-item">
                        <div class="score-meta">
                            <span>${s.label}</span>
                            <span class="score-pct ${cls}">${s.val}%</span>
                        </div>
                        <div class="progress-track">
                            <div class="progress-fill" style="width:${s.val}%;${s.val < 50 ? 'background:#a2601d;' : s.val < 100 ? '' : ''}"></div>
                        </div>
                    </div>`;
    });

    const pendientes = scores.filter(s => s.val < 100).map(s => s.label);
    document.getElementById('score-tip').textContent = pendientes.length
        ? `Completa: ${pendientes.join(', ')}.`
        : '¡Todo completo! Tu anuncio tiene maxima visibilidad.';
    const pvLogo = document.getElementById('pv-logo');
    pvLogo.style.background = iconColor;
    pvLogo.style.color = '#ffffff';

    if (imagenSubida) {
        const img = document.querySelector('#icon-preview img');
        pvLogo.innerHTML = img
            ? `<img src="${img.src}" style="width:38px;height:38px;object-fit:cover;border-radius:10px;">`
            : iconSVGHtml;
    } else {
        pvLogo.innerHTML = iconSVGHtml;
        const svgEl = pvLogo.querySelector('svg');
        if (svgEl) { svgEl.style.width = '22px'; svgEl.style.height = '22px'; }
    }
}

function v(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}

irAPaso(0);