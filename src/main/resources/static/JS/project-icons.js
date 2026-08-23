document.addEventListener('DOMContentLoaded', () => {
  const iconMap = {
    layers: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2" /><polyline points="2 17 12 22 22 17" /><polyline points="2 12 12 17 22 12" /></svg>',
    code: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" /></svg>',
    zap: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z" /></svg>',
    box: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" /></svg>',
    cpu: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="16" height="16" rx="2" /><rect x="9" y="9" width="6" height="6" /><line x1="9" y1="1" x2="9" y2="4" /><line x1="15" y1="1" x2="15" y2="4" /><line x1="9" y1="20" x2="9" y2="23" /><line x1="15" y1="20" x2="15" y2="23" /><line x1="20" y1="9" x2="23" y2="9" /><line x1="20" y1="14" x2="23" y2="14" /><line x1="1" y1="9" x2="4" y2="9" /><line x1="1" y1="14" x2="4" y2="14" /></svg>',
    globe: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10" /><line x1="2" y1="12" x2="22" y2="12" /><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" /></svg>',
    chart: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /></svg>',
    pen: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" /></svg>',
    rocket: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z" /><path d="m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z" /><path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 0 5 0" /><path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5" /></svg>',
    star: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>',
    shield: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>',
    terminal: '<svg class="icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 17 10 11 4 5" /><line x1="12" y1="19" x2="20" y2="19" /></svg>'
  };

  const renderIcon = (tile) => {
    const logoUrl = tile.dataset.logoUrl;
    const key = tile.dataset.iconKey || 'layers';
    const color = tile.dataset.iconColor || '#1f8f8f';

    if (logoUrl) {
      tile.style.background = 'transparent';
      tile.innerHTML = '<img src="' + logoUrl + '" alt="Logo" style="width:32px;height:32px;object-fit:cover;border-radius:8px;">';
      return;
    }

    tile.style.background = color;
    tile.style.color = '#fff';
    tile.innerHTML = iconMap[key] || iconMap.layers;
  };

  const COVER_CLASSES = [
    'cover-ocean',
    'cover-studio',
    'cover-aurora',
    'cover-purple',
    'cover-fire',
    'cover-mint',
    'cover-night',
    'cover-grape',
    'cover-ice',
    'cover-cosmic',
    'cover-ash',
    'cover-sunset',
    'cover-dawn',
    'cover-forest'
  ];

  const isGradient = (value) => /gradient\(/i.test(value || '');
  const isColor = (value) => /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(value || '')
    || /^(rgb|hsl)a?\(/i.test(value || '');

  const clamp = (num) => Math.min(255, Math.max(0, num));

  const shadeHex = (hex, percent) => {
    let clean = (hex || '').replace('#', '').trim();
    if (clean.length === 3) {
      clean = clean.split('').map((ch) => ch + ch).join('');
    }
    if (clean.length !== 6) return hex;
    const num = parseInt(clean, 16);
    const r = (num >> 16) & 0xff;
    const g = (num >> 8) & 0xff;
    const b = num & 0xff;
    const factor = percent / 100;
    const next = (value) => clamp(Math.round(value + 255 * factor));
    return `#${[next(r), next(g), next(b)]
      .map((v) => v.toString(16).padStart(2, '0'))
      .join('')}`;
  };

  const buildGradient = (value) => {
    if (isGradient(value)) return value;
    if (/^#/.test(value || '')) {
      const start = shadeHex(value, 12);
      const end = shadeHex(value, -10);
      return `linear-gradient(135deg, ${start} 0%, ${end} 100%)`;
    }
    return `linear-gradient(135deg, ${value} 0%, ${value} 100%)`;
  };

  const clearCoverClasses = (banner) => {
    COVER_CLASSES.forEach((cls) => banner.classList.remove(cls));
  };

  const applyCoverClass = (banner, cover) => {
    clearCoverClasses(banner);
    banner.style.removeProperty('--project-banner-bg');
    banner.classList.add(cover);
  };

  const applyCustomBanner = (banner, value) => {
    clearCoverClasses(banner);
    banner.style.setProperty('--project-banner-bg', buildGradient(value));
  };

  const ensureBannerCover = (banner) => {
    const dataCover = (banner.dataset.cover || '').trim();
    const classCover = Array.from(banner.classList).find((cls) => cls.startsWith('cover-')) || '';
    const preferred = (dataCover || classCover || '').trim();

    if (preferred) {
      if (isGradient(preferred) || isColor(preferred)) {
        applyCustomBanner(banner, preferred);
        return;
      }
      const token = preferred.split(/\s+/)[0];
      if (token.startsWith('cover-')) {
        applyCoverClass(banner, token);
        return;
      }
    }

    const bannerColor = (banner.dataset.bannerColor || '').trim();
    if (bannerColor && (isGradient(bannerColor) || isColor(bannerColor))) {
      applyCustomBanner(banner, bannerColor);
      return;
    }

    applyCoverClass(banner, 'cover-ocean');
  };

  document.querySelectorAll('.project-icon').forEach(renderIcon);
  document.querySelectorAll('.project-banner').forEach(ensureBannerCover);
});
