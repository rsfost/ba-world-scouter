<script lang="ts">
  import { getTheme, setTheme, type Theme } from '../lib/theme.svelte';

  const options: { value: Theme; label: string; glyph: string }[] = [
    { value: 'light', label: 'Light theme', glyph: '☀' },
    { value: 'system', label: 'Follow system theme', glyph: '◐' },
    { value: 'dark', label: 'Dark theme', glyph: '☾' },
  ];
</script>

<div class="toggle" role="group" aria-label="Theme">
  {#each options as option (option.value)}
    <button
      type="button"
      title={option.label}
      aria-label={option.label}
      aria-pressed={getTheme() === option.value}
      class:active={getTheme() === option.value}
      onclick={() => setTheme(option.value)}
    >
      {option.glyph}
    </button>
  {/each}
</div>

<style>
  .toggle {
    display: flex;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    overflow: hidden;
    background: var(--surface);
  }

  button {
    padding: 0.3rem 0.6rem;
    background: none;
    border: 0;
    border-right: 1px solid var(--border);
    color: var(--text-muted);
    cursor: pointer;
    line-height: 1.2;
  }

  button:last-child {
    border-right: 0;
  }

  button:hover {
    background: var(--surface-hover);
    color: var(--text);
  }

  button.active {
    background: var(--accent-bg);
    color: var(--accent);
  }

  button:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: -2px;
  }
</style>
