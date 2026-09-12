<script lang="ts">
  import type { ConnectionState } from '../lib/worlds.svelte';

  interface Props {
    state: ConnectionState;
  }

  const { state }: Props = $props();

  const labels: Record<ConnectionState, string> = {
    connecting: 'Connecting',
    live: 'Live',
    reconnecting: 'Reconnecting',
    offline: 'Offline',
  };
</script>

<span class="badge {state}">
  <span class="dot"></span>
  {labels[state]}
</span>

<style>
  .badge {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.25rem 0.6rem;
    border: 1px solid var(--border);
    border-radius: 999px;
    background: var(--surface);
    font-size: 0.8rem;
    color: var(--text-muted);
    white-space: nowrap;
  }

  .dot {
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background: var(--text-faint);
  }

  .live .dot {
    background: var(--ok);
  }

  .connecting .dot,
  .reconnecting .dot {
    background: var(--warn);
    animation: pulse 1.2s ease-in-out infinite;
  }

  .offline .dot {
    background: var(--bad);
  }

  @keyframes pulse {
    50% {
      opacity: 0.25;
    }
  }
</style>
