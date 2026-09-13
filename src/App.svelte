<script lang="ts">
  import { WorldStore } from './lib/worlds.svelte';
  import { nowSeconds } from './lib/clock.svelte';
  import { formatAge } from './lib/time';
  import WorldTable from './components/WorldTable.svelte';
  import StatusBadge from './components/StatusBadge.svelte';
  import ThemeToggle from './components/ThemeToggle.svelte';

  // Always ends in a slash, and carries the /ba-world-scouter/ prefix in
  // production builds.
  const base = import.meta.env.BASE_URL;

  const store = new WorldStore();
  let filter = $state('');

  $effect(() => {
    store.start();
    return () => store.stop();
  });

  const worlds = $derived([...store.worlds.values()]);
  const lastUpdateAge = $derived(
    store.lastUpdate === null ? null : nowSeconds() - store.lastUpdate / 1000,
  );
</script>

<header>
  <div class="titles">
    <h1>BA World Scouter</h1>
    <p class="sub">{worlds.length} scouted {worlds.length === 1 ? 'world' : 'worlds'}</p>
  </div>

  <div class="controls">
    <input
      type="search"
      inputmode="numeric"
      placeholder="Filter world…"
      aria-label="Filter by world number"
      bind:value={filter}
    />
    <StatusBadge state={store.connection} />
    <ThemeToggle />
  </div>
</header>

<div class="plug-wrap">
  <aside class="plug">
    <span>
      World info is crowd-sourced by players running the
      <strong>BA World Scouter</strong> RuneLite plugin.
    </span>
    <a href="https://runelite.net/plugin-hub/show/ba-world-scouter" target="_blank" rel="noopener">
      Install it to contribute&nbsp;→
    </a>
  </aside>
</div>

{#if store.error}
  <p class="error" role="alert">
    Could not reach the API: {store.error}
    <button type="button" onclick={() => store.resync()}>Retry</button>
  </p>
{/if}

<main>
  <WorldTable {worlds} {filter} />
</main>

<footer>
  <span>
    {#if lastUpdateAge === null}
      Waiting for data…
    {:else}
      Updated {formatAge(lastUpdateAge)} ago
    {/if}
  </span>
  <span class="links">
    <!-- The flag sprites are BSD-2-Clause material from RuneLite; the license
         requires the notice to travel with the deployed bundle. -->
    <a href="{base}NOTICE.txt">Flag icons from RuneLite (BSD-2-Clause)</a>
    <a href="https://github.com/rsfost/ba-world-scouter/tree/web">Source</a>
  </span>
</footer>

<style>
  header,
  .plug-wrap,
  main,
  footer {
    max-width: 900px;
    margin: 0 auto;
    padding-inline: 16px;
  }

  header {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    gap: 0.75rem;
    padding-block: 1.5rem 1rem;
  }

  h1 {
    margin: 0;
    font-size: 1.25rem;
    letter-spacing: -0.01em;
  }

  .sub {
    margin: 0.15rem 0 0;
    color: var(--text-muted);
    font-size: 0.85rem;
  }

  .controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
  }

  .plug {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 0.25rem 1rem;
    margin-bottom: 0.75rem;
    padding: 0.5rem 0.9rem;
    border: 1px solid var(--border);
    border-left: 3px solid var(--accent);
    border-radius: var(--radius);
    background: var(--surface);
    color: var(--text-muted);
    font-size: 0.85rem;
  }

  .plug strong {
    color: var(--text);
    font-weight: 600;
  }

  .plug a {
    color: var(--accent);
    font-weight: 600;
    text-decoration: none;
    white-space: nowrap;
  }

  .plug a:hover,
  .plug a:focus-visible {
    text-decoration: underline;
  }

  input {
    width: 9rem;
    padding: 0.3rem 0.6rem;
    background: var(--surface);
    color: var(--text);
    border: 1px solid var(--border);
    border-radius: var(--radius);
  }

  input:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: -1px;
  }

  .error {
    max-width: 900px;
    margin: 0 auto 0.75rem;
    padding: 0.6rem 0.9rem;
    border: 1px solid var(--bad);
    border-radius: var(--radius);
    background: var(--surface);
    color: var(--bad);
  }

  .error button {
    margin-left: 0.5rem;
    padding: 0.15rem 0.5rem;
    background: none;
    border: 1px solid currentColor;
    border-radius: var(--radius);
    cursor: pointer;
  }

  footer {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    padding-block: 0.75rem 2rem;
    color: var(--text-faint);
    font-size: 0.8rem;
  }

  footer a {
    color: inherit;
  }

  .links {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
  }

  @media (max-width: 520px) {
    header {
      align-items: stretch;
    }

    input {
      flex: 1 1 auto;
      width: auto;
    }
  }
</style>
