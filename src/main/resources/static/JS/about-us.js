// DevCoast landing — terminal/source-file interactions (GSAP-driven).
(function () {
    var hasGsap = typeof window.gsap !== 'undefined';
    if (hasGsap && window.ScrollTrigger) {
        gsap.registerPlugin(ScrollTrigger);
    }

    // ---------- mobile nav ----------
    var menuBtn = document.getElementById('dc-menu-btn');
    var mobileNav = document.getElementById('dc-mobile-nav');
    if (menuBtn && mobileNav) {
        menuBtn.addEventListener('click', function () {
            var open = mobileNav.classList.toggle('is-open');
            menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
        });
        mobileNav.querySelectorAll('a').forEach(function (a) {
            a.addEventListener('click', function () {
                mobileNav.classList.remove('is-open');
                menuBtn.setAttribute('aria-expanded', 'false');
            });
        });
    }

    // ---------- save job toggle ----------
    document.querySelectorAll('.dc-job-save').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var pressed = btn.getAttribute('aria-pressed') === 'true';
            btn.setAttribute('aria-pressed', String(!pressed));
            if (hasGsap) {
                gsap.fromTo(btn, { scale: .7 }, { scale: 1, duration: .4, ease: 'back.out(3)' });
            }
        });
    });

    // ---------- boot sequence typing (hero) ----------
    document.addEventListener('DOMContentLoaded', function () {
        var normalPart = 'Encuentra proyectos serios y ';
        var accentPart = 'talento que habla tu idioma.';
        var normalEl = document.getElementById('dc-htw-normal');
        var accentEl = document.getElementById('dc-htw-accent');
        var cursorEl = document.getElementById('dc-htw-cursor');
        if (normalEl && accentEl) {
            var full = normalPart + accentPart;
            if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                normalEl.textContent = normalPart;
                accentEl.textContent = accentPart;
            } else {
                if (cursorEl) cursorEl.classList.add('is-typing');
                var i = 0;
                (function tick() {
                    i++;
                    var shown = full.slice(0, i);
                    if (shown.length <= normalPart.length) {
                        normalEl.textContent = shown;
                    } else {
                        normalEl.textContent = normalPart;
                        accentEl.textContent = shown.slice(normalPart.length);
                    }
                    if (i < full.length) {
                        setTimeout(tick, 26);
                    } else {
                        if (cursorEl) cursorEl.classList.remove('is-typing');
                        // the typed headline can grow past its reserved min-height,
                        // which shifts every section's offsetTop below it
                        if (typeof measure === 'function') measure();
                    }
                })();
            }
        }
    });

    // ---------- scroll-driven UI: gutter readout, header state, active nav ----------
    // A single rAF-throttled listener instead of three independent ones — avoids
    // redoing the same layout reads (scrollY, offsetTop) three times per frame,
    // which is what was making scroll feel laggy.
    var cursor = document.getElementById('dc-gutter-cursor');
    var track = document.querySelector('.dc-gutter-track');
    var readout = document.getElementById('dc-gutter-readout');
    var navLinks = document.querySelectorAll('.dc-nav a, .dc-mobile-nav a');
    var sectionIds = ['boot', 'people', 'compare', 'jobs', 'faq'];
    var sections = sectionIds.map(function (id) { return document.getElementById(id); }).filter(Boolean);
    var TOTAL_LINES = 482;
    var scrollTicking = false;
    var trackHeight = 0;
    var cursorTravel = 0;
    var sectionOffsets = [];
    var docScrollable = 0;

    // offsetTop only changes on resize/reflow, not on scroll — reading it inside
    // the scroll handler was forcing a synchronous layout every frame. Cache it.
    function measure() {
        trackHeight = track ? track.clientHeight : 0;
        // the cursor is anchored by its top edge (top:0), so its own height has
        // to come off the travel distance or its bottom edge overshoots the
        // track once it reaches 100%
        cursorTravel = cursor ? Math.max(0, trackHeight - cursor.offsetHeight) : trackHeight;
        sectionOffsets = sections.map(function (sec) { return sec.offsetTop; });
        docScrollable = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    }
    window.addEventListener('resize', measure, { passive: true });
    window.addEventListener('load', measure);
    measure();

    function renderScrollState() {
        scrollTicking = false;
        var scrollY = window.scrollY;
        var pct = docScrollable > 0 ? Math.min(1, scrollY / docScrollable) : 0;

        // transform instead of top: compositor-only, avoids a layout reflow every scroll frame
        if (cursor) cursor.style.transform = 'translate(-50%, ' + (pct * cursorTravel) + 'px)';
        if (readout) {
            var line = Math.max(1, Math.round(pct * TOTAL_LINES));
            // padStart the percentage too (not just the line number) — this
            // text is vertical-rl, so its character count is what determines
            // the element's rendered height. Going 99% -> 100% used to add a
            // digit and grow the readout mid-scroll, which squeezed the
            // flex track it shares a column with and made the cursor
            // overshoot right at 100%, every time.
            var pctStr = String(Math.round(pct * 100)).padStart(3, ' ');
            readout.textContent = 'LN ' + String(line).padStart(3, '0') + '  ' + pctStr + '%';
        }

        var y = scrollY + 140;
        var current = sections[0] && sections[0].id;
        sections.forEach(function (sec, i) {
            if (sectionOffsets[i] <= y) current = sec.id;
        });
        navLinks.forEach(function (link) {
            var href = link.getAttribute('href') || '';
            link.classList.toggle('is-active', href === '#' + current);
        });
    }

    function onScroll() {
        if (!scrollTicking) {
            scrollTicking = true;
            requestAnimationFrame(renderScrollState);
        }
    }
    window.addEventListener('scroll', onScroll, { passive: true });
    renderScrollState();

    navLinks.forEach(function (link) {
        link.addEventListener('click', function (e) {
            var href = link.getAttribute('href');
            if (!href || href.charAt(0) !== '#') return;
            var target = document.querySelector(href);
            if (!target) return;
            e.preventDefault();
            target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });

    // ---------- man-page / FAQ accordion ----------
    document.querySelectorAll('.dc-man-q').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var entry = btn.closest('.dc-man-entry');
            var isOpen = entry.classList.contains('is-open');
            document.querySelectorAll('.dc-man-entry.is-open').forEach(function (e) { e.classList.remove('is-open'); });
            if (!isOpen) entry.classList.add('is-open');
            btn.setAttribute('aria-expanded', String(!isOpen));
        });
    });

    // ---------- marquee hover pause ----------
    var marqueeTrack = document.querySelector('.dc-marquee-track');
    var marqueeWrap = document.querySelector('.dc-marquee');
    if (marqueeTrack && marqueeWrap) {
        marqueeWrap.addEventListener('mouseenter', function () { marqueeTrack.style.animationPlayState = 'paused'; });
        marqueeWrap.addEventListener('mouseleave', function () { marqueeTrack.style.animationPlayState = 'running'; });
    }

    // ---------- counters ----------
    function animateCount(el, target, dur) {
        dur = dur || 1200;
        var start = performance.now();
        function upd(now) {
            var p = Math.min((now - start) / dur, 1);
            var v = Math.round((1 - Math.pow(1 - p, 3)) * target);
            el.textContent = v;
            if (p < 1) requestAnimationFrame(upd);
        }
        requestAnimationFrame(upd);
    }

    if (hasGsap && window.ScrollTrigger) {
        document.querySelectorAll('[data-count]').forEach(function (el) {
            ScrollTrigger.create({
                trigger: el,
                start: 'top 85%',
                once: true,
                onEnter: function () {
                    animateCount(el, parseInt(el.dataset.count, 10) || 0);
                }
            });
        });
    } else {
        document.querySelectorAll('[data-count]').forEach(function (el) {
            animateCount(el, parseInt(el.dataset.count, 10) || 0);
        });
    }

    // ---------- section reveals + gutter cursor via GSAP ----------
    // NOTE: gsap.matchMedia() with a conditions object only invokes its callback
    // when at least one named condition currently matches — "reduceMotion: false"
    // alone never fires it, so a plain matchMedia check is used here instead.
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (hasGsap && window.ScrollTrigger && !reduce) {
        // Fade only here (no y/transform): .dc-section wraps large subtrees, and
        // a translateY on ~10 of them each promotes a big GPU compositing layer.
        // clearProps removes it afterward, but the cheapest fix is to just not
        // animate transform on the biggest elements in the first place — the
        // smaller cards/rows below still get the nicer slide+fade.
        document.querySelectorAll('.dc-section').forEach(function (sec) {
            gsap.fromTo(sec, { autoAlpha: 0 }, {
                autoAlpha: 1,
                duration: .6,
                ease: 'power2.out',
                scrollTrigger: { trigger: sec, start: 'top 88%', once: true },
                onStart: function () { sec.classList.add('dc-in'); }
            });
        });

        document.querySelectorAll('.dc-fn-card, .dc-joblist-row, .dc-log-entry, .dc-portfolio-card, .dc-diff-line').forEach(function (el) {
            gsap.set(el, { autoAlpha: 0, y: 10 });
        });
        ScrollTrigger.batch('.dc-fn-card, .dc-joblist-row, .dc-log-entry, .dc-portfolio-card', {
            start: 'top 90%',
            once: true,
            onEnter: function (els) {
                gsap.to(els, { autoAlpha: 1, y: 0, duration: .5, stagger: .08, ease: 'power2.out', clearProps: 'transform' });
            }
        });
        ScrollTrigger.batch('.dc-diff-line', {
            start: 'top 90%',
            once: true,
            onEnter: function (els) {
                gsap.to(els, { autoAlpha: 1, y: 0, duration: .35, stagger: .06, ease: 'power1.out', clearProps: 'transform' });
            }
        });
    } else {
        document.querySelectorAll('.dc-section').forEach(function (sec) { sec.classList.add('dc-in'); });
    }
})();

