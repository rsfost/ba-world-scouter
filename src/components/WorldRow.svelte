<script lang="ts">
  import type { TrackedWorld } from '../lib/worlds.svelte';
  import { formatAge, freshness } from '../lib/time';
  import { flagFor } from '../lib/flags';

  interface Props {
    world: TrackedWorld;
    now: number;
  }

  const { world, now }: Props = $props();

  const flag = $derived(flagFor(world.worldId));

  const confirmedAge = $derived(now - world.confirmed.time);
  // The updater recomputes predictions on its own schedule, so a fresh
  // confirmation can sit here before a matching prediction exists.
  const predictionStale = $derived(
    !world.prediction || world.prediction.based_on < world.confirmed.time,
  );

  let flash = $state(false);
  let timer: ReturnType<typeof setTimeout> | undefined;

  $effect(() => {
    if (world.updatedAt === undefined) return;
    flash = true;
    clearTimeout(timer);
    timer = setTimeout(() => (flash = false), 1200);
    return () => clearTimeout(timer);
  });
</script>

<tr class:flash>
  <td class="num world">
    <span class="id">
      <span class="flag">
        {#if flag}
          <img src={flag.src} alt={flag.country} title={flag.country} width="18" height="18" />
        {/if}
      </span>
      {world.worldId}
    </span>
  </td>
  <td class="num">{world.population < 0 ? '—' : world.population}</td>
  <td class="num strong" class:dim={predictionStale}>
    {world.prediction ? world.prediction.y : '—'}
  </td>
  <td class="num">{world.confirmed.y}</td>
  <td class="num age {freshness(confirmedAge)}">{formatAge(confirmedAge)}</td>
</tr>

<style>
  td {
    padding: 0.4rem 0.75rem;
    border-bottom: 1px solid var(--border);
    white-space: nowrap;
  }

  tr:hover td {
    background: var(--surface-hover);
  }

  .num {
    text-align: right;
    font-family: var(--mono);
    font-variant-numeric: tabular-nums;
  }

  .world {
    font-weight: 600;
    color: var(--accent);
  }

  .id {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
  }

  /* Fixed slot so world numbers stay aligned when a flag is missing. */
  .flag {
    display: inline-flex;
    width: 18px;
    height: 18px;
  }

  .flag img {
    image-rendering: pixelated;
  }

  .strong {
    font-weight: 600;
  }

  .dim {
    color: var(--text-faint);
    font-weight: 400;
  }

  .age.fresh {
    color: var(--ok);
  }

  .age.aging {
    color: var(--warn);
  }

  .age.stale {
    color: var(--bad);
  }

  .flash td {
    animation: flash 1.2s ease-out;
  }

  @keyframes flash {
    from {
      background: var(--flash);
    }
    to {
      background: transparent;
    }
  }
</style>
