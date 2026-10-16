document.addEventListener('DOMContentLoaded', () => {
  const filterChips = Array.from(document.querySelectorAll('.project-filters .chip'))
  const projects = Array.from(document.querySelectorAll('.project-item'))
  const searchInput = document.getElementById('project-search')

  if (!filterChips.length || !projects.length) return

  const normalize = (value) =>
    (value || '')
      .toString()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase()

  const applyFilters = () => {
    const activeChip = filterChips.find((chip) => chip.classList.contains('active'))
    const activeFilter = activeChip ? activeChip.dataset.filter : 'all'
    const searchTerm = normalize(searchInput ? searchInput.value : '')

    projects.forEach((project) => {
      const tags = normalize(project.dataset.tags)
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean)
      const title = normalize(project.dataset.title)
      const meta = normalize(project.dataset.meta)
      const textMatches = !searchTerm || title.includes(searchTerm) || meta.includes(searchTerm)
      const tagMatches = activeFilter === 'all' || tags.includes(activeFilter)

      const isVisible = tagMatches && textMatches
      project.classList.toggle('is-hidden', !isVisible)
    })
  }

  filterChips.forEach((chip) => {
    const activate = () => {
      filterChips.forEach((item) => {
        item.classList.remove('active')
        item.setAttribute('aria-pressed', 'false')
      })
      chip.classList.add('active')
      chip.setAttribute('aria-pressed', 'true')
      applyFilters()
    }

    chip.addEventListener('click', activate)
    chip.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        activate()
      }
    })
  })

  if (searchInput) {
    searchInput.addEventListener('input', applyFilters)
  }

  applyFilters()
})
