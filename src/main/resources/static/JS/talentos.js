/* paginación de /talentos por fetch, mismo patrón que empleos.js.
   Los filtros/buscador siguen navegando completo a propósito, solo
   la paginación va por AJAX */
document.addEventListener('DOMContentLoaded', () => {

    const navBar = document.getElementById('nav-progress')
    const resultadosId = 'talentos-resultados'

    let peticionActual = 0
    let abortActual = null

    function startNavBar() {
        if (!navBar) return
        navBar.style.transition = 'none'
        navBar.style.width = '0'
        navBar.style.opacity = '1'
        requestAnimationFrame(() => {
            navBar.style.transition = 'width .5s cubic-bezier(.4,0,.2,1)'
            navBar.style.width = '80%'
        })
    }
    function finishNavBar() {
        if (!navBar) return
        navBar.style.transition = 'width .2s ease'
        navBar.style.width = '100%'
        setTimeout(() => { navBar.style.opacity = '0' }, 250)
    }
    function hideNavBar() {
        if (!navBar) return
        navBar.style.transition = 'none'
        navBar.style.width = '0'
        navBar.style.opacity = '0'
    }

    function dcLoadTalentos(url, opts) {
        opts = opts || {}
        const miId = ++peticionActual
        if (abortActual) abortActual.abort()
        abortActual = new AbortController()

        fetch(url, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin',
            signal: abortActual.signal
        })
            .then(r => {
                if (!r.ok || r.redirected) throw new Error('respuesta inesperada')
                return r.text()
            })
            .then(html => {
                if (miId !== peticionActual) return // una request más nueva ya ganó

                const tpl = document.createElement('template')
                tpl.innerHTML = html.trim()
                const nuevo = tpl.content.querySelector('#' + resultadosId)
                const actual = document.getElementById(resultadosId)
                if (!nuevo || !actual) throw new Error('fragmento de resultados no encontrado')

                actual.dataset.currentPage = nuevo.dataset.currentPage
                actual.dataset.totalPages = nuevo.dataset.totalPages
                actual.dataset.pageSize = nuevo.dataset.pageSize
                actual.dataset.totalElements = nuevo.dataset.totalElements
                actual.innerHTML = nuevo.innerHTML

                if (opts.pushState) history.pushState({}, '', url)
                finishNavBar()
                if (opts.scrollToResults) actual.scrollIntoView({ behavior: 'smooth', block: 'start' })
            })
            .catch(err => {
                if (err.name === 'AbortError') return // una request más nueva la reemplazó
                if (miId === peticionActual) window.location = url
            })
    }

    // delegado en `document` para que siga funcionando aunque se
    // reemplace #talentos-resultados por fetch (las flechas nuevas
    // no tienen listener propio, pero burbujean hasta acá)
    document.addEventListener('click', e => {
        const tab = e.target.closest('.pagination-tab')
        if (!tab || !document.getElementById(resultadosId)?.contains(tab)) return

        if (tab.classList.contains('is-disabled') || tab.classList.contains('active')) {
            e.preventDefault()
            hideNavBar() // ws-shell.js ya pudo haber arrancado la barra para este <a>
            return
        }
        e.preventDefault()
        startNavBar()
        dcLoadTalentos(tab.href, { pushState: true, scrollToResults: true })
    })

    /* Atrás/adelante del navegador */
    window.addEventListener('popstate', () => {
        dcLoadTalentos(location.href, { pushState: false })
    })
})
