/** One world's scouted instance state, as returned by the API. */
export interface WorldInstance {
  worldId: number;
  /** Player count from the RuneLite world list, or -1 when unknown. */
  population: number;
  /** The last observation reported by a plugin client. Unix seconds. */
  confirmed: { time: number; x: number; y: number };
  /**
   * Server-side extrapolation of `confirmed` forward to `time`.
   * `based_on` is the confirmed observation it was derived from.
   */
  prediction?: { based_on: number; time: number; y: number };
}

const API_BASE = (
  import.meta.env.VITE_API_BASE ?? 'https://bascout.jfost.com/api/v1'
).replace(/\/$/, '');

export const WORLDS_URL = `${API_BASE}/worlds`;
export const STREAM_URL = `${API_BASE}/worlds/stream`;

export async function fetchWorlds(signal?: AbortSignal): Promise<WorldInstance[]> {
  const resp = await fetch(WORLDS_URL, { signal });
  if (!resp.ok) {
    throw new Error(`GET /worlds failed (http ${resp.status})`);
  }
  return (await resp.json()) as WorldInstance[];
}
