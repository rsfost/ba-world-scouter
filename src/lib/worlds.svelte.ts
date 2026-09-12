import { SvelteMap } from 'svelte/reactivity';
import { fetchWorlds, STREAM_URL, type WorldInstance } from './api';

export type ConnectionState = 'connecting' | 'live' | 'reconnecting' | 'offline';

/** Full re-fetch interval. The stream never emits deletions, so worlds that
 *  expire server-side only disappear when we reconcile against a snapshot. */
const RESYNC_INTERVAL_MS = 120_000;
/** Backoff used when EventSource gives up entirely and we re-create it. */
const RECONNECT_DELAY_MS = 5_000;

export interface TrackedWorld extends WorldInstance {
  /** Client clock (ms) of the last stream message for this world. Drives the
   *  row-update flash; unset for rows that only ever came from a snapshot. */
  updatedAt?: number;
}

/**
 * Live view of the world list: a snapshot fetch seeds it, the SSE stream keeps
 * it current, and periodic snapshots reconcile away expired worlds.
 */
export class WorldStore {
  readonly worlds = new SvelteMap<number, TrackedWorld>();
  connection = $state<ConnectionState>('connecting');
  /** Client clock (ms) of the last successful snapshot or stream message. */
  lastUpdate = $state<number | null>(null);
  error = $state<string | null>(null);

  #source: EventSource | null = null;
  #resyncTimer: ReturnType<typeof setInterval> | null = null;
  #reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  #inflight: AbortController | null = null;
  /** Worlds seen on the stream since the in-flight snapshot was requested.
   *  They must survive reconciliation even though the snapshot predates them. */
  #seenSinceFetch = new Set<number>();
  #stopped = false;

  start(): void {
    this.#stopped = false;
    this.#openStream();
    void this.resync();
    this.#resyncTimer = setInterval(() => void this.resync(), RESYNC_INTERVAL_MS);
    document.addEventListener('visibilitychange', this.#onVisibilityChange);
  }

  stop(): void {
    this.#stopped = true;
    this.#source?.close();
    this.#source = null;
    this.#inflight?.abort();
    if (this.#resyncTimer !== null) clearInterval(this.#resyncTimer);
    if (this.#reconnectTimer !== null) clearTimeout(this.#reconnectTimer);
    document.removeEventListener('visibilitychange', this.#onVisibilityChange);
  }

  /** Re-fetches the whole list and reconciles it against current state. */
  async resync(): Promise<void> {
    this.#inflight?.abort();
    const controller = new AbortController();
    this.#inflight = controller;
    this.#seenSinceFetch.clear();

    try {
      const snapshot = await fetchWorlds(controller.signal);
      this.#applySnapshot(snapshot);
      this.error = null;
      this.lastUpdate = Date.now();
    } catch (err) {
      if (controller.signal.aborted) return;
      this.error = err instanceof Error ? err.message : String(err);
    } finally {
      if (this.#inflight === controller) this.#inflight = null;
    }
  }

  #applySnapshot(snapshot: WorldInstance[]): void {
    const present = new Set<number>();
    for (const world of snapshot) {
      present.add(world.worldId);
      const existing = this.worlds.get(world.worldId);
      // A stream message that landed mid-fetch is newer than the snapshot.
      if (existing && observedAt(existing) > observedAt(world)) continue;
      this.worlds.set(world.worldId, { ...existing, ...world });
    }
    for (const id of [...this.worlds.keys()]) {
      if (!present.has(id) && !this.#seenSinceFetch.has(id)) this.worlds.delete(id);
    }
  }

  #openStream(): void {
    if (this.#stopped) return;
    this.#source?.close();

    const source = new EventSource(STREAM_URL);
    this.#source = source;

    source.onopen = () => {
      const reconnected = this.connection !== 'connecting';
      this.connection = 'live';
      // A dropped connection loses every event sent while we were away, and
      // the server keeps no backlog, so re-seed from a snapshot.
      if (reconnected) void this.resync();
    };

    source.onmessage = (event) => {
      let world: WorldInstance;
      try {
        world = JSON.parse(event.data) as WorldInstance;
      } catch {
        return; // Ignore malformed frames rather than tearing down the stream.
      }
      if (typeof world?.worldId !== 'number') return;
      this.#seenSinceFetch.add(world.worldId);
      const existing = this.worlds.get(world.worldId);
      this.worlds.set(world.worldId, { ...existing, ...world, updatedAt: Date.now() });
      this.connection = 'live';
      this.lastUpdate = Date.now();
    };

    source.onerror = () => {
      if (source.readyState === EventSource.CLOSED) {
        // EventSource has given up; rebuild it ourselves after a delay.
        this.connection = 'offline';
        this.#reconnectTimer = setTimeout(() => this.#openStream(), RECONNECT_DELAY_MS);
      } else {
        this.connection = 'reconnecting';
      }
    };
  }

  #onVisibilityChange = (): void => {
    if (document.visibilityState === 'visible') void this.resync();
  };
}

/** Newest server timestamp carried by a record, for ordering merges. */
function observedAt(world: WorldInstance): number {
  return Math.max(world.confirmed?.time ?? 0, world.prediction?.time ?? 0);
}
