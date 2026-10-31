(function () {
  var tokenMeta = document.querySelector('meta[name="_csrf"]');
  var headerMeta = document.querySelector('meta[name="_csrf_header"]');
  if (!tokenMeta || !headerMeta || !window.fetch) return;

  var token = tokenMeta.getAttribute('content');
  var headerName = headerMeta.getAttribute('content');
  var metodosProtegidos = ['POST', 'PUT', 'PATCH', 'DELETE'];
  var fetchOriginal = window.fetch;

  window.fetch = function (input, init) {
    init = init || {};
    var metodo = (init.method || 'GET').toUpperCase();
    if (metodosProtegidos.indexOf(metodo) !== -1) {
      var headers = new Headers(init.headers || {});
      if (!headers.has(headerName)) headers.set(headerName, token);
      init = Object.assign({}, init, { headers: headers });
    }
    return fetchOriginal(input, init);
  };
})();

const THEMES = {
  light: {
    '--bg': '#f6f9ff',
    '--bg-2': '#eaf2fb',
    '--surface': '#ffffff',
    '--surface-2': '#f3f7fc',
    '--text': '#0f172a',
    '--muted': '#475569',
    '--accent': '#1d7fd0',
    '--accent-strong': '#0f5ea5',
    '--accent-soft': '#dbeaf7',
    '--highlight': '#93c5fd',
    '--border': '#d7e2ef',
    '--shadow-soft': '0 14px 30px rgba(15, 23, 42, 0.08)',
    '--radius-sm': '12px',
    '--radius-md': '18px',
    '--radius-lg': '26px',
    '--desc': 'var(--muted)',
    '--widget': 'var(--surface)',
    '--login': 'var(--surface)',
    '--login-border': 'var(--border)',
    '--division': 'var(--border)',
    '--main': 'var(--surface)',
    '--main-card': 'var(--surface-2)',
    '--badge-open-bg': '#dcfce7',
    '--badge-open-text': '#16a34a',
    '--input-bg': 'var(--surface)',
    '--accent-on': '#ffffff',
    '--border-strong': 'rgba(15, 23, 42, 0.16)',
    '--text-faint': '#94a3b8',
    '--chrome-dark': '#1b2333',
    '--chrome-dark-text': '#e7ecf3',
    '--chrome-dark-border': 'rgba(255, 255, 255, 0.08)'
  },
  dark: {
    '--bg': '#161a21',
    '--bg-2': '#13161c',
    '--surface': '#1b1f27',
    '--surface-2': '#21252e',
    '--text': '#e7ebf1',
    '--muted': '#8891a1',
    '--accent': '#2fb8d6',
    '--accent-strong': '#5cc9e0',
    '--accent-soft': 'rgba(47, 184, 214, .13)',
    '--highlight': '#7fd4e8',
    '--border': 'rgba(255, 255, 255, 0.08)',
    '--shadow-soft': '0 16px 32px rgba(0, 0, 0, 0.4)',
    '--radius-sm': '12px',
    '--radius-md': '18px',
    '--radius-lg': '26px',
    '--desc': 'var(--muted)',
    '--widget': 'var(--surface-2)',
    '--login': 'var(--surface)',
    '--login-border': 'var(--border)',
    '--division': 'var(--border)',
    '--main': 'var(--surface)',
    '--main-card': 'var(--surface-2)',
    '--badge-open-bg': 'rgba(47, 179, 125, .16)',
    '--badge-open-text': '#7FE0B8',
    '--input-bg': 'var(--surface-2)',
    '--accent-on': '#0c1f26',
    '--border-strong': 'rgba(255, 255, 255, 0.14)',
    '--text-faint': '#5f6773',
    '--cat-dev-text': '#8FC0F5',
    '--cat-data-text': '#F5C583',
    '--cat-success-text': '#7FE0B8',
    '--cat-error-text': '#E98888',
    '--cat-design-text': '#F0A0BE',
    '--chrome-dark': '#0a0d12',
    '--chrome-dark-text': '#c9d6e0',
    '--chrome-dark-border': 'rgba(255, 255, 255, 0.08)'
  },
  midnight: {
    '--bg': '#0b0f1a',
    '--bg-2': '#0d1220',
    '--surface': '#111827',
    '--surface-2': '#131829',
    '--text': '#e2e8f4',
    '--muted': '#8899bb',
    '--accent': '#1d7fd0',
    '--accent-strong': '#60a5fa',
    '--accent-soft': '#0f2a4a',
    '--highlight': '#93c5fd',
    '--border': '#1e2540',
    '--shadow-soft': '0 14px 30px rgba(0, 0, 0, 0.4)',
    '--radius-sm': '12px',
    '--radius-md': '18px',
    '--radius-lg': '26px',
    '--desc': '#4a5578',
    '--widget': '#131829',
    '--login': '#111827',
    '--login-border': '#1e2540',
    '--division': '#1a2035',
    '--main': '#0f1525',
    '--main-card': '#1a2035',
    '--badge-open-bg': '#0d2a18',
    '--badge-open-text': '#4ade80',
    '--input-bg': 'var(--login-border)',
    '--accent-on': '#ffffff',
    '--border-strong': 'rgba(255, 255, 255, 0.14)',
    '--text-faint': '#5b6c94',
    '--cat-dev-text': '#93c5fd',
    '--cat-data-text': '#F5C583',
    '--cat-success-text': '#7FE0B8',
    '--cat-error-text': '#E98888',
    '--cat-design-text': '#F0A0BE',
    '--chrome-dark': '#05070d',
    '--chrome-dark-text': '#e2e8f4',
    '--chrome-dark-border': '#1e2540'
  }
}

