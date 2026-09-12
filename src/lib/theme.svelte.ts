export type Theme = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'ba-scout-theme';

function load(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') return stored;
  } catch {
    // Private windows and blocked site data throw on access; fall through.
  }
  return 'system';
}

let theme = $state<Theme>(load());

export function getTheme(): Theme {
  return theme;
}

export function setTheme(next: Theme): void {
  theme = next;
  apply(next);
  try {
    localStorage.setItem(STORAGE_KEY, next);
  } catch {
    // Preference simply won't persist; the page still works.
  }
}

/** Stamps the root element so CSS can override the media-query default. */
export function apply(next: Theme = theme): void {
  const root = document.documentElement;
  if (next === 'system') delete root.dataset.theme;
  else root.dataset.theme = next;
}
