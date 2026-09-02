(function () {
    var hasGsap = typeof window.gsap !== 'undefined';
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    document.addEventListener('DOMContentLoaded', function () {
        var frameWrap = document.getElementById('lc-frame-wrap');
        var frame = document.getElementById('lc-frame');
        var handles = frameWrap ? frameWrap.querySelectorAll('.lc-handle') : [];

        // Entrada del frame: cae con rebote (back.out) y los tiradores de las
        // esquinas "encajan" un instante despues, como si el objeto se
        // acabara de soltar sobre el canvas. Si GSAP no cargo (red bloqueada)
        // o el usuario pidio reducir movimiento, el frame ya es visible por
        // CSS de por si — no depende de este script para mostrarse.
        if (hasGsap && !reduce && frame) {
            var tl = gsap.timeline();
            tl.fromTo(frame,
                { opacity: 0, y: -24, scale: .92 },
                { opacity: 1, y: 0, scale: 1, duration: .6, ease: 'back.out(1.7)' })
                .fromTo(handles,
                    { opacity: 0, scale: 0 },
                    { opacity: 1, scale: 1, duration: .35, ease: 'back.out(2.5)', stagger: .05 }, '-=0.15')
                .fromTo(frame,
                    { boxShadow: '0 0 0 0 rgba(79,160,232,.55)' },
                    { boxShadow: '0 0 0 14px rgba(79,160,232,0)', duration: .6, ease: 'power1.out' }, '<');
        }

        // Si el servidor marco el frame como error (login fallido), el foco
        // va directo al correo — ni el correo ni la contraseña sobreviven
        // al redirect, asi que el usuario tiene que reescribir los dos igual.
        if (frame && frame.classList.contains('has-error')) {
            var correoInput = document.getElementById('correo');
            if (correoInput) correoInput.focus();
        }

        // Contador de intentos fallidos en esta pestaña (sessionStorage, no
        // toca el servidor). Se resetea apenas la pagina carga sin error —
        // asi no arrastra un numero viejo de una sesion anterior.
        try {
            var ATTEMPT_KEY = 'dc_login_attempts';
            var attemptEl = document.getElementById('lc-attempt');
            if (frame && frame.classList.contains('has-error')) {
                var attempts = parseInt(sessionStorage.getItem(ATTEMPT_KEY) || '0', 10) + 1;
                sessionStorage.setItem(ATTEMPT_KEY, String(attempts));
                if (attemptEl) attemptEl.textContent = ' · intento #' + (attempts + 1);
            } else {
                sessionStorage.removeItem(ATTEMPT_KEY);
            }
        } catch (err) {
            // sessionStorage bloqueado (privado / permisos) — se omite sin romper nada
        }

        // Borde ambar solo despues de que el usuario escribio algo invalido
        // y salio del campo — nunca al cargar la pagina con los required
        // vacios, eso solo regaña sin necesidad.
        if (frame) {
            frame.querySelectorAll('.lc-input-row input').forEach(function (inp) {
                var row = inp.closest('.lc-input-row');
                if (!row) return;
                inp.addEventListener('blur', function () {
                    row.classList.toggle('is-invalid', inp.value.length > 0 && !inp.checkValidity());
                });
                inp.addEventListener('input', function () {
                    if (row.classList.contains('is-invalid')) {
                        row.classList.toggle('is-invalid', !inp.checkValidity());
                    }
                });
            });
        }

        // El tag de arriba pasa a "editando" mientras un campo tiene foco.
        var frameTag = document.getElementById('lc-frame-tag');
        if (frameTag && frame) {
            frame.querySelectorAll('input').forEach(function (inp) {
                inp.addEventListener('focus', function () {
                    frameTag.classList.remove('is-editing');
                    void frameTag.offsetWidth; // reinicia la animacion si ya estaba corriendo
                    frameTag.classList.add('is-editing');
                });
                inp.addEventListener('blur', function () {
                    frameTag.classList.remove('is-editing');
                });
            });
        }

        // Coordenadas en vivo en la barra de estado, como el inspector de
        // cualquier herramienta de diseño — mas el playhead que se desliza
        // sobre las reglas siguiendo al mouse de verdad.
        var canvas = document.querySelector('.lc-canvas');
        var coords = document.getElementById('lc-coords');
        var rulerX = document.querySelector('.lc-ruler-x');
        var rulerY = document.querySelector('.lc-ruler-y');
        var markX = document.getElementById('lc-ruler-mark-x');
        var markY = document.getElementById('lc-ruler-mark-y');
        var cursorTag = document.getElementById('lc-cursor-tag');
        if (canvas && coords) {
            canvas.addEventListener('mousemove', function (e) {
                var r = canvas.getBoundingClientRect();
                var x = Math.max(0, Math.round(e.clientX - r.left));
                var y = Math.max(0, Math.round(e.clientY - r.top));
                coords.textContent = 'x: ' + x + '   y: ' + y + '   ·   zoom 100%   ·   devcoast v2';

                // el recorrido del triangulo se limita al largo real de su
                // propia regla — antes se lo recortaba "overflow:hidden" en
                // el contenedor, pero eso tambien se comia la puntita del
                // triangulo (se dibuja fuera de su propia caja a propósito)
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
                // tooltip de coordenadas junto al cursor mismo, ademas del
                // playhead en las reglas
                if (cursorTag) {
                    cursorTag.style.left = x + 'px';
                    cursorTag.style.top = y + 'px';
                    cursorTag.textContent = x + ', ' + y;
                    cursorTag.classList.add('is-visible');
                }
            });
            canvas.addEventListener('mouseleave', function () {
                if (markX) markX.classList.remove('is-visible');
                if (markY) markY.classList.remove('is-visible');
                if (cursorTag) cursorTag.classList.remove('is-visible');
            });
        }

        // grilla de puntos animada de fondo (reemplaza el radial-gradient
        // estatico): cada punto se aparta del cursor cuando pasa cerca y
        // vuelve solo a su lugar al alejarse. Ademas la capa entera hace un
        // paralaje sutil, opuesto a hacia donde se arrastra o redimensiona
        // el frame — mismo mecanismo que el mockup "Guías de Alineación".
        var dotsCanvasEl = document.getElementById('lc-dots-canvas');
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

        // Mostrar / ocultar contraseña.
        var eyeBtn = document.getElementById('lc-eye');
        var pwInput = document.getElementById('password');
        if (eyeBtn && pwInput) {
            eyeBtn.addEventListener('click', function () {
                var showing = pwInput.type === 'text';
                pwInput.type = showing ? 'password' : 'text';
                eyeBtn.classList.toggle('is-showing', !showing);
                eyeBtn.setAttribute('aria-label', showing ? 'Mostrar contraseña' : 'Ocultar contraseña');
            });
        }

        // Lectura de "fuerza" puramente informativa mientras se escribe la
        // contraseña — no valida nada, es un dato del inspector nada mas
        // (aca no se esta creando una contraseña nueva).
        var strengthEl = document.getElementById('lc-strength');
        var strengthLabel = document.getElementById('lc-strength-label');
        if (pwInput && strengthEl && strengthLabel) {
            var STRENGTH_LABELS = { 1: 'básica', 2: 'moderada', 3: 'buena', 4: 'fuerte' };
            pwInput.addEventListener('input', function () {
                var v = pwInput.value;
                if (!v) {
                    strengthEl.classList.remove('is-visible');
                    strengthEl.removeAttribute('data-level');
                    return;
                }
                var score = 0;
                if (v.length >= 6) score++;
                if (v.length >= 10) score++;
                if (/[0-9]/.test(v) && /[a-zA-Z]/.test(v)) score++;
                if (/[^a-zA-Z0-9]/.test(v)) score++;
                score = Math.max(1, Math.min(4, score));
                strengthEl.setAttribute('data-level', String(score));
                strengthEl.classList.add('is-visible');
                strengthLabel.textContent = STRENGTH_LABELS[score];
            });
        }

        // Aviso de Bloq Mayus mientras se escribe la contraseña — el estado
        // de esa tecla solo se puede leer desde un evento de teclado.
        var capsWarn = document.getElementById('lc-capswarn');
        if (pwInput && capsWarn) {
            var checkCaps = function (e) {
                var isCaps = !!(e.getModifierState && e.getModifierState('CapsLock'));
                capsWarn.classList.toggle('is-visible', isCaps);
            };
            pwInput.addEventListener('keydown', checkCaps);
            pwInput.addEventListener('keyup', checkCaps);
            pwInput.addEventListener('blur', function () {
                capsWarn.classList.remove('is-visible');
            });
        }

        // Punto de "cambios sin guardar" en el tag de arriba, como en la
        // pestaña de un editor — aparece apenas hay texto en algun campo.
        var unsavedDot = document.getElementById('lc-unsaved-dot');
        var correoField = document.getElementById('correo');
        if (unsavedDot && frame) {
            var updateUnsaved = function () {
                var hasContent = (correoField && correoField.value.length > 0) ||
                    (pwInput && pwInput.value.length > 0);
                unsavedDot.classList.toggle('is-visible', !!hasContent);
            };
            frame.querySelectorAll('.lc-input-row input').forEach(function (inp) {
                inp.addEventListener('input', updateUnsaved);
            });
        }

        // El chip de color copia su hex al portapapeles, como cualquier
        // color picker real — mismo gesto, un dato menos que escribir a mano.
        var copyChip = document.getElementById('lc-copy-hex');
        if (copyChip && navigator.clipboard && navigator.clipboard.writeText) {
            copyChip.addEventListener('click', function () {
                navigator.clipboard.writeText('#4FA0E8').then(function () {
                    copyChip.classList.add('is-copied');
                    setTimeout(function () {
                        copyChip.classList.remove('is-copied');
                    }, 1200);
                }).catch(function () { /* portapapeles no disponible — se ignora */ });
            });
        }

        // El boton pasa a "verificando..." mientras el navegador procesa el
        // envio real del formulario (submit normal, sin AJAX de por medio),
        // y el frame muestra la barra de carga en su borde inferior.
        var form = document.querySelector('.lc-form');
        var submitBtn = document.getElementById('lc-submit');
        if (form && submitBtn) {
            form.addEventListener('submit', function () {
                submitBtn.classList.add('is-loading');
                // se deshabilita DESPUES de que el submit ya se disparo, asi
                // que no cancela el envio en curso — solo evita un segundo
                // click/Enter mientras el navegador procesa el primero.
                submitBtn.disabled = true;
                if (frame) frame.classList.add('is-loading');

                // Cierre con la misma intencion que la entrada: el frame se
                // achica y se apaga en vez de cortarse en seco. El submit no
                // se frena para esto — la pagina va a navegar de todas
                // formas, esto solo se alcanza a ver durante el ida-y-vuelta
                // al servidor.
                if (hasGsap && !reduce && frameWrap) {
                    gsap.to(frameWrap, { opacity: 0, y: 8, scale: .97, duration: .25, ease: 'power2.in' });
                }
            });
        }

        // Atajo Cmd/Ctrl + Enter para enviar desde cualquier campo del frame.
        if (frame && form) {
            frame.addEventListener('keydown', function (e) {
                if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
                    e.preventDefault();
                    if (typeof form.requestSubmit === 'function') {
                        form.requestSubmit();
                    } else {
                        form.submit();
                    }
                }
            });
        }

        // Seleccionar el frame (click fuera de inputs/boton/link) y
        // arrastrarlo agarrando el icono de mover — nunca clickeando
        // cualquier parte del frame, para que nunca se mueva sin querer
        // mientras se completa el formulario. Con limites dentro del canvas.
        var moveHandle = document.getElementById('lc-move-handle');
        if (frameWrap && frame && canvas && moveHandle) {
            frame.addEventListener('click', function (e) {
                if (e.target.closest('input, button, a')) return;
                frameWrap.classList.add('is-selected');
            });
            canvas.addEventListener('click', function (e) {
                if (!frameWrap.contains(e.target)) frameWrap.classList.remove('is-selected');
            });

            var curTX = 0, curTY = 0, dragging = false;
            var startX = 0, startY = 0, minDX = 0, maxDX = 0, minDY = 0, maxDY = 0;
            var POS_KEY = 'dc_login_pos';

            // El transform ya se restauro de forma sincrona en un <script>
            // inline pegado al HTML del frame (antes de este archivo, que
            // es defer y pintaria tarde) — aca solo se recupera el mismo
            // valor para que curTX/curTY arranquen en el punto correcto y
            // el proximo arrastre calcule sobre la posicion real, no sobre 0.
            try {
                if (frame.classList.contains('has-error')) {
                    var savedPos = JSON.parse(sessionStorage.getItem(POS_KEY) || 'null');
                    if (savedPos) {
                        curTX = savedPos.x || 0;
                        curTY = savedPos.y || 0;
                    }
                }
            } catch (err) {
                // sessionStorage bloqueado (privado / permisos) — se omite sin romper nada
            }

            // imán al centro y a los tercios del canvas, con guías visuales
            // — mismo mecanismo que el mockup "Guías de Alineación"
            var SNAP = 7;
            var guideV = document.getElementById('lc-guide-v');
            var guideH = document.getElementById('lc-guide-h');
            var thirdV1 = document.getElementById('lc-third-v1');
            var thirdV2 = document.getElementById('lc-third-v2');
            var thirdH1 = document.getElementById('lc-third-h1');
            var thirdH2 = document.getElementById('lc-third-h2');
            var centeredChip = document.getElementById('lc-centered-chip');
            var edgeT = document.getElementById('lc-edge-t'), edgeTVal = document.getElementById('lc-edge-t-val');
            var edgeB = document.getElementById('lc-edge-b'), edgeBVal = document.getElementById('lc-edge-b-val');
            var edgeL = document.getElementById('lc-edge-l'), edgeLVal = document.getElementById('lc-edge-l-val');
            var edgeR = document.getElementById('lc-edge-r'), edgeRVal = document.getElementById('lc-edge-r-val');
            var inspX = document.getElementById('lc-insp-x'), inspY = document.getElementById('lc-insp-y');
            var dragCanvasRect = null, thirdOffX = 0, thirdOffY = 0, edgesWanted = false;
            var wasAnySnap = false, snapEaseTimer = null, guideFadeTimer = null;

            // las guias quedan prendidas mientras se interactua, pero una
            // vez que soltas se apagan solas — dejarlas fijas para siempre
            // en "centrado" hace ruido en el canvas sin aportar nada mas
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

                // el salto al enganchar se suaviza con una transicion breve
                // que se apaga sola, para que el arrastre libre siga 1:1
                var isAnySnap = onX || onY || onThirdXNeg || onThirdXPos || onThirdYNeg || onThirdYPos;
                if (isAnySnap && !wasAnySnap && !reduce) {
                    frameWrap.classList.add('is-snap-ease');
                    clearTimeout(snapEaseTimer);
                    snapEaseTimer = setTimeout(function () { frameWrap.classList.remove('is-snap-ease'); }, 170);
                }
                wasAnySnap = isAnySnap;

                if (inspX) inspX.textContent = Math.round(totalX);
                if (inspY) inspY.textContent = Math.round(totalY);

                // las lineas de distancia se apagan en el eje que ya
                // coincide con su guia, para no dibujarse encima
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
                startX = e.clientX;
                startY = e.clientY;
                var wrapRect = frameWrap.getBoundingClientRect();
                var canvasRect = canvas.getBoundingClientRect();
                // el limite de arriba/izquierda respeta el espacio de las
                // reglas (si estan ocultas por pantalla chica, su rect mide
                // 0 y no molesta)
                var rulerYW = rulerY ? rulerY.getBoundingClientRect().width : 0;
                var rulerXH = rulerX ? rulerX.getBoundingClientRect().height : 0;
                minDX = (canvasRect.left + rulerYW) - wrapRect.left;
                maxDX = canvasRect.right - wrapRect.right;
                minDY = (canvasRect.top + rulerXH) - wrapRect.top;
                maxDY = canvasRect.bottom - wrapRect.bottom;

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

                var frameScale = document.getElementById('lc-frame-scale');
                if (!reduce && frameScale) {
                    frameScale.classList.remove('is-dropping');
                    void frameScale.offsetWidth;
                    frameScale.classList.add('is-dropping');
                    setTimeout(function () { frameScale.classList.remove('is-dropping'); }, 280);
                }
            });

            // nudge con teclado — con el frame seleccionado, las flechas lo
            // mueven 1px (10px con Shift), mismos imanes que el arrastre
            var nudgeParallaxTimer = null;
            document.addEventListener('keydown', function (e) {
                if (dragging || !frameWrap.classList.contains('is-selected')) return;
                var active = document.activeElement;
                if (active && /INPUT|TEXTAREA/.test(active.tagName)) return;
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
                var lMinDX = (canvasRect.left + rulerYW) - wrapRect.left;
                var lMaxDX = canvasRect.right - wrapRect.right;
                var lMinDY = (canvasRect.top + rulerXH) - wrapRect.top;
                var lMaxDY = canvasRect.bottom - wrapRect.bottom;
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

            // (Hubo resize funcional desde las esquinas en una version
            // anterior — se saco porque, con el alto siempre atado al
            // contenido, no se podia resolver el eje vertical de forma que
            // se sintiera bien, y una funcion a medias es peor que no
            // tenerla. Las esquinas volvieron a ser solo decorativas.)
        }

        // Explorer: colapsar/expandir carpetas de verdad. Son <div>, no
        // <button> — el sidebar entero es aria-hidden (puro adorno), asi
        // que no deben quedar en el orden de tabulacion antes del formulario.
        document.querySelectorAll('.lc-tree-row.is-folder').forEach(function (row) {
            row.addEventListener('click', function () {
                var item = row.closest('.lc-tree-item');
                var children = item && item.querySelector(':scope > .lc-tree-children');
                if (!children) return;
                var collapsed = children.classList.toggle('is-collapsed');
                row.classList.toggle('is-collapsed', collapsed);
            });
        });
    });
})();
