<script lang="ts">
  import type { TrackedWorld } from '../lib/worlds.svelte';
  import { nowSeconds } from '../lib/clock.svelte';
  import WorldRow from './WorldRow.svelte';

  interface Props {
    worlds: TrackedWorld[];
    filter: string;
  }

  const { worlds, filter }: Props = $props();

  type SortKey = 'worldId' | 'population' | 'predictedY' | 'confirmedY' | 'confirmedAge';

  interface Column {
    label: string;
    title: string;
    sort: SortKey;
  }

  const columns: Column[] = [
    { label: 'World', title: 'World number', sort: 'worldId' },
    { label: 'Players', title: 'Player count from the RuneLite world list', sort: 'population' },
    { label: 'Pred Y', title: 'Server-side prediction of the current y', sort: 'predictedY' },
    { label: 'Conf Y', title: 'Last y reported by a plugin client', sort: 'confirmedY' },
    { label: 'Confirmed', title: 'Age of the last client report', sort: 'confirmedAge' },
  ];

  let sortKey = $state<SortKey>('worldId');
  let ascending = $state(true);

  const now = $derived(nowSeconds());

  function sortValue(world: TrackedWorld, key: SortKey): number {
    switch (key) {
      case 'worldId':
        return world.worldId;
      case 'population':
        return world.population;
      case 'predictedY':
        return world.prediction?.y ?? -1;
      case 'confirmedY':
        return world.confirmed.y;
      case 'confirmedAge':
        return world.confirmed.time;
    }
  }

  function toggle(key: SortKey): void {
    if (sortKey === key) {
      ascending = !ascending;
    } else {
      sortKey = key;
      // Ages read most naturally newest-first; everything else ascending.
      ascending = key !== 'confirmedAge';
    }
  }

  const visible = $derived.by(() => {
    const query = filter.trim();
    const rows = query
      ? worlds.filter((w) => String(w.worldId).includes(query))
      : worlds;
    const direction = ascending ? 1 : -1;
    return [...rows].sort(
      (a, b) =>
        direction * (sortValue(a, sortKey) - sortValue(b, sortKey)) ||
        a.worldId - b.worldId,
    );
  });
</script>

<div class="wrap">
  <table>
    <thead>
      <tr>
        {#each columns as column (column.label)}
          {@const key = column.sort}
          <th title={column.title} aria-sort={sortKey === key
              ? ascending ? 'ascending' : 'descending'
              : 'none'}>
            <button type="button" onclick={() => toggle(key)}>
              {column.label}
              <span class="arrow" class:active={sortKey === key}>
                {sortKey === key && !ascending ? '▼' : '▲'}
              </span>
            </button>
          </th>
        {/each}
      </tr>
    </thead>
    <tbody>
      {#each visible as world (world.worldId)}
        <WorldRow {world} {now} />
      {/each}
    </tbody>
  </table>

  {#if visible.length === 0}
    <p class="empty">
      {worlds.length === 0 ? 'No scouted worlds yet.' : 'No worlds match that filter.'}
    </p>
  {/if}
</div>

<style>
  .wrap {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    overflow-x: auto;
  }

  table {
    width: 100%;
    border-collapse: collapse;
  }

  th {
    position: sticky;
    top: 0;
    z-index: 1;
    padding: 0;
    text-align: right;
    background: var(--surface-alt);
    border-bottom: 1px solid var(--border-strong);
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--text-muted);
    white-space: nowrap;
  }

  th button {
    display: block;
    width: 100%;
    padding: 0.5rem 0.75rem;
    text-align: right;
    background: none;
    border: 0;
    cursor: pointer;
  }

  th button:hover {
    background: var(--surface-hover);
    color: var(--text);
  }

  th button:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: -2px;
  }

  .arrow {
    display: inline-block;
    width: 0.7em;
    font-size: 0.7em;
    color: transparent;
  }

  .arrow.active {
    color: var(--accent);
  }

  .empty {
    margin: 0;
    padding: 2rem 1rem;
    text-align: center;
    color: var(--text-muted);
  }
</style>