// ---------- live match-network (hero stats panel) ----------
// Node count is driven by the real talent count, not a made-up figure — the
// same "no fake numbers" rule the rest of the page's KPIs follow.
(function () {
    var canvas = document.getElementById('dc-net');
    if (!canvas) return;
    var ctx = canvas.getContext('2d');
    var colors = ['#4fa0e8', '#f2ab5c', '#a78bfa', '#ef7b92'];
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var running = false;
    var dims = { w: 0, h: 0 };

    function resize() {
        var rect = canvas.getBoundingClientRect();
        var dpr = window.devicePixelRatio || 1;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        dims = { w: rect.width, h: rect.height };
    }
    resize();
    window.addEventListener('resize', resize, { passive: true });

    var talentos = parseInt(canvas.dataset.talentos, 10) || 0;
    var COUNT = Math.min(24, Math.max(10, Math.round(talentos / 60)));
    var LINK_DIST = 95;
    var nodes = [];
    for (var i = 0; i < COUNT; i++) {
        nodes.push({
            x: Math.random() * dims.w,
            y: Math.random() * dims.h,
            vx: (Math.random() - 0.5) * 0.2,
            vy: (Math.random() - 0.5) * 0.2,
            r: Math.random() < 0.25 ? 2.4 : 1.5,
            c: colors[Math.floor(Math.random() * colors.length)]
        });
    }

    var rgbCache = {};
    colors.forEach(function (c) {
        var v = parseInt(c.slice(1), 16);
        rgbCache[c] = [(v >> 16) & 255, (v >> 8) & 255, v & 255];
    });

    function draw() {
        ctx.clearRect(0, 0, dims.w, dims.h);
        for (var i = 0; i < nodes.length; i++) {
            var n = nodes[i];
            if (!reduce) {
                n.x += n.vx; n.y += n.vy;
                if (n.x < 0 || n.x > dims.w) n.vx *= -1;
                if (n.y < 0 || n.y > dims.h) n.vy *= -1;
                n.x = Math.max(0, Math.min(dims.w, n.x));
                n.y = Math.max(0, Math.min(dims.h, n.y));
            }
        }
        for (var a = 0; a < nodes.length; a++) {
            for (var b = a + 1; b < nodes.length; b++) {
                var dx = nodes[a].x - nodes[b].x, dy = nodes[a].y - nodes[b].y;
                var d2 = dx * dx + dy * dy;
                if (d2 < LINK_DIST * LINK_DIST) {
                    var dist = Math.sqrt(d2);
                    var alpha = (1 - dist / LINK_DIST) * 0.3;
                    var rgb = rgbCache[nodes[a].c];
                    ctx.strokeStyle = 'rgba(' + rgb[0] + ',' + rgb[1] + ',' + rgb[2] + ',' + alpha + ')';
                    ctx.lineWidth = 1;
                    ctx.beginPath();
                    ctx.moveTo(nodes[a].x, nodes[a].y);
                    ctx.lineTo(nodes[b].x, nodes[b].y);
                    ctx.stroke();
                }
            }
        }
        for (var j = 0; j < nodes.length; j++) {
            var node = nodes[j];
            ctx.beginPath();
            ctx.arc(node.x, node.y, node.r, 0, Math.PI * 2);
            ctx.fillStyle = node.c;
            ctx.globalAlpha = 0.9;
            ctx.fill();
            ctx.globalAlpha = 1;
        }
    }

    function loop() {
        if (!running) return;
        draw();
        if (!reduce) requestAnimationFrame(loop);
    }

    // stop entirely once scrolled past the hero — no point animating what
    // nobody's looking at, and it's one less rAF loop running in the background
    if ('IntersectionObserver' in window) {
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                running = entry.isIntersecting;
                if (running) loop();
            });
        }, { threshold: 0.05 });
        io.observe(canvas);
    } else {
        running = true;
        loop();
    }
    draw();
})();

