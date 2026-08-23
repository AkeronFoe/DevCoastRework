// ── Wizard de registro (talento / empresa) ──────────────────────────────
// Genérico: cuenta los .paso-panel presentes y conecta los botones
// [data-next]/[data-back] sin depender de IDs fijos por paso.
(function () {
    const panels = Array.from(document.querySelectorAll('.paso-panel'));
    if (!panels.length) return;

    const stepItems  = Array.from(document.querySelectorAll('.step-item'));
    const stepNums   = Array.from(document.querySelectorAll('.step-num'));
    const stepLabels = Array.from(document.querySelectorAll('.step-label'));
    const stepLines  = Array.from(document.querySelectorAll('.step-line'));
    const form       = document.querySelector('form.register-wizard');

    let paso = 0;

    function irAPaso(n) {
        paso = n;
        panels.forEach((p, i) => { p.style.display = i === paso ? 'block' : 'none'; });

        stepNums.forEach((s, i) => {
            s.classList.remove('done', 'active');
            s.textContent = i < paso ? '✓' : String(i + 1);
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

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    stepItems.forEach((item, i) => {
        item.addEventListener('click', () => { if (i < paso) irAPaso(i); });
    });

    function validarPaso(n) {
        const reqs = panels[n].querySelectorAll('[required]');
        let ok = true;
        let primerInvalido = null;
        reqs.forEach((el) => {
            el.classList.remove('error');
            const vacio = !el.value || !el.value.trim();
            const invalido = vacio || (el.dataset.customInvalid === 'true');
            if (invalido) {
                el.classList.add('error');
                if (!primerInvalido) primerInvalido = el;
                ok = false;
            }
        });
        if (primerInvalido) primerInvalido.focus();
        return ok;
    }

    document.querySelectorAll('[data-next]').forEach((btn) => {
        btn.addEventListener('click', () => {
            if (validarPaso(paso)) irAPaso(Math.min(paso + 1, panels.length - 1));
        });
    });
    document.querySelectorAll('[data-back]').forEach((btn) => {
        btn.addEventListener('click', () => irAPaso(Math.max(paso - 1, 0)));
    });

    document.querySelectorAll('.form-input, .form-select, .form-textarea, .register-field input, .register-field select, .register-field textarea')
        .forEach((el) => el.addEventListener('input', () => el.classList.remove('error')));

    if (form) {
        form.addEventListener('submit', (e) => {
            for (let i = 0; i < panels.length; i++) {
                if (!validarPaso(i)) {
                    e.preventDefault();
                    irAPaso(i);
                    return;
                }
            }
        });
    }

    // ── Ir directo al paso que trae un campo con error (post-redirect) ──
    const stepConError = document.querySelector('.paso-panel [data-error-field]');
    if (stepConError) {
        const idx = panels.indexOf(stepConError.closest('.paso-panel'));
        irAPaso(idx >= 0 ? idx : 0);
    } else {
        irAPaso(0);
    }

    // ── Mostrar/ocultar contraseña ────────────────────────────────────
    document.querySelectorAll('.pw-toggle-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;
            const showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            btn.classList.toggle('is-showing', !showing);
            btn.setAttribute('aria-label', showing ? 'Mostrar contraseña' : 'Ocultar contraseña');
        });
    });

    // ── Medidor de fuerza de contraseña ────────────────────────────────
    const pwInput    = document.getElementById('password');
    const pwMeter    = document.getElementById('password-strength-fill');
    const pwLabel    = document.getElementById('password-strength-label');
    const pwHelp     = document.getElementById('password-help');

    function evaluarFuerza(pw) {
        if (!pw) return 0;
        let score = 0;
        if (pw.length >= 8) score++;
        if (pw.length >= 12) score++;
        if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) score++;
        if (/\d/.test(pw)) score++;
        if (/[^A-Za-z0-9]/.test(pw)) score++;
        return Math.min(score, 4);
    }

    if (pwInput && pwMeter && pwLabel) {
        const niveles = [
            { label: 'Muy corta', color: 'var(--cat-error, #ef4444)' },
            { label: 'Débil',     color: 'var(--cat-error, #ef4444)' },
            { label: 'Aceptable', color: 'var(--cat-data, #f59e0b)' },
            { label: 'Buena',     color: 'var(--cat-success, #22c55e)' },
            { label: 'Excelente', color: 'var(--cat-success, #22c55e)' },
        ];
        pwInput.addEventListener('input', () => {
            const pw = pwInput.value;
            const tooShort = pw.length > 0 && pw.length < 8;
            pwInput.dataset.customInvalid = tooShort || pw.length === 0 ? 'true' : 'false';
            if (pwHelp) {
                pwHelp.textContent = tooShort
                    ? `Faltan ${8 - pw.length} caracteres para el mínimo de 8.`
                    : 'Mínimo 8 caracteres. Usa mayúsculas, números y símbolos para más seguridad.';
                pwHelp.classList.toggle('is-warning', tooShort);
            }
            const nivel = pw.length === 0 ? 0 : evaluarFuerza(pw);
            const pct = pw.length === 0 ? 0 : Math.max(20, (nivel / 4) * 100);
            pwMeter.style.width = pct + '%';
            pwMeter.style.background = niveles[nivel].color;
            pwLabel.textContent = pw.length === 0 ? '' : niveles[nivel].label;
            pwLabel.style.color = niveles[nivel].color;
            checkConfirmMatch();
        });
    }

    // ── Confirmación de contraseña ─────────────────────────────────────
    const pwConfirm  = document.getElementById('passwordConfirm');
    const pwMatchMsg = document.getElementById('password-match-msg');

    function checkConfirmMatch() {
        if (!pwConfirm || !pwInput) return;
        if (!pwConfirm.value) {
            pwConfirm.dataset.customInvalid = 'true';
            if (pwMatchMsg) pwMatchMsg.textContent = '';
            return;
        }
        const coincide = pwConfirm.value === pwInput.value;
        pwConfirm.dataset.customInvalid = coincide ? 'false' : 'true';
        pwConfirm.classList.toggle('error', !coincide);
        if (pwMatchMsg) {
            pwMatchMsg.textContent = coincide ? '✓ Las contraseñas coinciden' : '✗ Las contraseñas no coinciden';
            pwMatchMsg.classList.toggle('is-ok', coincide);
            pwMatchMsg.classList.toggle('is-warning', !coincide);
        }
    }
    if (pwConfirm) pwConfirm.addEventListener('input', checkConfirmMatch);

    // ── Chequeo de correo disponible (en vivo) ─────────────────────────
    const correoInput    = document.getElementById('correo');
    const correoFeedback = document.getElementById('correo-feedback');
    let correoTimer = null;

    function setCorreoFeedback(msg, cls) {
        if (!correoFeedback) return;
        correoFeedback.textContent = msg;
        correoFeedback.className = 'field-feedback' + (cls ? ' ' + cls : '');
    }

    if (correoInput) {
        correoInput.addEventListener('input', () => {
            correoInput.dataset.customInvalid = 'false';
            clearTimeout(correoTimer);
            const val = correoInput.value.trim();
            if (!val) { setCorreoFeedback('', ''); return; }
            setCorreoFeedback('Comprobando…', 'is-checking');
            correoTimer = setTimeout(() => {
                fetch('/register/disponible?correo=' + encodeURIComponent(val))
                    .then((r) => r.json())
                    .then((data) => {
                        if (!data.formatoValido) {
                            setCorreoFeedback('Ingresa un correo con formato válido.', 'is-warning');
                            correoInput.dataset.customInvalid = 'true';
                        } else if (!data.disponible) {
                            setCorreoFeedback('✗ Ya existe una cuenta con este correo.', 'is-warning');
                            correoInput.dataset.customInvalid = 'true';
                        } else {
                            setCorreoFeedback('✓ Correo disponible', 'is-ok');
                            correoInput.dataset.customInvalid = 'false';
                        }
                    })
                    .catch(() => setCorreoFeedback('', ''));
            }, 450);
        });
    }

    // ── Contador de caracteres (descripción de empresa) ────────────────
    document.querySelectorAll('textarea[maxlength]').forEach((ta) => {
        const counter = document.getElementById(ta.dataset.counter);
        if (!counter) return;
        const update = () => { counter.textContent = ta.value.length + ' / ' + ta.getAttribute('maxlength'); };
        ta.addEventListener('input', update);
        update();
    });
})();
