document.addEventListener('DOMContentLoaded', () => {
  const themeSelect = document.getElementById('theme-select')
  const densityChips = Array.from(document.querySelectorAll('[data-density]'))
  const textSizeSelect = document.getElementById('text-size-select')
  const contrastToggle = document.getElementById('toggle-contrast')

  const DENSITY_KEY = 'ui-density'
  const TEXT_SIZE_KEY = 'ui-text-size'
  const CONTRAST_KEY = 'ui-contrast'

  if (themeSelect) {
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme) {
      themeSelect.value = savedTheme
    }

    themeSelect.addEventListener('change', (event) => {
      if (typeof applyTheme === 'function') {
        applyTheme(event.target.value)
      }
    })
  }

  const savedDensity = localStorage.getItem(DENSITY_KEY) || 'normal'
  if (densityChips.length) {
    const setActiveDensity = (value) => {
      densityChips.forEach((chip) => {
        const isActive = chip.dataset.density === value
        chip.classList.toggle('active', isActive)
        chip.setAttribute('aria-pressed', isActive ? 'true' : 'false')
      })
    }

    setActiveDensity(savedDensity)
    densityChips.forEach((chip) => {
      chip.addEventListener('click', () => {
        const value = chip.dataset.density || 'normal'
        localStorage.setItem(DENSITY_KEY, value)
        setActiveDensity(value)
        if (typeof applyUiPreferences === 'function') {
          applyUiPreferences()
        }
      })
    })
  }

  if (textSizeSelect) {
    const savedTextSize = localStorage.getItem(TEXT_SIZE_KEY) || 'normal'
    textSizeSelect.value = savedTextSize
    textSizeSelect.addEventListener('change', (event) => {
      localStorage.setItem(TEXT_SIZE_KEY, event.target.value)
      if (typeof applyUiPreferences === 'function') {
        applyUiPreferences()
      }
    })
  }

  if (contrastToggle) {
    const savedContrast = localStorage.getItem(CONTRAST_KEY) === 'high'
    contrastToggle.checked = savedContrast
    contrastToggle.addEventListener('change', (event) => {
      localStorage.setItem(CONTRAST_KEY, event.target.checked ? 'high' : 'normal')
      if (typeof applyUiPreferences === 'function') {
        applyUiPreferences()
      }
    })
  }
})
