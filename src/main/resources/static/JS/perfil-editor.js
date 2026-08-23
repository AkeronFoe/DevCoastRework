let stackChips = [...STACK_INICIAL];
let habsChips = [...HABS_INICIALES];

// Logros: guardar estado previo de tarjetas para detectar cambios
const _estadoLogros = {};
document.querySelectorAll('.logro-card').forEach((card, i) => {
    _estadoLogros[i] = card.classList.contains('unlocked');
});

function checkLogrosDesbloqueados(payload) {
    const logrosNombres = {
        1: { campo: 'bio',          nombre: 'Historia propia' },
        7: { campo: 'linkedinUrl',  nombre: 'Conectado' },
        8: { campo: 'githubUrl',    nombre: 'Código público' },
        9: { campo: 'disponibilidad', nombre: 'Disponible' },
    };
    const cards = document.querySelectorAll('.logro-card');
    Object.entries(logrosNombres).forEach(([i, { campo, nombre }]) => {
        const card = cards[i];
        if (!card) return;
        const eraLocked  = !_estadoLogros[i];
        const ahoraActivo = payload[campo] && payload[campo].length > 0;
        if (eraLocked && ahoraActivo) {
            card.classList.remove('locked');
            card.classList.add('unlocked');
            _estadoLogros[i] = true;
            setTimeout(() => showToast('🏆 Logro desbloqueado: ' + nombre, 'ok'), 600);
        }
    });
}


function renderChips(arr, wrapperId, inputId, removeFn) {
    const wrapper = document.getElementById(wrapperId);
    const input = document.getElementById(inputId);
    wrapper.querySelectorAll('.chip-pill').forEach(c => c.remove());
    arr.forEach((val, i) => {
        const chip = document.createElement('span');
        chip.className = 'chip-pill';
        chip.innerHTML = `${val} <button onclick="${removeFn}(${i})" type="button">✕</button>`;
        wrapper.insertBefore(chip, input);
    });
}

function renderStack() { renderChips(stackChips, 'stack-wrapper', 'stack-input', 'removeStack'); }
function renderHabs() { renderChips(habsChips, 'habilidades-wrapper', 'habilidades-input', 'removeHab'); }

function removeStack(i) { stackChips.splice(i, 1); renderStack(); }
function removeHab(i) { habsChips.splice(i, 1); renderHabs(); }

function setupChipInput(inputId, arr, renderFn) {
    const input = document.getElementById(inputId);
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            const val = this.value.trim().replace(/,$/, '');
            if (val && !arr.includes(val)) { arr.push(val); renderFn(); }
            this.value = '';
        }
    });
    document.getElementById(input.closest('.chips-wrapper, [id$="-wrapper"]') ?
        input.parentElement.id : input.id)
        .addEventListener('click', () => input.focus());
}

renderStack();
renderHabs();

document.getElementById('stack-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter' || e.key === ',') {
        e.preventDefault();
        const val = this.value.trim().replace(/,$/, '');
        if (val && !stackChips.includes(val)) { stackChips.push(val); renderStack(); }
        this.value = '';
    }
});
document.getElementById('stack-wrapper').addEventListener('click', () => document.getElementById('stack-input').focus());

document.getElementById('habilidades-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter' || e.key === ',') {
        e.preventDefault();
        const val = this.value.trim().replace(/,$/, '');
        if (val && !habsChips.includes(val)) { habsChips.push(val); renderHabs(); }
        this.value = '';
    }
});
document.getElementById('habilidades-wrapper').addEventListener('click', () => document.getElementById('habilidades-input').focus());

const pfRolInput = document.getElementById('pf-rol');
const rolInput = document.getElementById('rol');
if (pfRolInput && rolInput) {
    pfRolInput.addEventListener('input', () => {
        if (rolInput.value !== pfRolInput.value) {
            rolInput.value = pfRolInput.value;
        }
    });
    rolInput.addEventListener('input', () => {
        if (pfRolInput.value !== rolInput.value) {
            pfRolInput.value = rolInput.value;
        }
    });
}

document.getElementById('pf-nombre').addEventListener('input', function () {
    const letter = this.value.trim().charAt(0).toUpperCase() || 'D';
    document.getElementById('pf-avatar').textContent = letter;
});

function showToast(msg, type = 'ok') {
    if (window.UIFeedback && typeof window.UIFeedback.toast === 'function') {
        window.UIFeedback.toast(msg, type);
        return;
    }
    const t = document.getElementById('toast');
    if (!t) return;
    t.textContent = msg;
    t.className = `toast ${type} show`;
    setTimeout(() => t.classList.remove('show'), 3200);
}

document.getElementById('btn-guardar-perfil').addEventListener('click', async () => {
    const btn = document.getElementById('btn-guardar-perfil');
    btn.classList.add('btn-saving');
    btn.textContent = 'Guardando...';

    const payload = {
        nombre: document.getElementById('pf-nombre').value.trim(),
        rolTitulo: document.getElementById('pf-rol').value.trim()
            || document.getElementById('rol').value.trim(),
        bio: document.getElementById('bio').value.trim(),
        emailContacto: document.getElementById('email').value.trim(),
        telefono: document.getElementById('telefono').value.trim(),
        portfolioUrl: document.getElementById('portfolio').value.trim(),
        linkedinUrl: document.getElementById('linkedin').value.trim(),
        githubUrl: document.getElementById('github').value.trim(),
        ubicacion: document.getElementById('ubicacion').value.trim(),
        modalidad: document.getElementById('modalidad').value.trim(),
        disponibilidad: document.getElementById('disponibilidad').value.trim(),
        salarioEsperado: document.getElementById('salario').value.trim(),
        especialidad: document.getElementById('especialidad').value.trim(),
        stack: stackChips,
        habilidadesBlandas: habsChips
    };

    try {
        const res = await fetch('/perfil/guardar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (data.ok) {
            showToast('✓ Perfil guardado correctamente', 'ok');
            if (payload.nombre) {
                document.querySelectorAll('.profile-name').forEach(el => el.textContent = payload.nombre);
                document.querySelectorAll('.profile-avatar').forEach(el => el.textContent = payload.nombre.charAt(0).toUpperCase());
            }
            if (payload.rolTitulo) {
                document.querySelectorAll('.profile-role').forEach(el => el.textContent = payload.rolTitulo);
            }
            if (payload.ubicacion) {
                const ubicacionEl = document.getElementById('pf-ubicacion-text');
                if (ubicacionEl) ubicacionEl.textContent = payload.ubicacion;
            }
            if (payload.modalidad) {
                const modalidadEl = document.getElementById('pf-chip-modalidad');
                if (modalidadEl) modalidadEl.textContent = payload.modalidad;
            }
            if (payload.disponibilidad) {
                const dispoEl = document.getElementById('pf-chip-disponibilidad');
                if (dispoEl) dispoEl.textContent = payload.disponibilidad;
            }

            // Detectar logros recién desbloqueados y notificar
            setTimeout(() => checkLogrosDesbloqueados(payload), 300);
        } else {
            showToast('Error: ' + (data.error || 'No se pudo guardar'), 'err');
        }
    } catch (err) {
        showToast('Error de red. Intenta de nuevo.', 'err');
    } finally {
        btn.classList.remove('btn-saving');
        btn.innerHTML = `<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> Guardar cambios`;
    }
});