const THEME_META = {
  light: {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="4"/>
      <path stroke-linecap="round" d="M12 2v2M12 20v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M2 12h2M20 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
    </svg>`,
    label: 'Modo claro'
  },
  dark: {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round" d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z"/>
    </svg>`,
    label: 'Modo oscuro'
  },
  midnight: {
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round" d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557L3.04 10.345a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.499z"/>
    </svg>`,
    label: 'Midnight'
  }
}

const UI_DENSITY_KEY = 'ui-density'
const UI_TEXT_SIZE_KEY = 'ui-text-size'
const UI_CONTRAST_KEY = 'ui-contrast'
const UI_REDUCE_MOTION_KEY = 'dc-reduce-motion'

function applyUiPreferences() {
  const density = localStorage.getItem(UI_DENSITY_KEY) || 'normal'
  const textSize = localStorage.getItem(UI_TEXT_SIZE_KEY) || 'normal'
  const contrast = localStorage.getItem(UI_CONTRAST_KEY) || 'normal'
  const reduceMotion = localStorage.getItem(UI_REDUCE_MOTION_KEY) === 'true'

  if (document.documentElement) {
    if (textSize === 'small' || textSize === 'large') {
      document.documentElement.dataset.textSize = textSize
    } else {
      delete document.documentElement.dataset.textSize
    }

    if (reduceMotion) {
      document.documentElement.dataset.reduceMotion = 'true'
    } else {
      delete document.documentElement.dataset.reduceMotion
    }
  }

  if (document.body) {
    if (density === 'compact' || density === 'spacious') {
      document.body.dataset.density = density
    } else {
      delete document.body.dataset.density
    }

    if (contrast === 'high') {
      document.body.dataset.contrast = 'high'
    } else {
      delete document.body.dataset.contrast
    }
  }
}

window.applyUiPreferences = applyUiPreferences

function syncThemeControls(name) {
  const meta = THEME_META[name]
  if (!meta) return

  const icon = document.getElementById('theme-icon')
  const label = document.getElementById('theme-label')
  if (icon) icon.innerHTML = meta.icon
  if (label) label.textContent = meta.label
}

function applyTheme(name) {
  if (!THEMES[name]) return
  Object.entries(THEMES[name]).forEach(([prop, val]) => {
    document.documentElement.style.setProperty(prop, val)
  })
  localStorage.setItem('theme', name)
  syncThemeControls(name)
  document.dispatchEvent(new CustomEvent('dc:theme-changed', { detail: { theme: name } }))
}

const THEME_ORDER = ['light', 'dark', 'midnight']

function cycleTheme() {
  const current = localStorage.getItem('theme') || 'dark'
  const idx = THEME_ORDER.indexOf(current)
  applyTheme(THEME_ORDER[(idx + 1) % THEME_ORDER.length])
}
window.cycleTheme = cycleTheme

document.addEventListener('click', (e) => {
  if (e.target.closest('.theme-toggle')) cycleTheme()
})
document.addEventListener('keydown', (e) => {
  if ((e.key === 'Enter' || e.key === ' ') && e.target.closest('.theme-toggle')) {
    e.preventDefault()
    cycleTheme()
  }
})

const savedTheme = localStorage.getItem('theme') || 'dark'
applyTheme(savedTheme)
applyUiPreferences()

document.addEventListener('DOMContentLoaded', () => {
  const currentTheme = localStorage.getItem('theme') || savedTheme
  syncThemeControls(currentTheme)
  applyUiPreferences()
})