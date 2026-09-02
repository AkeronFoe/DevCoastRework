(function () {
    var hasGsap = typeof window.gsap !== 'undefined';
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    document.addEventListener('DOMContentLoaded', function () {
        var page = document.querySelector('.rg-page');
        var frameWrap = document.getElementById('rg-frame-wrap');
        var frame = document.getElementById('rg-frame');
        var fbody = document.getElementById('rg-fbody');
        var canvas = document.getElementById('rg-canvas');
        if (!page || !frame) return;

        var handles = frameWrap ? frameWrap.querySelectorAll('.rg-handle') : [];

        // Posición de arrastre del frame — declarada acá arriba (no adentro
        // del bloque de drag, más abajo) porque el estado chooser/wizard
        // también necesita tocarla: el frame cambia bastante de alto entre
        // el selector y cada wizard, y entre los dos wizards entre sí (los
        // campos de empresa no ocupan lo mismo que los de talento).
        var curTX = 0, curTY = 0;
        var POS_KEY = 'dc_register_pos';

        // Se llama después de cada cambio de contenido (elegir tipo, volver
        // al selector, cambiar de paso) — pero a diferencia de un reset a
        // cero, sólo TOCA la posición si el tamaño nuevo realmente la deja
        // fuera del canvas, y en ese caso la mueve lo mínimo necesario para
        // que vuelva a entrar (nunca la "recentra" de prepo). Si ya entraba,
        // no hace nada — nada de saltos cuando no hacen falta.
        var recenterTimer = null;

        function clampFramePosition() {
            if (!frameWrap || !canvas) return;
            var rulerXEl = document.querySelector('.rg-ruler-x');
            var rulerYEl = document.querySelector('.rg-ruler-y');
            var wrapRect = frameWrap.getBoundingClientRect();
            var canvasRect = canvas.getBoundingClientRect();
            var rulerYW = rulerYEl ? rulerYEl.getBoundingClientRect().width : 0;
            var rulerXH = rulerXEl ? rulerXEl.getBoundingClientRect().height : 0;

            // wrapRect ya incluye el transform vigente (curTX/curTY) — restarlo
            // da la posición "de reposo" sobre la que calcular los límites
            var restLeft = wrapRect.left - curTX;
            var restTop = wrapRect.top - curTY;
            var restRight = wrapRect.right - curTX;
            var restBottom = wrapRect.bottom - curTY;

            var minDX = (canvasRect.left + rulerYW) - restLeft;
            var maxDX = canvasRect.right - restRight;
            var minDY = (canvasRect.top + rulerXH) - restTop;
            var maxDY = canvasRect.bottom - restBottom;

            var clampedX = Math.min(maxDX, Math.max(minDX, curTX));
            var clampedY = Math.min(maxDY, Math.max(minDY, curTY));

            if (Math.round(clampedX) === Math.round(curTX) && Math.round(clampedY) === Math.round(curTY)) {
                return; // ya entra en el canvas, no tocar nada
            }

            curTX = clampedX;
            curTY = clampedY;
            frameWrap.classList.add('is-recentering');
            frameWrap.style.transform = (curTX || curTY) ? 'translate(' + curTX + 'px,' + curTY + 'px)' : '';
            clearTimeout(recenterTimer);
            recenterTimer = setTimeout(function () { frameWrap.classList.remove('is-recentering'); }, 420);
            try {
                sessionStorage.setItem(POS_KEY, JSON.stringify({ x: curTX, y: curTY }));
            } catch (err) { /* bloqueado — se ignora */ }
        }

        // Entrada del frame: mismo rebote que el login (back.out) — cae y
        // los tiradores encajan un instante despues.
        if (hasGsap && !reduce) {
            var tl = gsap.timeline();
            tl.fromTo(frame,
                { opacity: 0, y: -24, scale: .92 },
                { opacity: 1, y: 0, scale: 1, duration: .6, ease: 'back.out(1.7)' })
                .fromTo(handles,
                    { opacity: 0, scale: 0 },
                    { opacity: 1, scale: 1, duration: .35, ease: 'back.out(2.5)', stagger: .05 }, '-=0.15');
        }

        /* =======================================================
         * Estado tipo=talento/empresa/null y máquina de estados
         * chooser -> loading -> wizard, ver register.css [data-state]
         * ======================================================= */

        var chooser = document.getElementById('rg-chooser');
        var loading = document.getElementById('rg-loading');
        var wizard = document.getElementById('rg-wizard');
        var frameTagLabel = document.getElementById('rg-frame-tag-label');
        var frameTag = document.getElementById('rg-frame-tag');
        var unsavedDot = document.getElementById('rg-unsaved-dot');
        var statusDot = document.getElementById('rg-status-dot');
        var statusLabel = document.getElementById('rg-status-label');
        var crumbFile = document.getElementById('rg-crumb-file');
        var copyLabel = document.getElementById('rg-copy-label');
        var pickTalentoBtn = document.getElementById('rg-pick-talento');
        var pickEmpresaBtn = document.getElementById('rg-pick-empresa');
        var changeTypeBtn = document.getElementById('rg-change-type');

        var COPY = {
            talento: {
                heroTitle: 'Construye un perfil que destaque.',
                heroSub: 'Crea tu cuenta, muestra tu stack y presenta tu trabajo en 3 pasos rápidos.',
                esta: 'Perfil profesional · 3 pasos · ~2 min',
                crumb: 'register-talento.tsx',
                crumbLoading: 'register.tsx → talento',
                hex: '#4FA0E8'
            },
            empresa: {
                heroTitle: 'Publica vacantes que sí conviertan.',
                heroSub: 'Configura tu cuenta corporativa y prepárate para publicar tu primera vacante.',
                esta: 'Cuenta corporativa · 2 pasos · ~90 seg',
                crumb: 'register-empresa.tsx',
                crumbLoading: 'register.tsx → empresa',
                hex: '#C7B6FC'
            }
        };

        var ICON_PERSON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        var ICON_BUILDING = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/><path d="M10 6h4M10 10h4M10 14h4M10 18h4"/></svg>';

        function applyWizardCopy(tipo) {
            var c = COPY[tipo];
            if (!c) return;
            var title = document.getElementById('rg-wizard-title');
            var sub = document.getElementById('rg-wizard-sub');
            var estaText = document.getElementById('rg-esta-text');
            var kickerTipo = document.getElementById('rg-kicker-tipo');
            var typeBadge = document.getElementById('rg-type-badge');
            if (title) title.textContent = c.heroTitle;
            if (sub) sub.textContent = c.heroSub;
            if (estaText) estaText.textContent = c.esta;
            if (kickerTipo) kickerTipo.textContent = tipo;
            if (typeBadge) typeBadge.innerHTML = tipo === 'talento' ? ICON_PERSON : ICON_BUILDING;
            if (crumbFile) crumbFile.textContent = c.crumb;
            if (frameTagLabel) frameTagLabel.textContent = 'register-frame · paso 1/3';
            if (statusLabel) statusLabel.textContent = '2 modificados';
            if (unsavedDot) unsavedDot.classList.add('is-visible');
            if (copyLabel) copyLabel.textContent = c.hex;
        }

        function setActiveForm(tipo) {
            var talentoForm = document.getElementById('rg-form-talento');
            var empresaForm = document.getElementById('rg-form-empresa');
            if (talentoForm) talentoForm.classList.toggle('is-active', tipo === 'talento');
            if (empresaForm) empresaForm.classList.toggle('is-active', tipo === 'empresa');
        }

        // correo/contraseña son la misma credencial sin importar el tipo —
        // si ya se escribieron en un formulario y el otro está vacío, se
        // copian al cambiar de tipo. El nombre NO se copia: "Andrea López"
        // y "Aurora Labs" no son el mismo dato reencuadrado.
        function copySharedFields(fromTipo, toTipo) {
            if (!fromTipo || fromTipo === toTipo) return;
            ['correo', 'password', 'passwordConfirm'].forEach(function (field) {
                var from = document.querySelector('#rg-form-' + fromTipo + ' [name="' + field + '"]');
                var to = document.querySelector('#rg-form-' + toTipo + ' [name="' + field + '"]');
                if (from && to && from.value && !to.value) {
                    to.value = from.value;
                    to.dispatchEvent(new Event('input', { bubbles: true }));
                }
            });
        }

        var lastTipo = null;

        function goToChooser() {
            page.setAttribute('data-state', 'chooser');
            page.removeAttribute('data-tipo');
            if (frameTagLabel) frameTagLabel.textContent = 'register-frame · elegir tipo';
            if (statusLabel) statusLabel.textContent = '1 sin confirmar';
            if (unsavedDot) unsavedDot.classList.remove('is-visible');
            if (crumbFile) crumbFile.textContent = 'register.tsx';
            if (copyLabel) copyLabel.textContent = '#4FA0E8';
            var cards = document.querySelectorAll('.rg-card');
            cards.forEach(function (c) {
                c.classList.remove('is-selected', 'is-dim');
                // replay la entrada escalonada cada vez que se vuelve al
                // selector (mismo truco que el build log: sólo animation-name,
                // nunca el shorthand, para no pisar el animation-delay del CSS)
                c.style.animationName = 'none';
                void c.offsetWidth;
                c.style.animationName = '';
            });
            // recién ahora el DOM ya tiene el tamaño del chooser — medir antes
            // de esto hubiera comparado contra el tamaño (más alto) del wizard
            // que se está por abandonar
            clampFramePosition();
        }

        function selectType(tipo) {
            var c = COPY[tipo];
            if (!c) return;
            lastTipo = tipo;

            // 1) el card elegido se ilumina, el otro se atenúa (~450ms)
            if (pickTalentoBtn) pickTalentoBtn.classList.toggle('is-selected', tipo === 'talento');
            if (pickTalentoBtn) pickTalentoBtn.classList.toggle('is-dim', tipo !== 'talento');
            if (pickEmpresaBtn) pickEmpresaBtn.classList.toggle('is-selected', tipo === 'empresa');
            if (pickEmpresaBtn) pickEmpresaBtn.classList.toggle('is-dim', tipo !== 'empresa');

            setTimeout(function () {
                // 2) "compilando" — barra de progreso + build log, ~1.4s
                page.setAttribute('data-state', 'loading');
                page.setAttribute('data-tipo', tipo);
                if (frameTagLabel) frameTagLabel.textContent = 'register-frame · compilando…';
                if (frameTag) frameTag.classList.add('is-busy');
                if (statusDot) statusDot.classList.add('is-busy');
                if (statusLabel) statusLabel.textContent = 'compilando…';
                if (crumbFile) crumbFile.textContent = c.crumbLoading;

                var loadingTitle = document.getElementById('rg-loading-title');
                var loadlabelTipo = document.getElementById('rg-loadlabel-tipo');
                var buildlogTipo = document.getElementById('rg-buildlog-tipo');
                if (loadingTitle) loadingTitle.textContent = c.heroTitle;
                if (loadlabelTipo) loadlabelTipo.textContent = tipo;
                if (buildlogTipo) buildlogTipo.textContent = tipo;

                // reinicia las animaciones CSS (build log stagger + barra de
                // progreso) por si el usuario ya pasó por acá antes
                var fill = document.getElementById('rg-loadbar-fill');
                var logItems = document.querySelectorAll('#rg-buildlog li');
                if (fill) { fill.classList.remove('is-filling'); void fill.offsetWidth; fill.classList.add('is-filling'); }
                logItems.forEach(function (li) {
                    // tocar sólo animation-name (no el shorthand "animation")
                    // — el shorthand resetea TODAS las sub-propiedades,
                    // incluido el animation-delay inline de cada <li>, y eso
                    // era lo que hacía que las tres líneas aparecieran juntas
                    // con delay 0 en vez de una por una.
                    li.style.animationName = 'none';
                    void li.offsetWidth;
                    li.style.animationName = '';
                });

                setTimeout(function () {
                    // 3) revelar el wizard
                    copySharedFields(tipo === 'talento' ? 'empresa' : 'talento', tipo);
                    applyWizardCopy(tipo);
                    setActiveForm(tipo);
                    page.setAttribute('data-state', 'wizard');
                    if (frameTag) frameTag.classList.remove('is-busy');
                    if (statusDot) statusDot.classList.remove('is-busy');
                    if (frame) {
                        frame.classList.remove('is-ready');
                        void frame.offsetWidth;
                        frame.classList.add('is-ready');
                    }
                    if (pickTalentoBtn) pickTalentoBtn.classList.remove('is-selected', 'is-dim');
                    if (pickEmpresaBtn) pickEmpresaBtn.classList.remove('is-selected', 'is-dim');
                    // recién ahora el DOM tiene el tamaño real del wizard (el
                    // de talento, con más campos, suele ser más alto que el
                    // selector) — medir antes hubiera comparado contra el
                    // tamaño viejo y dejado pasar un desborde real
                    clampFramePosition();
                }, 1400);
            }, 450);
        }

        if (pickTalentoBtn) pickTalentoBtn.addEventListener('click', function () { selectType('talento'); });
        if (pickEmpresaBtn) pickEmpresaBtn.addEventListener('click', function () { selectType('empresa'); });
        if (changeTypeBtn) changeTypeBtn.addEventListener('click', goToChooser);

        // estado inicial: si el servidor ya trae un tipo (bookmark directo,
        // o volviendo de un error de validación), arrancar directo en el
        // wizard sin la animación — esa animación es sólo el gesto de
        // "elegir una tarjeta", no algo para repetir en cada carga.
        var initialTipo = page.getAttribute('data-tipo');
        if (initialTipo === 'talento' || initialTipo === 'empresa') {
            lastTipo = initialTipo;
            applyWizardCopy(initialTipo);
            setActiveForm(initialTipo);
        }

        /* =======================================================
         * Chip de color: copia el hex activo al portapapeles
         * ======================================================= */
        var copyChip = document.getElementById('rg-copy-hex');
        if (copyChip && navigator.clipboard && navigator.clipboard.writeText) {
            copyChip.addEventListener('click', function () {
                var hex = copyLabel ? copyLabel.textContent : '#4FA0E8';
                navigator.clipboard.writeText(hex).then(function () {
                    copyChip.classList.add('is-copied');
                    setTimeout(function () { copyChip.classList.remove('is-copied'); }, 1200);
                }).catch(function () { /* portapapeles no disponible — se ignora */ });
            });
        }

        /* =======================================================
         * Sidebar: colapsar/expandir carpetas
         * ======================================================= */
        document.querySelectorAll('.rg-tree-row.is-folder').forEach(function (row) {
            row.addEventListener('click', function () {
                var item = row.closest('.rg-tree-item');
                var children = item && item.querySelector(':scope > .rg-tree-children');
                if (!children) return;
                var collapsed = children.classList.toggle('is-collapsed');
                row.classList.toggle('is-collapsed', collapsed);
            });
        });

        /* =======================================================
         * Wizard: navegación de pasos, por formulario
         * ======================================================= */
        document.querySelectorAll('.rg-wizard-form').forEach(function (form) {
            var pasos = Array.prototype.slice.call(form.querySelectorAll('.rg-paso-panel'));
            var stepItems = Array.prototype.slice.call(form.querySelectorAll('[data-stepper] [data-step]'));
            var stepLines = Array.prototype.slice.call(form.querySelectorAll('[data-stepper] .rg-sline'));

            function irAPaso(n, isInitial) {
                pasos.forEach(function (p) {
                    p.style.display = (Number(p.dataset.paso) === n) ? 'block' : 'none';
                });
                stepItems.forEach(function (item) {
                    var idx = Number(item.dataset.step);
                    var num = item.querySelector('.rg-snum');
                    var label = item.querySelector('.rg-slabel');
                    var done = idx < n, active = idx === n;
                    if (num) {
                        num.classList.toggle('is-active', active);
                        num.classList.toggle('is-done', done);
                        num.textContent = done ? '✓' : String(idx + 1);
                    }
                    if (label) label.classList.toggle('is-active', active || done);
                });
                stepLines.forEach(function (line, idx) {
                    line.classList.toggle('is-done', idx < n);
                });
                // recién ahora el panel nuevo ya está en el DOM con su alto
                // real — cada paso tiene una cantidad de campos distinta.
                // Se salta en la llamada inicial: ahí el drag ni arrancó, y
                // tocar la posición pisaría la que se restaura más abajo
                // (sessionStorage) cuando se vuelve de un error de validación.
                if (!isInitial) clampFramePosition();
            }

            function panelHasErrors(panel) {
                var invalid = false;
                panel.querySelectorAll('[required]').forEach(function (field) {
                    var empty = !field.value || !field.value.trim();
                    var customBad = field.dataset.customInvalid === 'true';
                    var row = field.closest('.rg-irow') || field;
                    if (empty || customBad) {
                        invalid = true;
                        row.classList.add('is-invalid');
                    } else {
                        row.classList.remove('is-invalid');
                    }
                });
                return invalid;
            }

            function firstInvalidField(panel) {
                var bad = null;
                panel.querySelectorAll('[required]').forEach(function (field) {
                    if (bad) return;
                    var empty = !field.value || !field.value.trim();
                    var customBad = field.dataset.customInvalid === 'true';
                    if (empty || customBad) bad = field;
                });
                return bad;
            }

            form.querySelectorAll('[data-next]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var panel = btn.closest('.rg-paso-panel');
                    if (panelHasErrors(panel)) {
                        var bad = firstInvalidField(panel);
                        if (bad) bad.focus();
                        return;
                    }
                    irAPaso(Number(btn.dataset.next));
                });
            });
            form.querySelectorAll('[data-back]').forEach(function (btn) {
                btn.addEventListener('click', function () { irAPaso(Number(btn.dataset.back)); });
            });

            form.addEventListener('submit', function (e) {
                for (var i = 0; i < pasos.length; i++) {
                    if (panelHasErrors(pasos[i])) {
                        e.preventDefault();
                        irAPaso(Number(pasos[i].dataset.paso));
                        var bad = firstInvalidField(pasos[i]);
                        if (bad) bad.focus();
                        return;
                    }
                }
                var submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.classList.add('is-loading');
                    submitBtn.disabled = true;
                }
                if (frame) frame.classList.add('is-loading');
            });

            // si el servidor marcó un campo con error, saltar directo a su
            // paso — sólo tiene sentido para el formulario activo
            var errored = form.querySelector('[data-error-field]');
            irAPaso(errored ? currentPasoIndexOf(errored) : 0, true);

            function currentPasoIndexOf(field) {
                var panel = field.closest('.rg-paso-panel');
                return panel ? Number(panel.dataset.paso) : 0;
            }
            if (errored) {
                setTimeout(function () { errored.focus(); }, hasGsap && !reduce ? 650 : 50);
            }

            /* -------- fuerza de contraseña -------- */
            var pwInput = form.querySelector('[data-password-input]');
            var pwStrength = form.querySelector('[data-pw-strength]');
            var pwLabel = form.querySelector('[data-pw-label]');
            var pwHelp = form.querySelector('[data-pw-help]');
            var PW_LABELS = { 1: 'débil', 2: 'aceptable', 3: 'buena', 4: 'fuerte' };
            var pwHelpDefault = pwHelp ? pwHelp.textContent : '';
            if (pwInput && pwStrength && pwLabel) {
                pwInput.addEventListener('input', function () {
                    var v = pwInput.value;
                    if (!v) {
                        pwStrength.classList.remove('is-visible');
                        pwStrength.removeAttribute('data-level');
                        pwInput.dataset.customInvalid = 'false';
                        if (pwHelp) { pwHelp.textContent = pwHelpDefault; pwHelp.classList.remove('is-error'); }
                        return;
                    }
                    var score = 0;
                    if (v.length >= 8) score++;
                    if (v.length >= 12) score++;
                    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++;
                    if (/[0-9]/.test(v) && /[^a-zA-Z0-9]/.test(v)) score++;
                    score = Math.max(1, Math.min(4, score));
                    pwStrength.setAttribute('data-level', String(score));
                    pwStrength.classList.add('is-visible');
                    pwLabel.textContent = PW_LABELS[score];
                    var short = v.length < 8;
                    pwInput.dataset.customInvalid = short ? 'true' : 'false';
                    if (pwHelp) {
                        pwHelp.textContent = short
                            ? 'Faltan ' + (8 - v.length) + ' caracteres para el mínimo de 8.'
                            : pwHelpDefault;
                        pwHelp.classList.toggle('is-error', short);
                    }
                    // la confirmación depende del valor de esta, revalidar
                    var confirmInput = form.querySelector('[data-password-confirm]');
                    if (confirmInput && confirmInput.value) {
                        confirmInput.dispatchEvent(new Event('input', { bubbles: true }));
                    }
                });
            }

            /* -------- confirmación de contraseña -------- */
            var confirmInput = form.querySelector('[data-password-confirm]');
            var matchMsg = form.querySelector('[data-password-match-msg]');
            if (pwInput && confirmInput && matchMsg) {
                confirmInput.addEventListener('input', function () {
                    var row = confirmInput.closest('.rg-irow');
                    if (!confirmInput.value) {
                        matchMsg.textContent = '';
                        matchMsg.className = 'rg-field-feedback';
                        if (row) row.classList.remove('is-invalid');
                        confirmInput.dataset.customInvalid = 'false';
                        return;
                    }
                    var matches = confirmInput.value === pwInput.value;
                    matchMsg.textContent = matches ? 'Las contraseñas coinciden' : 'Las contraseñas no coinciden';
                    matchMsg.className = 'rg-field-feedback ' + (matches ? 'is-ok' : 'is-error');
                    if (row) row.classList.toggle('is-invalid', !matches);
                    confirmInput.dataset.customInvalid = matches ? 'false' : 'true';
                });
            }

            /* -------- disponibilidad de correo (AJAX) -------- */
            var correoInput = form.querySelector('[data-correo-input]');
            var correoFeedback = form.querySelector('[data-correo-feedback]');
            var correoCheck = form.querySelector('[data-correo-check]');
            var correoTimer = null;
            if (correoInput && correoFeedback) {
                correoInput.addEventListener('input', function () {
                    clearTimeout(correoTimer);
                    var value = correoInput.value.trim();
                    if (correoCheck) correoCheck.classList.remove('is-visible');
                    if (!value) {
                        correoFeedback.textContent = '';
                        correoFeedback.className = 'rg-field-feedback';
                        correoInput.dataset.customInvalid = 'false';
                        return;
                    }
                    correoFeedback.textContent = 'Comprobando…';
                    correoFeedback.className = 'rg-field-feedback';
                    correoTimer = setTimeout(function () {
                        fetch('/register/disponible?correo=' + encodeURIComponent(value))
                            .then(function (r) { return r.json(); })
                            .then(function (data) {
                                if (!data.formatoValido) {
                                    correoFeedback.textContent = 'Ingresa un correo con formato válido';
                                    correoFeedback.className = 'rg-field-feedback is-error';
                                    correoInput.dataset.customInvalid = 'true';
                                } else if (!data.disponible) {
                                    correoFeedback.textContent = 'Ya existe una cuenta con este correo';
                                    correoFeedback.className = 'rg-field-feedback is-error';
                                    correoInput.dataset.customInvalid = 'true';
                                } else {
                                    correoFeedback.textContent = 'Correo disponible';
                                    correoFeedback.className = 'rg-field-feedback is-ok';
                                    correoInput.dataset.customInvalid = 'false';
                                    if (correoCheck) correoCheck.classList.add('is-visible');
                                }
                            })
                            .catch(function () { /* red caída — no se bloquea el avance por esto */ });
                    }, 450);
                });
            }

            /* -------- toggle mostrar/ocultar contraseña -------- */
            form.querySelectorAll('[data-pw-toggle]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    var target = document.getElementById(btn.dataset.target);
                    if (!target) return;
                    var showing = target.type === 'text';
                    target.type = showing ? 'password' : 'text';
                    btn.classList.toggle('is-showing', !showing);
                    btn.setAttribute('aria-label', showing ? 'Mostrar contraseña' : 'Ocultar contraseña');
                });
            });

            /* -------- contador de caracteres -------- */
            form.querySelectorAll('textarea[data-counter]').forEach(function (ta) {
                var counter = document.getElementById(ta.dataset.counter);
                if (!counter) return;
                var max = ta.getAttribute('maxlength');
                var update = function () { counter.textContent = ta.value.length + ' / ' + max; };
                ta.addEventListener('input', update);
                update();
            });

            /* -------- borde ámbar sólo tras tocar un campo inválido -------- */
            form.querySelectorAll('.rg-irow input, .rg-irow select').forEach(function (inp) {
                var row = inp.closest('.rg-irow');
                if (!row) return;
                inp.addEventListener('blur', function () {
                    if (inp.hasAttribute('required')) {
                        row.classList.toggle('is-invalid', inp.value.length > 0 && !inp.checkValidity());
                    }
                });
                inp.addEventListener('input', function () {
                    if (row.classList.contains('is-invalid')) {
                        row.classList.toggle('is-invalid', !inp.checkValidity());
                    }
                });
            });

            /* -------- punto de "sin guardar" mientras hay contenido -------- */
            form.querySelectorAll('input, textarea, select').forEach(function (inp) {
                inp.addEventListener('input', function () {
                    if (!unsavedDot) return;
                    var hasContent = Array.prototype.some.call(
                        form.querySelectorAll('input, textarea'),
                        function (f) { return f.value && f.value.length > 0; });
                    unsavedDot.classList.toggle('is-visible', hasContent);
                });
            });
        });

        // el tag de arriba pulsa "editando" mientras un input del frame
        // tiene foco — igual que en el login
        if (frameTag) {
            frame.querySelectorAll('input, textarea, select').forEach(function (inp) {
                inp.addEventListener('focus', function () {
                    frameTag.classList.remove('is-editing');
                    void frameTag.offsetWidth;
                    frameTag.classList.add('is-editing');
                });
                inp.addEventListener('blur', function () {
                    frameTag.classList.remove('is-editing');
                });
            });
        }

        // Cmd/Ctrl + Enter envía el formulario activo
        frame.addEventListener('keydown', function (e) {
            if (!((e.metaKey || e.ctrlKey) && e.key === 'Enter')) return;
            var activeForm = frame.querySelector('.rg-wizard-form.is-active');
            if (!activeForm) return;
            e.preventDefault();
            if (typeof activeForm.requestSubmit === 'function') activeForm.requestSubmit();
            else activeForm.submit();
        });

        /* =======================================================
         * Coordenadas en vivo + playhead sobre las reglas
         * ======================================================= */
        var coords = document.getElementById('rg-coords');
        var rulerX = document.querySelector('.rg-ruler-x');
        var rulerY = document.querySelector('.rg-ruler-y');
        var markX = document.getElementById('rg-ruler-mark-x');
        var markY = document.getElementById('rg-ruler-mark-y');
        var cursorTag = document.getElementById('rg-cursor-tag');
        if (canvas && coords) {
            canvas.addEventListener('mousemove', function (e) {
                var r = canvas.getBoundingClientRect();
                var x = Math.max(0, Math.round(e.clientX - r.left));
                var y = Math.max(0, Math.round(e.clientY - r.top));
                coords.textContent = 'x: ' + x + '   y: ' + y + '   ·   zoom 100%   ·   devcoast v2';
                if (markX) {
                    var maxX = rulerX ? rulerX.getBoundingClientRect().width : x;
                    markX.style.transform = 'translateX(' + Math.min(maxX, Math.max(0, x - 24)) + 'px)';
                    markX.classList.add('is-visible');
                }
                if (markY) {
                    var maxY = rulerY ? rulerY.getBoundingClientRect().height : y;
                    markY.style.transform = 'translateY(' + Math.min(maxY, Math.max(0, y - 22)) + 'px)';
                    markY.classList.add('is-visible');
                }
                // el tooltip de coordenadas es un gesto de "lienzo vacío" —
                // sobre el frame (cards, campos, botones) sólo se ve como un
                // numerito flotando encima de contenido interactivo
                if (cursorTag) {
                    var overFrame = frame.contains(e.target);
                    if (overFrame) {
                        cursorTag.classList.remove('is-visible');
                    } else {
                        cursorTag.style.left = x + 'px';
                        cursorTag.style.top = y + 'px';
                        cursorTag.textContent = x + ', ' + y;
                        cursorTag.classList.add('is-visible');
                    }
                }
            });
            canvas.addEventListener('mouseleave', function () {
                if (markX) markX.classList.remove('is-visible');
                if (markY) markY.classList.remove('is-visible');
                if (cursorTag) cursorTag.classList.remove('is-visible');
            });
        }

        /* =======================================================
         * Grilla de puntos con repulsión + paralaje de fondo
         * ======================================================= */
        var dotsCanvasEl = document.getElementById('rg-dots-canvas');
        function applyDotsParallax(tx, ty) {
            if (!dotsCanvasEl || reduce) return;
            var px = Math.max(-5, Math.min(5, -tx * 0.035));
            var py = Math.max(-5, Math.min(5, -ty * 0.035));
            dotsCanvasEl.style.transform = 'translate(' + px + 'px,' + py + 'px)';
        }
        (function initDotField() {
            if (!dotsCanvasEl || !canvas) return;
            var ctx = dotsCanvasEl.getContext('2d');
            var SPACING = 24, RADIUS = 100, MAX_PUSH = 11;
            var BASE = { r: 42, g: 48, b: 58 };
            var GLOW = { r: 79, g: 160, b: 232 };
            var points = [], dpr = Math.min(window.devicePixelRatio || 1, 2);
            var mouse = { x: -9999, y: -9999, active: false };
            var w = 0, h = 0;

            function build() {
                var r = canvas.getBoundingClientRect();
                w = r.width; h = r.height;
                dotsCanvasEl.width = w * dpr; dotsCanvasEl.height = h * dpr;
                dotsCanvasEl.style.width = w + 'px'; dotsCanvasEl.style.height = h + 'px';
                ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
                points = [];
                var padX = (w % SPACING) / 2, padY = (h % SPACING) / 2;
                for (var y = padY; y < h; y += SPACING) {
                    for (var x = padX; x < w; x += SPACING) {
                        points.push({ x0: x, y0: y, ox: 0, oy: 0 });
                    }
                }
            }
            build();
            window.addEventListener('resize', build);

            canvas.addEventListener('mousemove', function (e) {
                var r = canvas.getBoundingClientRect();
                mouse.x = e.clientX - r.left; mouse.y = e.clientY - r.top; mouse.active = true;
            });
            canvas.addEventListener('mouseleave', function () { mouse.active = false; });

            function drawStatic() {
                ctx.clearRect(0, 0, w, h);
                ctx.fillStyle = 'rgba(' + BASE.r + ',' + BASE.g + ',' + BASE.b + ',.55)';
                for (var i = 0; i < points.length; i++) {
                    var p = points[i];
                    ctx.beginPath(); ctx.arc(p.x0, p.y0, 1, 0, Math.PI * 2); ctx.fill();
                }
            }
            if (reduce) { drawStatic(); return; }

            function tick() {
                ctx.clearRect(0, 0, w, h);
                for (var i = 0; i < points.length; i++) {
                    var p = points[i];
                    var tx = 0, ty = 0;
                    if (mouse.active) {
                        var dx = p.x0 - mouse.x, dy = p.y0 - mouse.y;
                        var dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist < RADIUS && dist > 0.001) {
                            var force = Math.pow(1 - dist / RADIUS, 2.3) * MAX_PUSH;
                            tx = (dx / dist) * force;
                            ty = (dy / dist) * force;
                        }
                    }
                    p.ox += (tx - p.ox) * .12;
                    p.oy += (ty - p.oy) * .12;
                    var t = Math.min(1, Math.sqrt(p.ox * p.ox + p.oy * p.oy) / MAX_PUSH);
                    var cr = BASE.r + (GLOW.r - BASE.r) * t;
                    var cg = BASE.g + (GLOW.g - BASE.g) * t;
                    var cb = BASE.b + (GLOW.b - BASE.b) * t;
                    var alpha = .42 + .45 * t;
                    var radius = 1 + .7 * t;
                    ctx.beginPath();
                    ctx.fillStyle = 'rgba(' + (cr | 0) + ',' + (cg | 0) + ',' + (cb | 0) + ',' + alpha + ')';
                    ctx.arc(p.x0 + p.ox, p.y0 + p.oy, radius, 0, Math.PI * 2);
                    ctx.fill();
                }
                requestAnimationFrame(tick);
            }
            requestAnimationFrame(tick);
        })();

        /* =======================================================
         * Arrastrar el frame agarrando el ícono de mover — mismo
         * mecanismo 1:1 que login.js: imanes al centro/tercios, guías,
         * distancias a bordes, nudge con flechas, posición persistida.
         * ======================================================= */
        var moveHandle = document.getElementById('rg-move-handle');
        if (frameWrap && frame && canvas && moveHandle) {
            frame.addEventListener('click', function (e) {
                if (e.target.closest('input, button, a, select, textarea')) return;
                frameWrap.classList.add('is-selected');
            });
            canvas.addEventListener('click', function (e) {
                if (!frameWrap.contains(e.target)) frameWrap.classList.remove('is-selected');
            });

            var dragging = false;
            var startX = 0, startY = 0, minDX = 0, maxDX = 0, minDY = 0, maxDY = 0;

            try {
                var params = new URLSearchParams(window.location.search);
                if (params.get('error')) {
                    var savedPos = JSON.parse(sessionStorage.getItem(POS_KEY) || 'null');
                    if (savedPos) {
                        curTX = savedPos.x || 0;
                        curTY = savedPos.y || 0;
                        frameWrap.style.transform = 'translate(' + curTX + 'px,' + curTY + 'px)';
                    }
                } else {
                    sessionStorage.removeItem(POS_KEY);
                }
            } catch (err) {
                // sessionStorage bloqueado (privado / permisos) — se omite sin romper nada
            }

            var SNAP = 7;
            var guideV = document.getElementById('rg-guide-v');
            var guideH = document.getElementById('rg-guide-h');
            var thirdV1 = document.getElementById('rg-third-v1');
            var thirdV2 = document.getElementById('rg-third-v2');
            var thirdH1 = document.getElementById('rg-third-h1');
            var thirdH2 = document.getElementById('rg-third-h2');
            var centeredChip = document.getElementById('rg-centered-chip');
            var edgeT = document.getElementById('rg-edge-t'), edgeTVal = document.getElementById('rg-edge-t-val');
            var edgeB = document.getElementById('rg-edge-b'), edgeBVal = document.getElementById('rg-edge-b-val');
            var edgeL = document.getElementById('rg-edge-l'), edgeLVal = document.getElementById('rg-edge-l-val');
            var edgeR = document.getElementById('rg-edge-r'), edgeRVal = document.getElementById('rg-edge-r-val');
            var inspX = document.getElementById('rg-insp-x'), inspY = document.getElementById('rg-insp-y');
            var dragCanvasRect = null, thirdOffX = 0, thirdOffY = 0, edgesWanted = false;
            var wasAnySnap = false, snapEaseTimer = null, guideFadeTimer = null;

            function scheduleGuideFade() {
                clearTimeout(guideFadeTimer);
                guideFadeTimer = setTimeout(function () {
                    if (guideV) guideV.classList.remove('is-active');
                    if (guideH) guideH.classList.remove('is-active');
                    if (thirdV1) thirdV1.classList.remove('is-active');
                    if (thirdV2) thirdV2.classList.remove('is-active');
                    if (thirdH1) thirdH1.classList.remove('is-active');
                    if (thirdH2) thirdH2.classList.remove('is-active');
                    if (centeredChip) centeredChip.classList.remove('is-active');
                }, 1500);
            }

            function setGuides(totalX, totalY) {
                var onX = Math.abs(totalX) <= SNAP;
                var onY = Math.abs(totalY) <= SNAP;

                var onThirdXNeg = !onX && Math.abs(totalX + thirdOffX) <= SNAP;
                var onThirdXPos = !onX && Math.abs(totalX - thirdOffX) <= SNAP;
                var onThirdYNeg = !onY && Math.abs(totalY + thirdOffY) <= SNAP;
                var onThirdYPos = !onY && Math.abs(totalY - thirdOffY) <= SNAP;

                if (guideV) guideV.classList.toggle('is-active', onX);
                if (guideH) guideH.classList.toggle('is-active', onY);
                if (thirdV1) thirdV1.classList.toggle('is-active', onThirdXNeg);
                if (thirdV2) thirdV2.classList.toggle('is-active', onThirdXPos);
                if (thirdH1) thirdH1.classList.toggle('is-active', onThirdYNeg);
                if (thirdH2) thirdH2.classList.toggle('is-active', onThirdYPos);

                var isCentered = onX && onY;
                if (centeredChip) centeredChip.classList.toggle('is-active', isCentered);

                var isAnySnap = onX || onY || onThirdXNeg || onThirdXPos || onThirdYNeg || onThirdYPos;
                if (isAnySnap && !wasAnySnap && !reduce) {
                    frameWrap.classList.add('is-snap-ease');
                    clearTimeout(snapEaseTimer);
                    snapEaseTimer = setTimeout(function () { frameWrap.classList.remove('is-snap-ease'); }, 170);
                }
                wasAnySnap = isAnySnap;

                if (inspX) inspX.textContent = Math.round(totalX);
                if (inspY) inspY.textContent = Math.round(totalY);

                if (edgeT) edgeT.classList.toggle('is-active', edgesWanted && !onX);
                if (edgeB) edgeB.classList.toggle('is-active', edgesWanted && !onX);
                if (edgeL) edgeL.classList.toggle('is-active', edgesWanted && !onY);
                if (edgeR) edgeR.classList.toggle('is-active', edgesWanted && !onY);

                var x = totalX, y = totalY;
                if (onX) x = 0; else if (onThirdXNeg) x = -thirdOffX; else if (onThirdXPos) x = thirdOffX;
                if (onY) y = 0; else if (onThirdYNeg) y = -thirdOffY; else if (onThirdYPos) y = thirdOffY;
                return { x: x, y: y };
            }

            function updateEdges() {
                if (!dragCanvasRect || !edgeT) return;
                var f = frameWrap.getBoundingClientRect();
                var cx = f.left + f.width / 2 - dragCanvasRect.left;
                var cy = f.top + f.height / 2 - dragCanvasRect.top;
                var topGap = Math.max(0, Math.round(f.top - dragCanvasRect.top));
                var bottomGap = Math.max(0, Math.round(dragCanvasRect.bottom - f.bottom));
                var leftGap = Math.max(0, Math.round(f.left - dragCanvasRect.left));
                var rightGap = Math.max(0, Math.round(dragCanvasRect.right - f.right));

                edgeT.style.left = cx + 'px'; edgeT.style.top = '0px'; edgeT.style.height = topGap + 'px'; edgeTVal.textContent = topGap;
                edgeB.style.left = cx + 'px'; edgeB.style.top = (f.bottom - dragCanvasRect.top) + 'px'; edgeB.style.height = bottomGap + 'px'; edgeBVal.textContent = bottomGap;
                edgeL.style.top = cy + 'px'; edgeL.style.left = '0px'; edgeL.style.width = leftGap + 'px'; edgeLVal.textContent = leftGap;
                edgeR.style.top = cy + 'px'; edgeR.style.left = (f.right - dragCanvasRect.left) + 'px'; edgeR.style.width = rightGap + 'px'; edgeRVal.textContent = rightGap;
            }

            moveHandle.addEventListener('mousedown', function (e) {
                e.preventDefault();
                dragging = true;
                moveHandle.classList.add('is-dragging');
                clearTimeout(guideFadeTimer);
                // si un clamp automático dejó la transición de "recentrado"
                // pegada (hasta 420ms), un drag que arranca en esa ventana
                // hereda esa transition y cada movimiento del mouse queda
                // suavizado/atrasado en vez de seguirlo 1:1 — se corta acá.
                clearTimeout(recenterTimer);
                frameWrap.classList.remove('is-recentering');
                startX = e.clientX;
                startY = e.clientY;
                var wrapRect = frameWrap.getBoundingClientRect();
                var canvasRect = canvas.getBoundingClientRect();
                var rulerYW = rulerY ? rulerY.getBoundingClientRect().width : 0;
                var rulerXH = rulerX ? rulerX.getBoundingClientRect().height : 0;
                // wrapRect ya incluye el transform vigente (curTX/curTY) — los
                // límites tienen que calcularse sobre la posición "de reposo"
                // (sin transform), si no cada arrastre que no arranca desde
                // curTX/curTY = 0 queda con los límites corridos esa misma
                // cantidad, y el error se acumula arrastre tras arrastre.
                var restLeft = wrapRect.left - curTX;
                var restTop = wrapRect.top - curTY;
                var restRight = wrapRect.right - curTX;
                var restBottom = wrapRect.bottom - curTY;
                minDX = (canvasRect.left + rulerYW) - restLeft;
                maxDX = canvasRect.right - restRight;
                minDY = (canvasRect.top + rulerXH) - restTop;
                maxDY = canvasRect.bottom - restBottom;

                dragCanvasRect = canvasRect;
                thirdOffX = canvasRect.width / 6;
                thirdOffY = canvasRect.height / 6;
                edgesWanted = true;
                setGuides(curTX, curTY);
                updateEdges();
            });
            document.addEventListener('mousemove', function (e) {
                if (!dragging) return;
                var rawX = Math.min(maxDX, Math.max(minDX, curTX + (e.clientX - startX)));
                var rawY = Math.min(maxDY, Math.max(minDY, curTY + (e.clientY - startY)));
                var snapped = setGuides(rawX, rawY);
                frameWrap.style.transform = 'translate(' + snapped.x + 'px,' + snapped.y + 'px)';
                updateEdges();
                applyDotsParallax(snapped.x, snapped.y);
            });
            document.addEventListener('mouseup', function (e) {
                if (!dragging) return;
                dragging = false;
                moveHandle.classList.remove('is-dragging');
                var rawX = Math.min(maxDX, Math.max(minDX, curTX + (e.clientX - startX)));
                var rawY = Math.min(maxDY, Math.max(minDY, curTY + (e.clientY - startY)));
                var snapped = setGuides(rawX, rawY);
                curTX = snapped.x; curTY = snapped.y;
                edgesWanted = false;
                setGuides(curTX, curTY);
                applyDotsParallax(0, 0);
                scheduleGuideFade();

                try {
                    sessionStorage.setItem(POS_KEY, JSON.stringify({ x: curTX, y: curTY }));
                } catch (err) {
                    // sessionStorage bloqueado — se omite sin romper nada
                }

                var frameScale = document.getElementById('rg-frame-scale');
                if (!reduce && frameScale) {
                    frameScale.classList.remove('is-dropping');
                    void frameScale.offsetWidth;
                    frameScale.classList.add('is-dropping');
                    setTimeout(function () { frameScale.classList.remove('is-dropping'); }, 280);
                }
            });

            var nudgeParallaxTimer = null;
            document.addEventListener('keydown', function (e) {
                if (dragging || !frameWrap.classList.contains('is-selected')) return;
                var active = document.activeElement;
                if (active && /INPUT|TEXTAREA|SELECT/.test(active.tagName)) return;
                var dx = 0, dy = 0, step = e.shiftKey ? 10 : 1;
                if (e.key === 'ArrowLeft') dx = -step;
                else if (e.key === 'ArrowRight') dx = step;
                else if (e.key === 'ArrowUp') dy = -step;
                else if (e.key === 'ArrowDown') dy = step;
                else return;
                e.preventDefault();
                clearTimeout(guideFadeTimer);

                var wrapRect = frameWrap.getBoundingClientRect();
                var canvasRect = canvas.getBoundingClientRect();
                var rulerYW = rulerY ? rulerY.getBoundingClientRect().width : 0;
                var rulerXH = rulerX ? rulerX.getBoundingClientRect().height : 0;
                // mismo ajuste que en el mousedown del drag: los límites se
                // calculan sobre la posición de reposo, no la ya transformada
                var lRestLeft = wrapRect.left - curTX;
                var lRestTop = wrapRect.top - curTY;
                var lRestRight = wrapRect.right - curTX;
                var lRestBottom = wrapRect.bottom - curTY;
                var lMinDX = (canvasRect.left + rulerYW) - lRestLeft;
                var lMaxDX = canvasRect.right - lRestRight;
                var lMinDY = (canvasRect.top + rulerXH) - lRestTop;
                var lMaxDY = canvasRect.bottom - lRestBottom;
                thirdOffX = canvasRect.width / 6;
                thirdOffY = canvasRect.height / 6;
                dragCanvasRect = canvasRect;

                var rawX = Math.min(lMaxDX, Math.max(lMinDX, curTX + dx));
                var rawY = Math.min(lMaxDY, Math.max(lMinDY, curTY + dy));
                edgesWanted = false;
                var snapped = setGuides(rawX, rawY);
                curTX = snapped.x; curTY = snapped.y;
                frameWrap.style.transform = 'translate(' + curTX + 'px,' + curTY + 'px)';

                try {
                    sessionStorage.setItem(POS_KEY, JSON.stringify({ x: curTX, y: curTY }));
                } catch (err) {
                    // sessionStorage bloqueado — se omite sin romper nada
                }

                applyDotsParallax(curTX, curTY);
                clearTimeout(nudgeParallaxTimer);
                nudgeParallaxTimer = setTimeout(function () { applyDotsParallax(0, 0); }, 220);
                scheduleGuideFade();
            });
        }
    });
})();