// ---------- flow-field ambient background (fixed, whole page) ----------
// This one can't be paused with IntersectionObserver the way the other two
// canvases are — it's viewport-fixed, so it's always "in view" by definition.
// To keep an always-on canvas cheap across a full session: redraw every 3rd
// frame only (the drift is slow enough that nobody can tell), and stop the
// loop outright while the tab is in the background (most browsers already
// throttle rAF there, but this makes it explicit rather than relying on it).
(function () {
    var canvas = document.getElementById('dc-flow');
    if (!canvas) return;
    var ctx = canvas.getContext('2d');
    var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var running = true;
    var t = 0;
    var frame = 0;
    var dims = { w: 0, h: 0 };

    function resize() {
        var rect = canvas.getBoundingClientRect();
        var dpr = window.devicePixelRatio || 1;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        dims = { w: rect.width, h: rect.height };
    }
    resize();
    window.addEventListener('resize', function () {
        resize();
        ensureWaves();
    }, { passive: true });

    var WAVE_COUNT = 9;
    var colors = ['rgba(79,160,232,', 'rgba(242,171,92,', 'rgba(167,139,250,'];
    var ALPHA = 0.1;
    var SAMPLE_STEP = 9;

    // Real sine waves, not a wandering noise field — each line sums two
    // frequencies at different, non-matching speeds (one can even run
    // backwards) so the combined shape keeps reshaping itself and never
    // settles into a repeating loop, while still reading as flowing water/
    // audio-waveform lines rather than random scribbles.
    function makeWave(i) {
        return {
            baseY: (dims.h / (WAVE_COUNT + 1)) * (i + 1),
            amp1: 16 + Math.random() * 24,
            amp2: 6 + Math.random() * 14,
            freq1: 0.0035 + Math.random() * 0.0035,
            freq2: 0.008 + Math.random() * 0.006,
            speed1: 0.35 + Math.random() * 0.3,
            speed2: -(0.5 + Math.random() * 0.4),
            phase: Math.random() * Math.PI * 2,
            driftSpeed: 0.06 + Math.random() * 0.06,
            driftPhase: Math.random() * Math.PI * 2
        };
    }

    var waves = [];
    function ensureWaves() {
        waves = [];
        for (var i = 0; i < WAVE_COUNT; i++) waves.push(makeWave(i));
    }
    ensureWaves();

    function draw() {
        ctx.clearRect(0, 0, dims.w, dims.h);
        for (var i = 0; i < waves.length; i++) {
            var w = waves[i];
            var driftY = Math.sin(t * w.driftSpeed + w.driftPhase) * 36;
            ctx.beginPath();
            for (var x = 0; x <= dims.w; x += SAMPLE_STEP) {
                var y = w.baseY + driftY
                    + Math.sin(x * w.freq1 + t * w.speed1 + w.phase) * w.amp1
                    + Math.sin(x * w.freq2 + t * w.speed2 + w.phase * 1.7) * w.amp2;
                if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
            }
            ctx.strokeStyle = colors[i % colors.length] + ALPHA + ')';
            ctx.lineWidth = 1;
            ctx.stroke();
        }
    }

    function loop() {
        if (!running) return;
        frame++;
        if (frame % 3 === 0) {
            t += 0.03;
            draw();
        }
        if (!reduce) requestAnimationFrame(loop);
    }

    document.addEventListener('visibilitychange', function () {
        running = document.visibilityState === 'visible';
        if (running && !reduce) loop();
    });

    draw();
    if (!reduce) loop();
})();
