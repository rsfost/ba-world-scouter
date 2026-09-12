/** Formats an elapsed duration in seconds as a compact age, e.g. "4m 20s". */
export function formatAge(seconds: number): string {
  if (!Number.isFinite(seconds)) return '—';
  const s = Math.max(0, Math.floor(seconds));
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m ${String(s % 60).padStart(2, '0')}s`;
  const h = Math.floor(m / 60);
  return `${h}h ${String(m % 60).padStart(2, '0')}m`;
}

/** Buckets an age in seconds into a freshness class used for colouring. */
export function freshness(seconds: number): 'fresh' | 'aging' | 'stale' {
  if (seconds < 120) return 'fresh';
  if (seconds < 900) return 'aging';
  return 'stale';
}
